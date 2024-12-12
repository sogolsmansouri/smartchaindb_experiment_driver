package com.bigchaindb.smartchaindb.driver;

import com.bigchaindb.util.KeyPairUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.KeyPair;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class ConsumerManager {
    private final Consumer<String, String> consumer;
    protected Map<String, Map<String, String>> topicConditionMap;
    protected Set<String> subscribedTopics;
    protected List<JSONObject> requestList;
    protected Set<String> processedTransactionIds;
    private BufferedWriter rfqWriter;
    private KeyPair keys;

    ConsumerManager() {
        topicConditionMap = new HashMap<>();
        subscribedTopics = new HashSet<>();
        requestList = new ArrayList<>();
        processedTransactionIds = new HashSet<>();
        consumer = ConsumerDriver.createConsumer("supplier-" + LocalDateTime.now().toString());

        try {
            rfqWriter = new BufferedWriter(
                    new FileWriter("rfq-log-multi.csv", true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ConsumerManager(List<String> topics, KeyPair keys) {
        this();
        this.keys = keys;
        subscribedTopics = new HashSet<>(topics);
        subscribe(topics);
    }


    public void addTopic(List<String> newTopics) {
        subscribedTopics.addAll(newTopics);
        // TODO: add topic to consumer
    }

    public void unsubscribeTopics(List<String> topics) {
        subscribedTopics.removeAll(topics);
        // TODO: remove topic from consumer
    }

    public void addConditions(String topic, String conditionKey, String conditionValue) {
        Map<String, String> conditionMap = topicConditionMap.get(topic);
        conditionMap.put(conditionKey, conditionValue);
    }

    public void addConditions(String topic, Map<String, String> conditions) {
        Map<String, String> conditionMap = topicConditionMap.get(topic);
        conditionMap.putAll(conditions);
    }

    public Set<String> getSubscribedTopics() {
        return subscribedTopics;
    }

    public Map<String, Map<String, String>> getTopicConditionMap() {
        return topicConditionMap;
    }

    public void setTopicConditionMap(Map<String, Map<String, String>> topicConditionMap) {
        this.topicConditionMap = topicConditionMap;
    }

    private void subscribe(List<String> topics) {
        String pubKey = KeyPairUtils.encodePublicKeyInBase58((EdDSAPublicKey) keys.getPublic());
        consumer.subscribe(topics);

        while (true) {
            final ConsumerRecords<String, String> consumerRecords = consumer
                    .poll(Duration.ofMillis(100));

            consumerRecords.forEach(record -> {
                Map<String, String> conditionMap = topicConditionMap.get(record.topic());
                final JSONObject jsonReq = new JSONObject(record.value());
                System.out.println("\nRead offset: " + record.offset());

                String owner = jsonReq.getString("owner");
                if (!owner.equals(pubKey)) {
                    if (checkRequest(jsonReq)) {
                        requestList.add(jsonReq);
                    }
                }

                writeToLog(jsonReq, rfqWriter);
            });

            consumer.commitAsync();
        }

    }

    private void writeToLog(final JSONObject jsonReq, BufferedWriter writer) {
        LocalDateTime requestTimestamp = LocalDateTime.parse(jsonReq.getString("requestTimestamp"));
        LocalDateTime kafkaInTimestamp = LocalDateTime.parse(jsonReq.getString("kafkaInTimestamp"));
        LocalDateTime now = LocalDateTime.now();

        final long timeDifferenceInMillis1 = Duration.between(requestTimestamp, now).toMillis();
        final long timeDifferenceInMillis2 = Duration.between(kafkaInTimestamp, now).toMillis();
        final int productCount = jsonReq.getJSONArray("products").length();
        final int capabilityCount = Integer.parseInt(jsonReq.getString("CapabilityCount"));

        try {
            writer.write(requestTimestamp + "," + kafkaInTimestamp + "," + now + "," + timeDifferenceInMillis1
                    + "," + timeDifferenceInMillis2 + "," + productCount + "," + capabilityCount + "\n");
            writer.flush();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /*
    This method performs all the filters, stored in TopicConditionMap, on the request received on
    a particular topic.
     */
    private boolean checkConditions(final JSONObject jsonReq, Map<String, String> conditionMap) {
        boolean result = true;

        for (String key : conditionMap.keySet()) {
            JSONArray jsonArray = jsonReq.getJSONArray("products");
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject currentObject = jsonArray.getJSONObject(i);
                Map<String, String> productMetadata = gson.fromJson(currentObject.toString(), type);
            }
        }

        return true;
    }

    /*
    This method checks whether the requested capabilities are subset of the supplier's offered capabilities.
     */
    private boolean checkRequest(final JSONObject jsonReq) {
        List<String> capabilityList = new ArrayList<>();
        final JSONArray capabilitiesJsonArray = jsonReq.getJSONArray("InferredCapability");

        for (int i = 0; i < capabilitiesJsonArray.length(); i++) {
            capabilityList.add(capabilitiesJsonArray.getString(i));
        }

        return subscribedTopics.containsAll(new HashSet<>(capabilityList));
    }
}
