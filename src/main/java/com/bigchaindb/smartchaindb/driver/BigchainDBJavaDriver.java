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
        for(int j = 0; j < 100; j++) {
            Transaction rfq = Simulation.createRFQ(driver, keys, null);
            rfq1 = rfq;
            for(int i = 0; i < 100 ; i++){
                Transaction bid = Simulation.createBid(driver, keys, rfq.getId(), null);
                bid1 = bid;
            }
        MetaData metaData = new MetaData();
        metaData.setMetaData("requestCreationTimestamp", LocalDateTime.now(Clock.systemUTC()).toString());
        Transactions.doAccept(driver, bid1.getId(), rfq1.getId(), metaData, keys);
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
        BigchainDbConfigBuilder.baseUrl("http://152.7.178.14:9984/").setup();

        // Multi-Node Setup
        List<Connection> connections = new ArrayList<>();
        for (String url : DriverConstants.VALIDATOR_NODES) {
            Map<String, Object> attributes = new TreeMap<>();
            attributes.put("baseUrl", url);
            connections.add(new Connection(attributes));
        }

        // BigchainDbConfigBuilder
        //        .addConnections(connections)
        //        .setTimeout(60000)
        //        .setup();
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