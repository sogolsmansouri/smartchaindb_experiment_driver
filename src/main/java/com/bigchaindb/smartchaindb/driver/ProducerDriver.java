package com.bigchaindb.smartchaindb.driver;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class ProducerDriver {

    public static Producer<String, String> createProducer(String clientId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, DriverConstants.KAFKA_BROKERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put("acks", "all");
        // TODO: need to decide what will be the key for rfq txn
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // TODO: need to change this to json serializer
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(props);
    }

    public static void produce(Producer<String, String> producer, String topic, String key, String req) {

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, req);
        try {
            RecordMetadata metadata = producer.send(record).get();

            System.out.println("Record -> topic " + topic + " at partition " + metadata.partition() + " with offset " + metadata.offset());
        } catch (ExecutionException e) {
            System.out.println("Error in sending record: " + e);
        } catch (InterruptedException e) {
            System.out.println("Error in sending record: " + e);
        }
    }
}
