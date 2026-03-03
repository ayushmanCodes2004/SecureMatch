package com.securematch.commands;

import com.securematch.Config;
import com.securematch.crypto.SecureTokenizer;
import com.securematch.database.SecureDatabase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.UUID;

@Command(
    name        = "add",
    description = "Encrypt and store a record in the secure database",
    mixinStandardHelpOptions = true
)
public class AddCommand implements Runnable {

    @Option(
        names       = {"--text", "-t"},
        description = "Plaintext to encrypt and store",
        required    = true
    )
    private String text;

    @Override
    public void run() {
        System.out.println();
        System.out.println("[+] Encrypting record...");

        String secretKey = Config.getRequired("SECRET_KEY");
        String dbUrl     = Config.getRequired("DATABASE_URL");
        String dbUser    = Config.getRequired("DB_USER");
        String dbPass    = Config.getRequired("DB_PASSWORD");

        SecureTokenizer tokenizer = new SecureTokenizer(secretKey);

        String salt = tokenizer.generateSalt();
        List<String> tokens = tokenizer.tokenize(text, salt);

        if (tokens.isEmpty()) {
            System.out.println("[X] Text too short. Minimum 3 characters required.");
            return;
        }

        String recordId = UUID.randomUUID().toString();
        String encryptedData = tokenizer.encryptAES(text);

        SecureDatabase db = new SecureDatabase(dbUrl, dbUser, dbPass);

        try {
            db.connect();
            db.setup();
            db.insertRecord(recordId, salt, tokens, encryptedData);

            System.out.println();
            System.out.println("[OK] Stored successfully!");
            System.out.println("[ID] Record ID  : " + recordId);
            System.out.println("[+] Salt       : " + salt);
            System.out.println("[+] Tokens     : " + tokens.size() + " encrypted tokens stored");
            System.out.println();

        } finally {
            db.close();
        }
    }
}
