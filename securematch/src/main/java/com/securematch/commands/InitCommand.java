package com.securematch.commands;

import com.securematch.Banner;
import com.securematch.Config;
import com.securematch.database.SecureDatabase;
import picocli.CommandLine.Command;

@Command(
    name        = "init",
    description = "Initialize the secure encrypted database",
    mixinStandardHelpOptions = true
)
public class InitCommand implements Runnable {

    @Override
    public void run() {
        Banner.printSection("Initializing SecureMatch Database");

        String dbUrl  = Config.getRequired("DATABASE_URL");
        String dbUser = Config.getRequired("DB_USER");
        String dbPass = Config.getRequired("DB_PASSWORD");

        SecureDatabase db = new SecureDatabase(dbUrl, dbUser, dbPass);

        try {
            db.connect();
            Banner.printInfo("Connected to PostgreSQL");
            
            db.setupPatientTable();
            Banner.printSuccess("Database initialized successfully!");
            
            System.out.println();
            Banner.printProperty("Table", "patient_records");
            Banner.printProperty("Fields", "name, phone, dob, policy (all encrypted)");
            Banner.printProperty("Indexes", "GIN indexes on all token fields");
            Banner.printProperty("Plaintext Stored", "ZERO");
            System.out.println();

        } finally {
            db.close();
        }
    }
}
