package com.bigchaindb.smartchaindb.driver;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class ConsumerDriver implements Runnable {

    private final KeyPair keys;

    public ConsumerDriver(KeyPair keys) {
        this.keys = keys;
    }

    public static Consumer<String, String> createConsumer(String consumerGroup) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DriverConstants.KAFKA_BROKERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        // TODO: need to decide what will be the key for rfq txn
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        // TODO: need to change this to json serializer
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, DriverConstants.MAX_POLL_RECORDS);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, DriverConstants.OFFSET_RESET_EARLIER);

        return new KafkaConsumer<>(props);
    }

    public void run() {
        final int maxCapabilityCount = 5;
        final Random random = new Random(System.nanoTime());

        final List<String> allTopics = new ArrayList<>(Capabilities.getAll());
        final List<String> randomTopics = new ArrayList<>();

        int supplierCount = random.nextInt(maxCapabilityCount) + 1;
        for (int k = 0; k < supplierCount; k++) {
            final int randomIndex = random.nextInt(allTopics.size());
            randomTopics.add(allTopics.get(randomIndex));
            allTopics.remove(randomIndex);
        }

        new ConsumerManager(randomTopics, keys);
    }
}