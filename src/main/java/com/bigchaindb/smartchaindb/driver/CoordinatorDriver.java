package com.bigchaindb.smartchaindb.driver;

import com.complexible.stardog.StardogException;
import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionPool;
import com.complexible.stardog.api.SelectQuery;
import com.stardog.stark.query.SelectQueryResult;

import java.util.HashMap;

public class CoordinatorDriver {

    public static void main(String[] args) {
        // getIdForTopics(topicToIdMap);
    }

    static HashMap<String, Integer> getIdForTopics(HashMap<String, Integer> topicToIdMap) {
        // function to store the topics and its corresponding ID's assigned to them. In
        // this case, we have simply used and iterator to assign the id's to the topic.
        try {
            ConnectionPool connectPool = DBConnectionPool.getPoolInstance();

            try (Connection connect = connectPool.obtain()) {
                try {
                    SelectQuery squery = connect.select("select DISTINCT ?o {\n" + "?s rdfs:domain ?o .\n" + "}");

                    SelectQueryResult sresult = squery.execute();

                    int i = 0;
                    while (sresult.hasNext()) {
                        String temp = sresult.next().get("o").toString();
                        topicToIdMap.put(temp.substring(42), i);
                        i++;
                    }

                } finally {
                    try {
                        connectPool.release(connect);
                    } catch (StardogException e) {
                        e.printStackTrace();
                    }
                    // connectPool.shutdown();
                }
            }
        } catch (StardogException e) {
            e.printStackTrace();
        }
        return topicToIdMap;
    }

    static void addManufacture() {
        // function to create a consumer group manufacturers in Kafka
        // ConsumerDriver.runConsumer();
    }
}
