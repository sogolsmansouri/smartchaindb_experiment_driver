package com.bigchaindb.smartchaindb.driver;


import com.bigchaindb.builders.BigchainDbConfigBuilder;
import com.bigchaindb.model.Connection;
import com.bigchaindb.model.GenericCallback;
import com.bigchaindb.model.MetaData;
import com.bigchaindb.model.Transaction;
import okhttp3.Response;

import java.security.KeyPair;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Random;

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

        setConfig();
        KeyPair keys = BigchainDBJavaDriver.getKeys();
        // KeyPair transferKeys = BigchainDBJavaDriver.getKeys();
        BigchainDBJavaDriver driver = new BigchainDBJavaDriver();

        // Run async ConsumerDriver
        //new ConsumerDriver(keys).run();

        // Execute your transactions
        Transaction rfq1 = null;
        Transaction bid1 = null;
        List<String> rfq_ids = new ArrayList<>();
        List<String> create_ids = new ArrayList<>();
        Transaction winningBid = null;
        int rfq_count = 100;
        int create_count = 2500;
        int bid_count = 2500;
        int bids_per_rfq = 25;
        int counter = 0;
        //create rfqs 
        for(int j = 0; j < rfq_count; j++) {
            Transaction rfq = Simulation.createRFQ(driver, keys, null);
            rfq_ids.add(rfq.getId());
        }
        // create assests seperately 
        for(int k = 0; k < create_count; k++) {
            String createId = Simulation.createCreate(driver, keys);
            create_ids.add(createId);
        }
        for(int i = 0; i  < rfq_ids.size() ; i++){
            String rfqId = rfq_ids.get(i);
            List<Transaction> bids = new ArrayList<>();
            for(int j = 0;  j < bids_per_rfq; j++) {
                String createId = create_ids.get(counter);
                counter ++;
                bids.add(Simulation.createBid(driver, keys, rfqId, createId));
            }
            Random rand = new Random();
            winningBid = bids.get(rand.nextInt(bids.size()));
            MetaData metaData = new MetaData();
            metaData.setMetaData("requestCreationTimestamp", LocalDateTime.now(Clock.systemUTC()).toString());
            Transactions.doAccept(driver, winningBid.getId(), rfqId, metaData, keys);
        }

        //Simulation.createBid(driver, keys, rfq.getId());

        // MetaData metaData = new MetaData();
        // metaData.setMetaData("requestCreationTimestamp", LocalDateTime.now(Clock.systemUTC()).toString());
        // Transactions.doAccept(driver, bid1.getId(), rfq1.getId(), metaData, keys);
    }

    /**
     * configures connection url and credentials
     */
    public static void setConfig() {
        // Single-Node Setup
        BigchainDbConfigBuilder.baseUrl("http://165.232.123.159:9984/").setup();

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
}
