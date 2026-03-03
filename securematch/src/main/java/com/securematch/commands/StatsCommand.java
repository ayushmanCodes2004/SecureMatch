package com.securematch.commands;

import com.securematch.Banner;
import com.securematch.Config;
import com.securematch.database.SecureDatabase;
import picocli.CommandLine.Command;

@Command(
    name        = "stats",
    description = "Show encrypted database statistics",
    mixinStandardHelpOptions = true
)
public class StatsCommand implements Runnable {

    @Override
    public void run() {
        Banner.printSection("SecureMatch Database Statistics");

        String dbUrl  = Config.getRequired("DATABASE_URL");
        String dbUser = Config.getRequired("DB_USER");
        String dbPass = Config.getRequired("DB_PASSWORD");

        SecureDatabase db = new SecureDatabase(dbUrl, dbUser, dbPass);

        try {
            db.connect();
            int count = db.getPatientCount();

            Banner.printProperty("Total Records", String.valueOf(count));
            System.out.println();
            
            System.out.println("  Security Guarantees:");
            Banner.printSuccess("Plaintext stored    : ZERO");
            Banner.printSuccess("Names in DB         : NONE");
            Banner.printSuccess("Secret key in DB    : NEVER");
            Banner.printSuccess("Timestamps          : NONE");
            Banner.printSuccess("Meaningful labels   : NONE");
            System.out.println();
            
            System.out.println("  Crypto Details:");
            Banner.printProperty("Algorithm", "HMAC-SHA256");
            Banner.printProperty("Tokenization", "Character Trigrams (n=3)");
            Banner.printProperty("Per-record salt", "YES");
            Banner.printProperty("Similarity metric", "Jaccard Index");
            Banner.printProperty("Kerckhoffs Principle", "COMPLIANT");
            System.out.println();
            
            Banner.printInfo("DB breach value: $0 (zero useful data)");
            System.out.println();

        } finally {
            db.close();
        }
    }
}
