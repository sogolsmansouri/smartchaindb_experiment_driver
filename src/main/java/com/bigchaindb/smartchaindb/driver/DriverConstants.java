package com.bigchaindb.smartchaindb.driver;

import java.util.Arrays;
import java.util.List;

public class DriverConstants {
    // Generic constants
    protected static final String SMARTCHAINDB_PUBKEY = "7EAsHUGQ15LdS2NoX9543bgEoZg7BmkVqdxSmiSda3Dv";
    protected static final List<String> VALIDATOR_NODES = Arrays.asList("http://152.7.176.32:9984/",
            "http://152.7.176.34:9984/", "http://152.7.176.38:9984/", "http://152.7.177.221:9984/");

    // Kafka-related constants
    protected static final String KAFKA_BROKERS = "152.7.99.165:9092";
    protected static final String OFFSET_RESET_EARLIER = "earliest";
    protected static final Integer MAX_POLL_RECORDS = 1;
    protected static final String COMMON_TOPIC = "SystemQueue";

    // Stardog-related constants
    protected static final String SERVER = "http://152.7.178.14:5820";
    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "admin";
    protected static final String PROCESS_DB = "MARCO-ProcessTaxonomy";
    protected static final String PRODUCT_DB = "MARCO-Product";
    protected static final String COMMON_DB = "MARCO-Common";

    protected static final String GROUP_MANAGER_ENDPOINT = "http://localhost:8080/GJoin";
    protected static final boolean GROUP_SIGNATURE = false;
}