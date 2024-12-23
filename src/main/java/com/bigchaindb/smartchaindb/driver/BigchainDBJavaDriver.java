package com.bigchaindb.smartchaindb.driver;


import com.bigchaindb.api.TransactionsApi;
import com.bigchaindb.builders.BigchainDbConfigBuilder;
import com.bigchaindb.model.Connection;
import com.bigchaindb.model.GenericCallback;
import com.bigchaindb.model.MetaData;
import com.bigchaindb.model.Transaction;
import okhttp3.Response;
import com.bigchaindb.constants.Operations;

import java.security.KeyPair;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Random;
import com.bigchaindb.model.Transactions; 

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

        /* -
        // GJoin: Uncomment this block if group manager is set up. Make sure GROUP_SIGNATURE flag
        // in DriverConstants is set to true
        String out = new Scanner(new URL(DriverConstants.GROUP_MANAGER_ENDPOINT + "?id=1&usk=1")
                .openStream(), "UTF-8").useDelimiter("\\A").next();
        String[] parts = out.split("\\|");
        String payloadString = "{transaction: test, tester: sen}";
        String secretKey='"'+parts[0].replaceAll(",","comma") +'"';
        String publicKey= '"'+parts[1].replaceAll(",","comma") + '"';
        String jsonPayload='"'+payloadString.replaceAll(",","comma")+ '"';
        */
        
        ExecutorService executor = Executors.newFixedThreadPool(2);

        setConfig();
        long startTime = System.nanoTime();
        KeyPair sellerKeyPair = BigchainDBJavaDriver.getKeys();
        KeyPair buyerKeyPair = BigchainDBJavaDriver.getKeys();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();
        
        // Lists to store transaction IDs
        List<String> sellerCreateIds = new CopyOnWriteArrayList<>();
        List<String> buyerCreateIds = new CopyOnWriteArrayList<>();
        List<String> advIds = new CopyOnWriteArrayList<>();
        List<String> buyOfferIds = new CopyOnWriteArrayList<>();
        List<String> invalidAdvIds = new CopyOnWriteArrayList<>();
        List<String> invalidBuyOfferIds = new CopyOnWriteArrayList<>();
        List<String> invalidSellIds = new CopyOnWriteArrayList<>();
        
        int validAssetCount = 150;
        int invalidAssetCount = 64;
        
        
        try {
            // Step 1: Create assets for the seller
            for (int i = 0; i < validAssetCount; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        String createId = Simulation.createCreate(driver, sellerKeyPair);
                        if (createId != null) {
                            sellerCreateIds.add(createId);
                            System.out.println("Seller Asset " + (index + 1) + " Created: " + createId);
                        } else {
                            System.err.println("Failed to create Seller Asset " + (index + 1));
                        }
                        Thread.sleep(300); // Optional delay to avoid overload
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
                            System.out.println("Buyer Asset " + (index + 1) + " Created: " + createId);
                        } else {
                            System.err.println("Failed to create Buyer Asset " + (index + 1));
                        }
                        Thread.sleep(300); // Optional delay to avoid overload
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
                                System.out.println("Advertisement " + (index + 1) + " Created: " + advTransaction.getId());
                               // waitForTransactionCommit(advTransaction.getId());
                            } else {
                                System.err.println("Failed to create Advertisement for Seller Asset " + (index + 1));
                            }
                        } else {
                            System.err.println("Seller Asset not available for Advertisement creation at index: " + index);
                        }
                        Thread.sleep(300); // Optional delay to avoid overload
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
                                System.out.println("Buy Offer " + (index + 1) + " Created: " + buyOfferTransaction.getId());
                                //waitForTransactionCommit(buyOfferTransaction.getId());
                            } else {
                                System.err.println("Failed to create Buy Offer for Advertisement " + (index + 1));
                            }
                        } else {
                            System.err.println("Advertisement or Buyer Asset not available for Buy Offer creation at index: " + index);
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
                                System.out.println("Sell Transaction " + (index + 1) + " Created: " + sellId);
                            } else {
                                System.err.println("Failed to create Sell Transaction for Advertisement " + (index + 1));
                            }
                        } else {
                            System.err.println("Seller Asset, Advertisement, or Buy Offer not available for Sell Transaction creation at index: " + index);
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
                                            System.out.println("Return and Accept Return for Sell Transaction " + (index + 1) + " Completed");
                                        } else {
                                            System.err.println("Failed to create Accept Return for Sell Transaction " + (index + 1));
                                        }
                                    } else {
                                        System.err.println("No return transactions available for Buy Offer ID: " + buyOfferId);
                                    }
                                } else {
                                    System.err.println("Failed to create Return Sell for Sell Transaction " + (index + 1));
                                }
                            } else {
                                System.err.println("No transfer transactions available for Sell ID: " + sellId);
                            }
                        } else {
                            System.err.println("Sell Transaction not available for return handling at index: " + index);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
        
            // Step 7: Create invalid advertisements (duplicates with open status)
            for (int i = 0; i < validAssetCount * 0.1; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        if (sellerCreateIds.size() > index) {
                            String createId = sellerCreateIds.get(index);
                            Transaction invalidAdvTransaction = Simulation.createAdv(driver, sellerKeyPair, createId, "Open");
                            if (invalidAdvTransaction != null && invalidAdvTransaction.getId() != null) {
                                invalidAdvIds.add(invalidAdvTransaction.getId());
                                System.out.println("Invalid Advertisement " + (index + 1) + " Created: " + invalidAdvTransaction.getId());
                            } else {
                                System.err.println("Failed to create Invalid Advertisement for Seller Asset " + (index + 1));
                            }
                        } else {
                            System.err.println("Seller Asset not available for Invalid Advertisement creation at index: " + index);
                        }
                        Thread.sleep(100); // Optional delay to avoid overload
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
        
            // Step 8: Create invalid buy offers (referencing closed advertisements)
            for (int i = 0; i < invalidAssetCount; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        if (advIds.size() > index && buyerCreateIds.size() > index) {
                            String advId = advIds.get(index);
                            Simulation.updateAdv(driver, sellerKeyPair, sellerCreateIds.get(index), advId); // Close the advertisement
                            Transaction invalidBuyOfferTransaction = Simulation.createBuyOffer(driver, buyerKeyPair, advId, buyerCreateIds.get(index));
                            if (invalidBuyOfferTransaction != null && invalidBuyOfferTransaction.getId() != null) {
                                invalidBuyOfferIds.add(invalidBuyOfferTransaction.getId());
                                System.out.println("Invalid Buy Offer " + (index + 1) + " Created: " + invalidBuyOfferTransaction.getId());
                            } else {
                                System.err.println("Failed to create Invalid Buy Offer for Advertisement " + (index + 1));
                            }
                        } else {
                            System.err.println("Advertisement or Buyer Asset not available for Invalid Buy Offer creation at index: " + index);
                        }
                        Thread.sleep(100); // Optional delay to avoid overload
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
        
            // Step 9: Create invalid sell transactions (referencing closed advertisements)
            for (int i = 0; i < invalidAssetCount; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        if (sellerCreateIds.size() > index && advIds.size() > index && invalidBuyOfferIds.size() > index) {
                            String createId = sellerCreateIds.get(index);
                            String advId = advIds.get(index);
                            String buyOfferId = invalidBuyOfferIds.get(index);
                            Transaction invalidSellTransaction = Simulation.createSell(driver, sellerKeyPair, createId, advId, buyOfferId);
                            if (invalidSellTransaction != null && invalidSellTransaction.getId() != null) {
                                invalidSellIds.add(invalidSellTransaction.getId());
                                System.out.println("Invalid Sell Transaction " + (index + 1) + " Created: " + invalidSellTransaction.getId());
                            } else {
                                System.err.println("Failed to create Invalid Sell Transaction for Advertisement " + (index + 1));
                            }
                        } else {
                            System.err.println("Seller Asset, Advertisement, or Invalid Buy Offer not available for Invalid Sell Transaction creation at index: " + index);
                        }
                        Thread.sleep(100); // Optional delay to avoid overload
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).get();
            }
            long endTime = System.nanoTime();
            long elapsedTime = endTime - startTime; // In nanoseconds
            double elapsedTimeInSeconds = elapsedTime / 1_000_000_000.0;
            int totalTransactions = sellerCreateIds.size() + buyerCreateIds.size() + advIds.size() +
            buyOfferIds.size() + sellIds.size() + invalidAssetCount *3;
    
            double throughput = totalTransactions / elapsedTimeInSeconds;
            println("Workflow completed successfully for " + validAssetCount + " valid assets and " + invalidAssetCount + " invalid transactions.");
            println("Total Transactions: " + totalTransactions);
            println("Elapsed Time: " + elapsedTimeInSeconds + " seconds");
            println("Throughput: " + throughput + " transactions/second");
    
            System.out.println("Workflow completed successfully for " + validAssetCount + " valid assets and " + invalidAssetCount + " invalid transactions.");
        
        } catch (InterruptedException e) {
            System.err.println("Operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
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
        // Transaction rfq1 = null;
        // Transaction bid1 = null;
        // List<String> rfq_ids = new ArrayList<>();
        // List<String> create_ids = new ArrayList<>();
        // List<String> win_bid_ids = new ArrayList<>();
        // Transaction winningBid = null;
        // int rfq_count = 200;
        // int create_count = 5000;
        // int bid_count = 5000;
        // int bids_per_rfq = 25;
        // int counter = 0;
        // //create rfqs 
        // for(int j = 0; j < rfq_count; j++) {
        //     Transaction rfq = Simulation.createRFQ(driver, keys, null);
        //     rfq_ids.add(rfq.getId());
        // }
        // // create assests seperately 
        // for(int k = 0; k < create_count; k++) {
        //     String createId = Simulation.createCreate(driver, keys);
        //     create_ids.add(createId);
        // }
        // for(int i = 0; i  < rfq_ids.size() ; i++){
        //     String rfqId = rfq_ids.get(i);
        //     List<Transaction> bids = new ArrayList<>();
        //     for(int j = 0;  j < bids_per_rfq; j++) {
        //         String createId = create_ids.get(counter);
        //         counter ++;
        //         bids.add(Simulation.createBid(driver, keys, rfqId, createId));
        //     }
        //     Random rand = new Random();
        //     winningBid = bids.get(rand.nextInt(bids.size()));
        //     win_bid_ids.add(winningBid.getId());}

        // for(int j = 0; j < rfq_ids.size() ; j++){
        //     String rfq_Id = rfq_ids.get(j);
        //     MetaData metaData = new MetaData();
        //     metaData.setMetaData("requestCreationTimestamp", LocalDateTime.now(Clock.systemUTC()).toString());
        //     Transactions.doAccept(driver, win_bid_ids.get(j), rfq_Id, metaData, keys);
        // }
    

        // //Simulation.createBid(driver, keys, rfq.getId());

        // // MetaData metaData = new MetaData();
        // // metaData.setMetaData("requestCreationTimestamp", LocalDateTime.now(Clock.systemUTC()).toString());
        // // Transactions.doAccept(driver, bid1.getId(), rfq1.getId(), metaData, keys);
    }

    // public void waitForTransactionCommit(String txId) {
    // try {
    //     // Fetch transaction details by ID
    //     Transaction transaction = TransactionsApi.getTransactionById(txId);

    //     if (transaction == null) {
    //         System.err.println("Transaction not found: " + txId);
    //         return;
    //     }

    //     // Check the transaction status
    //     String status = transaction.getStatus(); // Assuming the status is a field in the response
    //     if (status == null) {
    //         System.err.println("Transaction status is null for transaction ID: " + txId);
    //         return;
    //     }

    //     // Process the status of the transaction
    //     if (status.equals("COMMITTED")) {
    //         System.out.println("Transaction " + txId + " is committed.");
    //     } else {
    //         System.out.println("Transaction " + txId + " is not yet committed. Status: " + status);
    //     }

    // } catch (Exception e) {
    //     System.err.println("Error checking transaction status for " + txId + ": " + e.getMessage());
    // }
//}

    

    /**
     * configures connection url and credentials
     */
    public static void setConfig() {
        // Single-Node Setup
        BigchainDbConfigBuilder.baseUrl("http://127.0.0.1:9984/").setup();

        // Multi-Node Setup
        // List<Connection> connections = new ArrayList<>();
        // for (String url : DriverConstants.VALIDATOR_NODES) {
        //     Map<String, Object> attributes = new TreeMap<>();
        //     attributes.put("baseUrl", url);
        //     connections.add(new Connection(attributes));
        // }


        //  BigchainDbConfigBuilder
        //         .addConnections(connections)
        //         .setTimeout(60000)
        //         .setup();
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
                System.out.println("Malformed: " + response.message());
            }

            public void pushedSuccessfully(Response response) {
                System.out.println("Transaction successfully posted");
            }

            public void otherError(Response response) {
                System.out.println("Other error: " + response.message());
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
