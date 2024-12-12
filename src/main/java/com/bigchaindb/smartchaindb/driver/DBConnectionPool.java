package com.bigchaindb.smartchaindb.driver;

import com.complexible.stardog.StardogException;
import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionConfiguration;
import com.complexible.stardog.api.ConnectionPool;
import com.complexible.stardog.api.ConnectionPoolConfig;
import com.complexible.stardog.api.admin.AdminConnection;
import com.complexible.stardog.api.admin.AdminConnectionConfiguration;
import com.stardog.stark.io.RDFFormats;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class DBConnectionPool {

    private static ConnectionPool poolInstance = null;

    public static void createConnectionPool() throws StardogException {
        try (AdminConnection connection = AdminConnectionConfiguration.toServer(DriverConstants.SERVER)
                .credentials(DriverConstants.ADMIN_USERNAME, DriverConstants.ADMIN_PASSWORD).connect()) {

            ConnectionConfiguration connectionConfig = ConnectionConfiguration.to(DriverConstants.PROCESS_DB)
                    .server(DriverConstants.SERVER)
                    .credentials(DriverConstants.ADMIN_USERNAME, DriverConstants.ADMIN_PASSWORD);

            ConnectionPoolConfig poolConfig = ConnectionPoolConfig.using(connectionConfig).minPool(500).maxPool(2500);
            poolInstance = poolConfig.create();
        }
    }

    public static Connection getConnection(String dbName) throws StardogException {
        ConnectionConfiguration connectionConfig = ConnectionConfiguration.to(dbName)
                .server(DriverConstants.SERVER)
                .credentials(DriverConstants.ADMIN_USERNAME, DriverConstants.ADMIN_PASSWORD);

        return connectionConfig.connect();
    }


    public static void destroyConnectionPool() {
        if (poolInstance != null) {
            poolInstance.shutdown();
        }
    }

    private static InputStream getFileFromResourceAsStream(String fileName) throws FileNotFoundException {

        ClassLoader classLoader = DBConnectionPool.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new FileNotFoundException("File not found! " + fileName);
        } else {
            return inputStream;
        }

    }

    private static void importOntology() {
        try (Connection connect = poolInstance.obtain()) {
            try {
                InputStream is = getFileFromResourceAsStream("ManuServiceOntology.xml");
                connect.begin();
                connect.add().io().format(RDFFormats.RDFXML)
                        .stream(is);
                connect.commit();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    poolInstance.release(connect);
                } catch (StardogException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static ConnectionPool getPoolInstance() {
        if (poolInstance == null) {
            createConnectionPool();
        }

        return poolInstance;
    }

    public static void shutdown() {
        poolInstance.shutdown();
    }
}