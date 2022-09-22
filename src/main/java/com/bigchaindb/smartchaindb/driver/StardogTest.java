package com.bigchaindb.smartchaindb.driver;

import com.complexible.stardog.StardogException;
import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionPool;
import com.complexible.stardog.api.SelectQuery;
import com.stardog.stark.Value;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.SelectQueryResult;

import java.util.*;

public class StardogTest {
    private static final String PREFIX = "<http://resourcedescription.tut.fi/ontology/processTaxonomyModel#";
    private static final HashSet<String> topics = Capabilities.getAll();
    private static final Random rand = new Random(System.nanoTime());
    private static final Map<String, String> reasonerCache = new HashMap<>();
    protected static List<String> keyList;
    private static List<String> capabilityList;
    private static List<String> materialList;
    private static Map<String, List<String>> values;

    /**
     * returns random number of keys for the request metadata.
     */
    static List<String> getKeys() {
        List<String> randomKeys = new ArrayList<>();

        if (keyList == null || keyList.isEmpty()) {
            try (Connection connect = DBConnectionPool.getConnection(DriverConstants.PRODUCT_DB)) {

                Set<String> keySet = new HashSet<>();
                SelectQuery squery = connect.select("select ?y where {\n"
                        + "?y rdf:type owl:Class \n"
                        + "}");
                SelectQueryResult result = squery.execute();

                while (result.hasNext()) {
                    Value next = result.next().get("y");
                    if (next != null && next.toString().contains("#")) {
                        String value = next.toString().split("#")[1];

                        if (!value.equals("Material") && !value.equals("Quantity")) {
                            keySet.add(value);
                        }
                    }
                }

                keyList = new ArrayList<>(keySet);
            } catch (StardogException e) {
                e.printStackTrace();
            }
        }

        // Random number of attributes
        int numOfKeys = rand.nextInt(6) + 2;
        for (int i = 0; i < numOfKeys; i++) {
            int index = rand.nextInt(keyList.size());
            randomKeys.add(keyList.get(index));
        }

        return randomKeys;
    }

    /**
     * returns random `Material` value.
     */
    static String getMaterial() {
        if (materialList == null || materialList.isEmpty()) {
            try (Connection connect = DBConnectionPool.getConnection(DriverConstants.COMMON_DB)) {

                materialList = new ArrayList<>();
                SelectQuery query = connect.select("SELECT ?y WHERE { ?y rdf:type "
                        + "<http://resourcedescription.tut.fi/ontology/commonConcepts#MaterialType> }");
                SelectQueryResult result = query.execute();

                while (result.hasNext()) {
                    Value next = result.next().get("y");

                    if (next.toString().contains("#")) {
                        materialList.add(next.toString().split("#")[1]);
                    }
                }

            } catch (StardogException e) {
                e.printStackTrace();
            }
        }

        int randIndex = rand.nextInt(materialList.size());
        return materialList.get(randIndex);
    }

    /**
     * returns random `Quantity` value.
     */
    static String getQuantity() {
        int num = rand.nextInt(10000);
        return Integer.toString(num);
    }

    /**
     * returns random value for the arg `key`.
     *
     * @param key
     */
    static String getRandomValues(String key) {
        if (values == null || !values.containsKey(key)) {
            values = new HashMap<>();
            try (Connection connect = DBConnectionPool.getConnection(DriverConstants.PRODUCT_DB)) {

                List<String> valueList = new ArrayList<>();
                String query = "SELECT ?y WHERE { ?y owl:disjointWith "
                        + "<http://resourcedescription.tut.fi/ontology/productModel#" + key + "> " +
                        "}";
                SelectQuery selectQuery = connect.select(query);
                SelectQueryResult result = selectQuery.execute();

                while (result.hasNext()) {
                    Value value = result.next().get("y");
                    if (value.toString().contains("#")) {
                        valueList.add(value.toString().split("#")[1]);
                    }
                }

                values.put(key, valueList);
            } catch (StardogException e) {
                e.printStackTrace();
            }
        }

        List<String> valueList = values.get(key);
        if (valueList.isEmpty()) {
            valueList.add("Miscellaneous");
        }

        int index = rand.nextInt(valueList.size());
        return valueList.get(index);
    }

