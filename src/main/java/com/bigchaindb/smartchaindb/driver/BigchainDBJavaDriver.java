package com.bigchaindb.smartchaindb.driver;


import com.bigchaindb.api.TransactionsApi;
import com.bigchaindb.builders.BigchainDbConfigBuilder;
import com.bigchaindb.model.Connection;
import com.bigchaindb.model.GenericCallback;
import com.bigchaindb.model.MetaData;
import com.bigchaindb.model.Transaction;
import okhttp3.Response;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.concurrent.*;
/**
 * simple usage of BigchainDB Java driver (https://github.com/bigchaindb/java-bigchaindb-driver)
 * to create TXs on BigchainDB network
 *
 * @author dev@bigchaindb.com
 */
public class BigchainDBJavaDriver {

    /**
     * Driver method
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        println("Running Process: " + getProcessId());
        
        setConfig();

        KeyPair sellerKeyPair = BigchainDBJavaDriver.getKeys();
        KeyPair buyerKeyPair = BigchainDBJavaDriver.getKeys();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
        
        // Lists to store transaction IDs
        List<String> sellerCreateIds = new CopyOnWriteArrayList<>();
        List<String> buyerCreateIds = new CopyOnWriteArrayList<>();
        List<String> advIds = new CopyOnWriteArrayList<>();
        List<String> buyOfferIds = new CopyOnWriteArrayList<>();
        
        int validAssetCount = 10;
        int invalidAssetCount = 0;
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        try {
            // Step 1: Create assets for the seller
            for (int i = 0; i < validAssetCount; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        String createId = Simulation.createCreate(driver, sellerKeyPair);
                        if (createId != null) {
                            sellerCreateIds.add(createId);
                            println("Seller Asset " + (index + 1) + " Created: " + createId);
                        } else {
                            printerr("Failed to create Seller Asset " + (index + 1));
                        }
                        Thread.sleep(100); // Optional delay to avoid overload
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
        
            // Step 2: Create assets for the buyer
            for (int i = 0; i < validAssetCount; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        String createId = Simulation.createCreate(driver, buyerKeyPair);
                        if (createId != null) {
                            buyerCreateIds.add(createId);
                            println("Buyer Asset " + (index + 1) + " Created: " + createId);
                        } else {
                            printerr("Failed to create Buyer Asset " + (index + 1));
                        }
                        Thread.sleep(100); // Optional delay to avoid overload
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
        
            // Step 3: Create valid advertisements for each seller asset
            for (int i = 0; i < validAssetCount; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        if (sellerCreateIds.size() > index) {
                            String createId = sellerCreateIds.get(index);
                            Transaction advTransaction = Simulation.createAdv(driver, sellerKeyPair, createId, "Open");
                            if (advTransaction != null && advTransaction.getId() != null) {
                                advIds.add(advTransaction.getId());
                                println("Advertisement " + (index + 1) + " Created: " + advTransaction.getId());
                            } else {
                                printerr("Failed to create Advertisement for Seller Asset " + (index + 1));
                            }
                        } else {
                            printerr("Seller Asset not available for Advertisement creation at index: " + index);
                        }
                        Thread.sleep(100); // Optional delay to avoid overload
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
        
            // Step 4: Create buy offers for each valid advertisement
            for (int i = 0; i < validAssetCount; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        if (advIds.size() > index && buyerCreateIds.size() > index) {
                            String advId = advIds.get(index);
                            String createBuyAssetId = buyerCreateIds.get(index);
                            Transaction buyOfferTransaction = Simulation.createBuyOffer(driver, buyerKeyPair, advId, createBuyAssetId);
                            if (buyOfferTransaction != null && buyOfferTransaction.getId() != null) {
                                buyOfferIds.add(buyOfferTransaction.getId());
                                println("Buy Offer " + (index + 1) + " Created: " + buyOfferTransaction.getId());
                            } else {
                                printerr("Failed to create Buy Offer for Advertisement " + (index + 1));
                            }
                        } else {
                            printerr("Advertisement or Buyer Asset not available for Buy Offer creation at index: " + index);
                        }
                        Thread.sleep(300); // Optional delay to avoid overload
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
        
            // Step 5: Create sell transactions for each valid advertisement and corresponding buy offer
            List<String> sellIds = new CopyOnWriteArrayList<>();
            for (int i = 0; i < validAssetCount; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        if (sellerCreateIds.size() > index && advIds.size() > index && buyOfferIds.size() > index) {
                            String createId = sellerCreateIds.get(index);
                            String advId = advIds.get(index);
                            String buyOfferId = buyOfferIds.get(index);
                            Transaction sellTransaction = Simulation.createSell(driver, sellerKeyPair, createId, advId, buyOfferId);
                            if (sellTransaction != null && sellTransaction.getId() != null) {
                                String sellId = sellTransaction.getId();
                                sellIds.add(sellId);
                                println("Sell Transaction " + (index + 1) + " Created: " + sellId);
                            } else {
                                printerr("Failed to create Sell Transaction for Advertisement " + (index + 1));
                            }
                        } else {
                            printerr("Seller Asset, Advertisement, or Buy Offer not available for Sell Transaction creation at index: " + index);
                        }
                        Thread.sleep(300); // Optional delay to avoid overload
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
        
            // Step 6: Handle returns for 20% of sell transactions
            for (int i = 0; i < validAssetCount * 0.2; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        if (sellIds.size() > index) {
                            String sellId = sellIds.get(index);
                            List<String> transferTxns = TransactionsApi.getTransferTransactionIdsByAssetId(sellId);
                            if (!transferTxns.isEmpty()) {
                                Transaction inverse = Simulation.createReturnSell(driver, buyerKeyPair, sellId, transferTxns.get(0));
                                if (inverse != null && inverse.getId() != null) {
                                    String buyOfferId = buyOfferIds.get(index);
                                    List<String> returnTxns = TransactionsApi.getTransferTransactionIdsByAssetId(buyOfferId);
                                    if (!returnTxns.isEmpty()) {
                                        Transaction acceptReturn = Simulation.createAcceptReturn(driver, sellerKeyPair, buyOfferId, returnTxns.get(0), inverse.getId());
                                        if (acceptReturn != null && acceptReturn.getId() != null) {
                                            println("Return and Accept Return for Sell Transaction " + (index + 1) + " Completed");
                                        } else {
                                            printerr("Failed to create Accept Return for Sell Transaction " + (index + 1));
                                        }
                                    } else {
                                        printerr("No return transactions available for Buy Offer ID: " + buyOfferId);
                                    }
                                } else {
                                    printerr("Failed to create Return Sell for Sell Transaction " + (index + 1));
                                }
                            } else {
                                printerr("No transfer transactions available for Sell ID: " + sellId);
                            }
                        } else {
                            printerr("Sell Transaction not available for return handling at index: " + index);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
        
            println("Workflow completed successfully for " + validAssetCount + " valid assets and " + invalidAssetCount + " invalid transactions.");
        
        } catch (InterruptedException e) {
            printerr("Operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            printerr("An error occurred: " + e.getMessage());
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * configures connection url and credentials
     */
    public static void setConfig() {
        // Single-Node Setup
       // BigchainDbConfigBuilder.baseUrl("http://127.0.0.1:9984/").setup();

        // Multi-Node Setup
        List<Connection> connections = new ArrayList<>();
        for (String url : DriverConstants.VALIDATOR_NODES) {
            Map<String, Object> attributes = new TreeMap<>();
            attributes.put("baseUrl", url);
            connections.add(new Connection(attributes));
        }


         BigchainDbConfigBuilder
                .addConnections(connections)
                .setTimeout(60000)
                .setup();
    }

    /**
     * generates EdDSA keypair to sign and verify transactions
     *
     * @return KeyPair
     */
    public static KeyPair getKeys() {
        net.i2p.crypto.eddsa.KeyPairGenerator edDsaKpg = new net.i2p.crypto.eddsa.KeyPairGenerator();
        return edDsaKpg.generateKeyPair();
    }

    public GenericCallback handleServerResponse(String operation, MetaData metaData, String txId) {

        return new GenericCallback() {
            public void transactionMalformed(Response response) {
                println("Malformed: " + response.message());
            }

            public void pushedSuccessfully(Response response) {
                println("Transaction successfully posted");
            }

            public void otherError(Response response) {
                println("Other error: " + response.message());
            }
        };
    }

    private static long getProcessId() {
        String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        try {
            return Long.parseLong(jvmName.split("@")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    private static void println(String out) {
        System.out.println(getProcessId() + ": " + out);
    }

    private static void printerr(String out) {
        System.err.println(getProcessId() + ": " + out);
    }
}
