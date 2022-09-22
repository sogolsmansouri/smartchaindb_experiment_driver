package com.bigchaindb.smartchaindb.driver;

import java.util.*;

public class RulesDriver {

    public static final int NUM_OF_RULES = 100;
    private static final List<Map<String, String>> rules = new ArrayList<>();

    /**
     * generates random rules.
     * invoke this method before any other RulesDriver method.
     */
    public static void generateRules() {
        Random rand = new Random(System.nanoTime());
        if (StardogTest.keyList == null || StardogTest.keyList.isEmpty()) {
            StardogTest.getKeys();
        }

        List<String> keys = StardogTest.keyList;
        for (int i = 0; i < NUM_OF_RULES; i++) {
            // NOTE: NUM_OF_KEYS Range here is same as of StardogTest.getKeys().
            final int NUM_OF_KEYS = rand.nextInt(6) + 2;
            HashMap<String, String> rule = new HashMap<>();

            for (int j = 0; j < NUM_OF_KEYS; j++) {
                int index = rand.nextInt(keys.size());
                String key = keys.get(index);

                String value = "";
                if (key.equals("Quantity")) {
                    value = StardogTest.getQuantity();
                } else if (key.equals("Material")) {
                    value = StardogTest.getMaterial();
                } else {
                    value = StardogTest.getRandomValues(key);
                }

                rule.put(key, value);
            }

            rules.add(rule);
        }
    }

    /**
     * checks the meta attributes with the generated rules.
     * After reasoning, assigns some random capability topics
     */
    public static List<String> getCapabilities(Map<String, String> metaMap) {
        boolean matchFound = true;
        Random rand = new Random(System.nanoTime());
        HashSet<String> capSet = new HashSet<>();
        List<String> capabilities = new ArrayList<>();
        List<String> keys = new ArrayList<>(metaMap.keySet());
        List<String> topics = new ArrayList<>(Capabilities.getAll());

        for (int i = 0; i < NUM_OF_RULES; i++) {
            for (Map.Entry<String, String> entry : rules.get(i).entrySet()) {
                if (entry.getKey() != "Quantity") {
                    if (metaMap.get(entry.getKey()) != entry.getValue()) {
                        matchFound = false;
                        break;
                    }
                } else {
                    if (Integer.parseInt(String.valueOf(metaMap.get("Quantity"))) < 1000) {
                        matchFound = false;
                        break;
                    }
                }
            }
        }

        final int NUM_OF_CAPS = rand.nextInt(3) + 1;
        for (int k = 0; k < NUM_OF_CAPS; k++) {
            int index = rand.nextInt(topics.size());
            capSet.add(topics.get(index));
            topics.remove(index);
        }

        capabilities.addAll(capSet);
        /* if (capabilities.isEmpty()) {
            capabilities.add(Capabilities.MISC);
        } */

        return capabilities;
    }
}
