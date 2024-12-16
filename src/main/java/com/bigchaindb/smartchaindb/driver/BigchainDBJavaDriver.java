package com.bigchaindb.smartchaindb.driver;

import com.bigchaindb.api.TransactionsApi;
import com.bigchaindb.builders.BigchainDbConfigBuilder;
import com.bigchaindb.model.Connection;
import com.bigchaindb.model.GenericCallback;
import com.bigchaindb.model.MetaData;
import com.bigchaindb.model.Transaction;
import okhttp3.Response;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;



public class BigchainDBJavaDriver {
    public boolean COMMIT_TX = true;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        println("Running Process: " + getProcessId());

        setConfig();

        KeyPair sellerKeyPair = getKeys();
        KeyPair buyerKeyPair = getKeys();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();

        int validAssetCount = 150;
        int invalidAssetCount = 0;

        // Transaction ID lists
        List<String> sellerCreateIds = new CopyOnWriteArrayList<>();
        List<String> buyerCreateIds = new CopyOnWriteArrayList<>();
        List<String> advIds = new CopyOnWriteArrayList<>();
        List<String> buyOfferIds = new CopyOnWriteArrayList<>();
        List<String> sellIds = new CopyOnWriteArrayList<>();

        // Execute each step in sequence:
        executeAndProcess(
            createSellerAssetsTasks(validAssetCount, driver, sellerKeyPair),
            "Seller Asset",
            sellerCreateIds
        )
        .thenCompose(v -> executeAndProcess(
            createBuyerAssetsTasks(validAssetCount, driver, buyerKeyPair),
            "Buyer Asset",
            buyerCreateIds
        ))
        .thenCompose(v -> executeAndProcess(
            createAdvertisementsTasks(sellerCreateIds, driver, sellerKeyPair),
            "Advertisement",
            advIds
        ))
        .thenCompose(v -> {
            CompletableFuture<Void> delay = new CompletableFuture<>();
            scheduler.schedule(() -> delay.complete(null), 10, TimeUnit.SECONDS); // Introduce a 10-second delay
            return delay;
        })
        .thenCompose(v -> executeAndProcess(
            createBuyOfferTasks(advIds, buyerCreateIds, driver, buyerKeyPair),
            "Buy Offer",
            buyOfferIds
        ))
        .thenCompose(v -> executeAndProcess(
            createSellTasks(sellerCreateIds, advIds, buyOfferIds, driver, sellerKeyPair),
            "Sell Transaction",
            sellIds
        ))
        .thenCompose(v -> {
            CompletableFuture<Void> delay = new CompletableFuture<>();
            scheduler.schedule(() -> delay.complete(null), 10, TimeUnit.SECONDS); // Introduce a 10-second delay
            return delay;
        })
        .thenCompose(v -> executeAndProcess(
            createReturnTasks(sellIds, buyOfferIds, driver, sellerKeyPair, buyerKeyPair, (int) (validAssetCount * 0.2)),
            "Return Handling",
            null // Not collecting IDs here
        ))
        .thenAccept(v -> {
            println("Workflow completed successfully for " + validAssetCount + " valid assets and " + invalidAssetCount + " invalid transactions.");
            // Shutdown the shared executor if implemented in Promise (if needed):
            Promise.shutdown();
        })
        .exceptionally(ex -> {
            printerr("An error occurred: " + ex.getMessage());
            Promise.shutdown();
            return null;
        }).join();

