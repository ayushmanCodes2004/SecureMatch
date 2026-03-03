package com.securematch.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {

    private static final String LOG_FILE = "audit.log";
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logSearch(
            int fieldsProvided,
            int matchCount,
            double topScore,
            boolean success) {

        String entry = String.format(
            "[%s] ACTION=MULTI_SEARCH  | FIELDS=%d/4 | MATCHES=%d | TOP_SCORE=%3.0f%% | STATUS=%s%n",
            LocalDateTime.now().format(FORMATTER),
            fieldsProvided,
            matchCount,
            topScore * 100,
            success ? "OK" : "ERROR"
        );

        write(entry);
    }

    public static void logAdd(
            int fieldsEncrypted,
            int tokenCount,
            boolean success) {

        String entry = String.format(
            "[%s] ACTION=MULTI_ADD     | FIELDS=%d/4 | TOKENS=%d  | TOP_SCORE=N/A | STATUS=%s%n",
            LocalDateTime.now().format(FORMATTER),
            fieldsEncrypted,
            tokenCount,
            success ? "OK" : "ERROR"
        );

        write(entry);
    }

    public static void logSingleSearch(
            int matchCount,
            double topScore,
            boolean success) {

        String entry = String.format(
            "[%s] ACTION=SINGLE_SEARCH | FIELDS=1/1 | MATCHES=%d | TOP_SCORE=%3.0f%% | STATUS=%s%n",
            LocalDateTime.now().format(FORMATTER),
            matchCount,
            topScore * 100,
            success ? "OK" : "ERROR"
        );

        write(entry);
    }

    public static void logSingleAdd(
            int tokenCount,
            boolean success) {

        String entry = String.format(
            "[%s] ACTION=SINGLE_ADD    | FIELDS=1/1 | TOKENS=%d  | TOP_SCORE=N/A | STATUS=%s%n",
            LocalDateTime.now().format(FORMATTER),
            tokenCount,
            success ? "OK" : "ERROR"
        );

        write(entry);
    }

    public static void logFraudAlert(
            int fieldsProvided,
            double score) {

        String entry = String.format(
            "[%s] ACTION=FRAUD_ALERT   | FIELDS=%d/4 | MATCHES=0 | TOP_SCORE=%3.0f%% | STATUS=ALERT%n",
            LocalDateTime.now().format(FORMATTER),
            fieldsProvided,
            score * 100
        );

        write(entry);

        System.out.println(
            "  [AUDIT] Fraud alert logged to audit.log"
        );
    }

    public static void logInit(boolean success) {

        String entry = String.format(
            "[%s] ACTION=DB_INIT       | FIELDS=N/A | MATCHES=N/A | TOP_SCORE=N/A | STATUS=%s%n",
            LocalDateTime.now().format(FORMATTER),
            success ? "OK" : "ERROR"
        );

        write(entry);
    }

    private static void write(String entry) {
        try {
            Path logPath = Paths.get(LOG_FILE);

            if (!Files.exists(logPath)) {
                String header =
                    "# SecureMatch Audit Log%n" +
                    "# Privacy-preserving: No PII logged%n" +
                    "# Format: [timestamp] ACTION | FIELDS | MATCHES | SCORE | STATUS%n" +
                    "#%n";
                Files.write(
                    logPath,
                    String.format(header).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            }

            Files.write(
                logPath,
                entry.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            System.err.println(
                "[!] Warning: Could not write to audit.log"
            );
        }
    }
}