    /**
     * returns matched topic for every capability in arg `capabilities`.
     * uses ontological reasoning to map capability to its superclass
     * that exists as topic on the Kafka message broker.
     *
     * @param capabilities
     * @return matchedTopics
     */
    public static List<String> getCapabilityTopic(List<String> capabilities) {
        List<String> matchedTopics = new ArrayList<>();
        try {
            ConnectionPool connectPool = DBConnectionPool.getPoolInstance();

            for (String capability : capabilities) {
                if (!reasonerCache.containsKey(capability)) {
                    Connection connect = connectPool.obtain();
                    String matchedTopic = Capabilities.MISC;

                    if (topics.contains(capability)) {
                        matchedTopic = capability;

                    } else {
                        StringBuilder query = new StringBuilder();
                        query.append("SELECT ?superclass (count(?mid) as ?rank) \nWHERE { \n");
                        query.append(PREFIX + capability + ">").append(" rdfs:subClassOf* ?mid .\n");
                        query.append("?mid rdfs:subClassOf* ?superclass .\n");
                        query.append("}\n");
                        query.append("group by ?superclass\n");
                        query.append("order by ?rank");

                        SelectQuery squery = connect.select(query.toString());
                        SelectQueryResult sresult = squery.execute();

                        while (sresult.hasNext()) {
                            String current_superclass = sresult.next().resource("superclass").get().toString()
                                    .substring(64);
                            if (topics.contains(current_superclass)) {
                                matchedTopic = current_superclass;
                                break;
                            }
                        }
                    }
                    reasonerCache.put(capability, matchedTopic);
                }

                matchedTopics.add(reasonerCache.get(capability));
//                matchedTopics.add(matchedTopic);
            }

        } catch (StardogException e) {
            e.printStackTrace();
        }

        return matchedTopics;
    }

    public static List<String> getRandomCapability() {
        populateCapabilityList();

        Random rand = new Random();
        List<String> randomCapabilityList = new ArrayList<>();
        int randCount = rand.nextInt(3) + 1;

        for (int i = 0; i < randCount; i++) {
            int randIndex = rand.nextInt(capabilityList.size());
            randomCapabilityList.add(capabilityList.get(randIndex));
        }

        return randomCapabilityList;
    }

    private static void populateCapabilityList() {
        if (capabilityList == null) {
            capabilityList = new ArrayList<>();
            try {
                ConnectionPool connectPool = DBConnectionPool.getPoolInstance();
                Connection connect = connectPool.obtain();

                String query = "SELECT ?subclass WHERE {\n" +
                        "?subclass rdfs:subClassOf* ?intermediate .\n" +
                        "?intermediate rdfs:subClassOf* " + PREFIX + "ProcessTaxonomyElement> .\n" +
                        "}\n" +
                        "group by ?subclass\n" +
                        "HAVING (count(?intermediate)-1 > 1)";
                SelectQuery squery = connect.select(query);
                SelectQueryResult sresult = squery.execute();

                while (sresult.hasNext()) {
                    capabilityList.add(sresult.next().resource("subclass").get().toString().substring(64));
                }
            } catch (StardogException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Demo Method: infers requested manufacturing processes for the
     * arg `requestLists`
     */
    public static void reasoning() {

        final String prefix = "<http://www.manunetwork.com/manuservice/v1#";

        final List<List<String>> requestLists = Arrays.asList(
                Arrays.asList("maxDepthOfCut", "diameterOfPlanarFace", "stockInAxialDirection",
                        "diameterOfInnerCylinder"),
                Arrays.asList("maxDepthOfCut", "diameterOfHole", "maxDiameterOfHole", "minDiameterOfHole",
                        "lengthOfHole", "diameterOfPredrilledHole"),
                Arrays.asList("radiusOfSmallestConcaveProfileBlend", "angleOfRisingFlank", "lengthOfProfile",
                        "maxDepthOfCut", "stockInRadialDirection", "qualityOfFace"),
                Arrays.asList("depthOfSlot", "widthOfSlot", "maxWidthOfSlot", "minWidthOfSlot", "angleOfWallValue",
                        "qualityOfWallSurface"),
                Arrays.asList("diameterOfPlanarSplitFace", "diameterOfInnerHole", "maxWidthOfRemovedMaterialZone"));
        try {
            ConnectionPool connectPool = DBConnectionPool.getPoolInstance();
            Connection connect = connectPool.obtain();

            for (List<String> requestList : requestLists) {
                String inferred_process = "ERROR: Could not infer!";

                StringBuilder query = new StringBuilder();
                query.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
                query.append("SELECT ?feature ?superfeature \nWHERE { \n");
                for (String param : requestList) {
                    query.append(prefix + param + ">").append(" rdfs:domain ?feature .\n");
                }
                query.append("?feature rdfs:subClassOf ?superfeature .\n");
                query.append("}");

                SelectQuery squery = connect.select(query.toString());
                SelectQueryResult sresult = squery.execute();

                System.out.println("\n\nINFERRED FEATURES:");
                while (sresult.hasNext()) {
                    BindingSet current_row = sresult.next();

                    inferred_process = current_row.resource("superfeature").get().toString();
                    System.out.println(current_row.resource("feature").get().toString().substring(42));
                }

                System.out.println("\nINFERRED MANUFACTURING PROCESS: \n" + inferred_process.substring(42));
            }
        } catch (StardogException e) {
            e.printStackTrace();
        }
    }
}
