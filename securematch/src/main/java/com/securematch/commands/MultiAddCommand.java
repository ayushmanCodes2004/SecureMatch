package com.securematch.commands;

import com.securematch.Config;
import com.securematch.crypto.SecureTokenizer;
import com.securematch.database.SecureDatabase;
import com.securematch.model.PatientRecord;
import com.securematch.util.AuditLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.UUID;

@Command(
    name        = "multi-add",
    description = "Encrypt and store multi-field patient record for insurance verification",
    mixinStandardHelpOptions = true
)
public class MultiAddCommand implements Runnable {

    @Option(
        names       = {"--name", "-n"},
        description = "Patient name (e.g. 'Venkateshwar Rao')",
        required    = true
    )
    private String name;

    @Option(
        names       = {"--phone", "-p"},
        description = "Phone number (e.g. '9876543210')",
        required    = true
    )
    private String phone;

    @Option(
        names       = {"--dob", "-d"},
        description = "Date of birth (e.g. '15-08-1985')",
        required    = true
    )
    private String dob;

    @Option(
        names       = {"--policy", "-pol"},
        description = "Insurance policy number (e.g. 'POL123456')",
        required    = true
    )
    private String policy;

    @Override
    public void run() {
        System.out.println();
        System.out.println("  ========================================");
        System.out.println("  INSURANCE PATIENT RECORD ENCRYPTION");
        System.out.println("  ========================================");
        System.out.println();

        String secretKey = Config.getRequired("SECRET_KEY");
        String dbUrl     = Config.getRequired("DATABASE_URL");
        String dbUser    = Config.getRequired("DB_USER");
        String dbPass    = Config.getRequired("DB_PASSWORD");

        SecureTokenizer tokenizer = new SecureTokenizer(secretKey);
        PatientRecord record = new PatientRecord(UUID.randomUUID().toString());

        System.out.println("  [i] Encrypting each field independently...");
        System.out.println();

        record.nameSalt      = tokenizer.generateSalt();
        record.nameTokens    = tokenizer.tokenize(name, record.nameSalt);
        record.nameEncrypted = tokenizer.encryptAES(name);
        System.out.println("  [+] Name    : " + record.nameTokens.size()
            + " HMAC tokens + AES-256 encrypted");

        record.phoneSalt      = tokenizer.generateSalt();
        record.phoneTokens    = tokenizer.tokenize(phone, record.phoneSalt);
        record.phoneEncrypted = tokenizer.encryptAES(phone);
        System.out.println("  [+] Phone   : " + record.phoneTokens.size()
            + " HMAC tokens + AES-256 encrypted");

        record.dobSalt      = tokenizer.generateSalt();
        record.dobTokens    = tokenizer.tokenize(dob, record.dobSalt);
        record.dobEncrypted = tokenizer.encryptAES(dob);
        System.out.println("  [+] DOB     : " + record.dobTokens.size()
            + " HMAC tokens + AES-256 encrypted");

        record.policySalt      = tokenizer.generateSalt();
        record.policyTokens    = tokenizer.tokenize(policy, record.policySalt);
        record.policyEncrypted = tokenizer.encryptAES(policy);
        System.out.println("  [+] Policy  : " + record.policyTokens.size()
            + " HMAC tokens + AES-256 encrypted");

        SecureDatabase db = new SecureDatabase(dbUrl, dbUser, dbPass);

        try {
            db.connect();
            db.setupPatientTable();
            db.insertPatientRecord(record);

            System.out.println();
            System.out.println("  ========================================");
            System.out.println("  [OK] Patient record stored successfully!");
            System.out.println("  Record ID : " + record.id);
            System.out.println();
            System.out.println("  Server NEVER saw:");
            System.out.println("  [x] Name   : '" + name + "'");
            System.out.println("  [x] Phone  : '" + phone + "'");
            System.out.println("  [x] DOB    : '" + dob + "'");
            System.out.println("  [x] Policy : '" + policy + "'");
            System.out.println("  ========================================");
            System.out.println();

            int totalTokens = record.nameTokens.size()
                            + record.phoneTokens.size()
                            + record.dobTokens.size()
                            + record.policyTokens.size();

            AuditLogger.logAdd(4, totalTokens, true);
            System.out.println("  [AUDIT] Action logged to audit.log");
            System.out.println();

        } catch (Exception e) {
            AuditLogger.logAdd(4, 0, false);
            throw e;
        } finally {
            db.close();
        }
    }
}
