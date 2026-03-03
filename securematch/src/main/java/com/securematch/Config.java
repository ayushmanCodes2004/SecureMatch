package com.securematch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private static final Map<String, String> envMap = new HashMap<>();

    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key   = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    envMap.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("[!] .env file not found. Using system environment variables.");
        }

        loaded = true;
    }

    public static String get(String key) {
        if (envMap.containsKey(key)) {
            return envMap.get(key);
        }
        return System.getenv(key);
    }

    public static String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            throw new RuntimeException(
                "\n[X] Required config '" + key + "' not found!\n" +
                "   Add this line to your .env file:\n" +
                "   " + key + "=yourvalue\n"
            );
        }
        return value;
    }
}
