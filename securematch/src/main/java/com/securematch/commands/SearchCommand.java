package com.securematch.commands;

import com.securematch.Config;
import com.securematch.crypto.SecureTokenizer;
import com.securematch.database.SecureDatabase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;

@Command(
    name        = "search",
    description = "Search encrypted database with fuzzy matching",
    mixinStandardHelpOptions = true
)
public class SearchCommand implements Runnable {

    @Option(
        names       = {"--query", "-q"},
        description = "Search query",
        required    = true
    )
    private String query;

    @Option(
        names       = {"--threshold", "-t"},
        description = "Similarity threshold (0.0-1.0)",
        defaultValue = "0.3"
    )
    private double threshold;

    @Override
    public void run() {
        System.out.println();
        System.out.println("[+] Searching encrypted database...");
        System.out.println("[+] Query: " + query);
        System.out.println("[+] Threshold: " + threshold);
        System.out.println();

        String secretKey = Config.getRequired("SECRET_KEY");
        String dbUrl     = Config.getRequired("DATABASE_URL");
        String dbUser    = Config.getRequired("DB_USER");
        String dbPass    = Config.getRequired("DB_PASSWORD");

        SecureTokenizer tokenizer = new SecureTokenizer(secretKey);
        SecureDatabase db = new SecureDatabase(dbUrl, dbUser, dbPass);

        try {
            db.connect();
            List<Map<String, Object>> allRecords = db.getAllRecords();

            int matchCount = 0;

            for (Map<String, Object> record : allRecords) {
                String salt = (String) record.get("salt");
                @SuppressWarnings("unchecked")
                List<String> storedTokens = (List<String>) record.get("token_set");

                List<String> queryTokens = tokenizer.tokenize(query, salt);
                double score = tokenizer.similarity(queryTokens, storedTokens);

                if (score >= threshold) {
                    matchCount++;
                    String encryptedData = (String) record.get("encrypted_data");
                    String decrypted = tokenizer.decryptAES(encryptedData);

                    System.out.println("[MATCH] Score: " + String.format("%.2f", score * 100) + "%");
                    System.out.println("        ID: " + record.get("id"));
                    System.out.println("        Data: " + decrypted);
                    System.out.println();
                }
            }

            if (matchCount == 0) {
                System.out.println("[!] No matches found above threshold " + threshold);
            } else {
                System.out.println("[OK] Found " + matchCount + " match(es)");
            }
            System.out.println();

        } finally {
            db.close();
        }
    }
}