        scheduler.shutdown();
    }

    /**
     * Configures connection URL and credentials
     */
    public static void setConfig() {
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
     * Generates EdDSA keypair to sign and verify transactions
     *
     * @return KeyPair
     */
    public static KeyPair getKeys() {
        net.i2p.crypto.eddsa.KeyPairGenerator edDsaKpg = new net.i2p.crypto.eddsa.KeyPairGenerator();
        return edDsaKpg.generateKeyPair();
    }

    /**
     * Creates tasks to create seller assets.
     */
    private static List<Supplier<String>> createSellerAssetsTasks(int count, BigchainDBJavaDriver driver, KeyPair sellerKeyPair) {
        return createTasks(count, "Seller Asset", i -> {
            try {
                String createId = Simulation.createCreate(driver, sellerKeyPair);
                if (createId != null) {
                    println("Seller Asset " + (i + 1) + " Created: " + createId);
                    return createId;
                } else {
                    printerr("Failed to create Seller Asset " + (i + 1));
                    return null;
                }
            } catch (Exception e) {
                printerr("Exception creating Seller Asset " + (i + 1) + ": " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Creates tasks to create buyer assets.
     */
    private static List<Supplier<String>> createBuyerAssetsTasks(int count, BigchainDBJavaDriver driver, KeyPair buyerKeyPair) {
        return createTasks(count, "Buyer Asset", i -> {
            try {
                String createId = Simulation.createCreate(driver, buyerKeyPair);
                if (createId != null) {
                    println("Buyer Asset " + (i + 1) + " Created: " + createId);
                    return createId;
                } else {
                    printerr("Failed to create Buyer Asset " + (i + 1));
                    return null;
                }
            } catch (Exception e) {
                printerr("Exception creating Buyer Asset " + (i + 1) + ": " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Creates tasks to create advertisements for each seller asset.
     */
    private static List<Supplier<String>> createAdvertisementsTasks(List<String> sellerCreateIds, BigchainDBJavaDriver driver, KeyPair sellerKeyPair) {
        return createTasks(sellerCreateIds.size(), "Advertisement", i -> {
            try {
                if (sellerCreateIds.size() > i) {
                    String createId = sellerCreateIds.get(i);
                    Transaction advTransaction = Simulation.createAdv(driver, sellerKeyPair, createId, "Open");
                    if (advTransaction != null && advTransaction.getId() != null) {
                        println("Advertisement " + (i + 1) + " Created: " + advTransaction.getId());
                        return advTransaction.getId();
                    } else {
                        printerr("Failed to create Advertisement for Seller Asset " + (i + 1));
                        return null;
                    }
                } else {
                    printerr("Seller Asset not available for Advertisement at index: " + i);
                    return null;
                }
            } catch (Exception e) {
                printerr("Exception creating Advertisement " + (i + 1) + ": " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Creates tasks to create buy offers for each valid advertisement.
     */
    private static List<Supplier<String>> createBuyOfferTasks(List<String> advIds, List<String> buyerCreateIds, BigchainDBJavaDriver driver, KeyPair buyerKeyPair) {
        int count = Math.min(advIds.size(), buyerCreateIds.size());
        return createTasks(count, "Buy Offer", i -> {
            try {
                String advId = advIds.get(i);
                String buyerCreateId = buyerCreateIds.get(i);
                Transaction committedAdvTx = TransactionsApi.waitForCommitWorkflow(advId);

                if (committedAdvTx != null) {
                    // Proceed with the buy offer creation after the advertisement is committed
                    Transaction buyOfferTransaction = Simulation.createBuyOffer(driver, buyerKeyPair, advId, buyerCreateId);
                    if (buyOfferTransaction != null && buyOfferTransaction.getId() != null) {
                        println("Buy Offer " + (i + 1) + " Created: " + buyOfferTransaction.getId());
                        return buyOfferTransaction.getId();
                    } else {
                        printerr("Failed to create Buy Offer " + (i + 1));
                        return null;
                    }
                } else {
                    printerr("Advertisement " + advId + " was not committed. Skipping Buy Offer creation.");
                    return null;
                }
                
            } catch (Exception e) {
                printerr("Exception creating Buy Offer " + (i + 1) + ": " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Creates tasks to create sell transactions for each valid advertisement and corresponding buy offer.
     */
    private static List<Supplier<String>> createSellTasks(List<String> sellerCreateIds, List<String> advIds, List<String> buyOfferIds, BigchainDBJavaDriver driver, KeyPair sellerKeyPair) {
        int count = Math.min(Math.min(sellerCreateIds.size(), advIds.size()), buyOfferIds.size());
        return createTasks(count, "Sell Transaction", i -> {
            try {
                String createId = sellerCreateIds.get(i);
                String advId = advIds.get(i);
                String buyOfferId = buyOfferIds.get(i);
                Transaction committedAdvTx = TransactionsApi.waitForCommitWorkflow(buyOfferId);

                if (committedAdvTx != null) {
                    Transaction sellTransaction = Simulation.createSell(driver, sellerKeyPair, createId, advId, buyOfferId);
                    if (sellTransaction != null && sellTransaction.getId() != null) {
                        println("Sell Transaction " + (i + 1) + " Created: " + sellTransaction.getId());
                        return sellTransaction.getId();
                    } else {
                        printerr("Failed to create Sell Transaction " + (i + 1));
                        return null;
                    }
                } else {
                    printerr("Advertisement " + advId + " was not committed. Skipping Buy Offer creation.");
                    return null;
                }
            } catch (Exception e) {
                printerr("Exception creating Sell Transaction " + (i + 1) + ": " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Creates tasks to handle returns for a subset of sell transactions.
     */
    private static List<Supplier<Void>> createReturnTasks(List<String> sellIds, List<String> buyOfferIds, BigchainDBJavaDriver driver, KeyPair sellerKeyPair, KeyPair buyerKeyPair, int count) {
        count = Math.min(count, sellIds.size());
        return createTasks(count, "Return Handling", i -> {
            try {
                String sellId = sellIds.get(i);
                Transaction committedAdvTx = TransactionsApi.waitForCommitWorkflow(sellId);

                if (committedAdvTx != null) {
                    List<String> transferTxns = TransactionsApi.getTransferTransactionIdsByAssetId(sellId);
                
                if (!transferTxns.isEmpty()) {
                    Transaction inverse = Simulation.createReturnSell(driver, buyerKeyPair, sellId, transferTxns.get(0));
                    if (inverse != null && inverse.getId() != null) {
                        String buyOfferId = buyOfferIds.get(i);
                        List<String> returnTxns = TransactionsApi.getTransferTransactionIdsByAssetId(buyOfferId);
                        if (!returnTxns.isEmpty()) {
                            Transaction acceptReturn = Simulation.createAcceptReturn(driver, sellerKeyPair, buyOfferId, returnTxns.get(0), inverse.getId());
                            if (acceptReturn != null && acceptReturn.getId() != null) {
                                println("Return and Accept Return for Sell Transaction " + (i + 1) + " Completed");
                            } else {
                                printerr("Failed to create Accept Return for Sell Transaction " + (i + 1));
                            }
                        } else {
                            printerr("No return transactions available for Buy Offer ID: " + buyOfferId);
                        }
                    } else {
                        printerr("Failed to create Return Sell for Sell Transaction " + (i + 1));
                    }
                } else {
                    printerr("No transfer transactions available for Sell ID: " + sellId);
                }
            } else {
                printerr("Advertisement " + sellId + " was not committed. Skipping Buy Offer creation.");
                return null;
            }
            } catch (Exception e) {
                printerr("Exception in Return Handling for Sell Transaction " + (i + 1) + ": " + e.getMessage());
            }
            return null;
        });
    }

    /**
     * A generic method to create a list of Supplier tasks given a count, a descriptive name, and a task creator function.
     */
    private static <T> List<Supplier<T>> createTasks(int count, String taskType, Function<Integer, T> taskCreator) {
        List<Supplier<T>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int index = i;
            tasks.add(() -> {
                T result = taskCreator.apply(index);
                // Optional short delay: Remove or adjust as needed
                return result;
            });
        }
        return tasks;
    }

    /**
     * Executes a list of tasks using Promise.allSettled and processes the results.
     * Adds fulfilled values to the provided results list (if not null) and logs errors.
     */
    private static <T> java.util.concurrent.CompletableFuture<Void> executeAndProcess(List<Supplier<T>> tasks, String taskType, List<T> results) {
        return Promise.allSettled(tasks).thenAccept(settlements -> processSettlements(settlements, results, taskType));
    }

    /**
     * Processes a list of settlements by collecting successful results and logging failures.
     */
    private static <T> void processSettlements(List<Settlement<T>> settlements, List<T> results, String taskType) {
        for (int i = 0; i < settlements.size(); i++) {
            Settlement<T> settlement = settlements.get(i);
            int taskNumber = i + 1;
            if (settlement.isFulfilled() && settlement.getValue() != null) {
                if (results != null) {
                    results.add(settlement.getValue());
                }
            } else {
                String errorMessage = (settlement.getReason() != null)
                        ? settlement.getReason().getMessage() : "Unknown Error";
                printerr(taskType + " " + taskNumber + " failed: " + errorMessage);
            }
        }
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
