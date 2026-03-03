package com.securematch.commands;

import com.securematch.Config;
import com.securematch.crypto.SecureTokenizer;
import com.securematch.database.SecureDatabase;
import com.securematch.model.PatientRecord;
import com.securematch.model.SearchResult;
import com.securematch.util.AuditLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;

@Command(
    name        = "multi-search",
    description = "Multi-field fuzzy SSE search for insurance claim verification",
    mixinStandardHelpOptions = true
)
public class MultiSearchCommand implements Runnable {

    @Option(names = {"--name",   "-n"},   description = "Patient name")
    private String name;

    @Option(names = {"--phone",  "-p"},   description = "Phone number")
    private String phone;

    @Option(names = {"--dob",    "-d"},   description = "Date of birth (DD-MM-YYYY)")
    private String dob;

    @Option(names = {"--policy", "-pol"}, description = "Policy number")
    private String policy;

    @Option(
        names        = {"--page-size"},
        description  = "Records per batch (default: 500)",
        defaultValue = "500"
    )
    private int pageSize;

    @Option(
        names        = {"--top"},
        description  = "Show top N results (default: 5)",
        defaultValue = "5"
    )
    private int topResults;

    private static final double WEIGHT_DOB    = 0.35;
    private static final double WEIGHT_POLICY = 0.30;
    private static final double WEIGHT_PHONE  = 0.20;
    private static final double WEIGHT_NAME   = 0.15;

    private static final double HARD_MIN_DOB    = 0.90;
    private static final double HARD_MIN_POLICY = 0.85;

    private static final double THRESHOLD_AUTO_APPROVE  = 0.95;
    private static final double THRESHOLD_HIGH          = 0.80;
    private static final double THRESHOLD_MEDIUM        = 0.60;
    private static final double THRESHOLD_LOW           = 0.40;

    @Override
    public void run() {

        // Validate: at least one field required
        if (name == null && phone == null && dob == null && policy == null) {
            System.out.println("  [!] Provide at least one search field.");
            System.out.println("  Usage: multi-search --name \"...\" --dob \"...\"");
            return;
        }

        System.out.println();
        System.out.println("  ========================================");
        System.out.println("  INSURANCE CLAIM VERIFICATION SEARCH");
        System.out.println("  ========================================");
        System.out.println();
        System.out.println("  Query Fields:");
        if (name   != null) System.out.println("  Name   : '" + name + "'");
        if (phone  != null) System.out.println("  Phone  : '" + phone + "'");
        if (dob    != null) System.out.println("  DOB    : '" + dob + "'");
        if (policy != null) System.out.println("  Policy : '" + policy + "'");
        System.out.println();

        String secretKey = Config.getRequired("SECRET_KEY");
        String dbUrl     = Config.getRequired("DATABASE_URL");
        String dbUser    = Config.getRequired("DB_USER");
        String dbPass    = Config.getRequired("DB_PASSWORD");

        SecureTokenizer tokenizer = new SecureTokenizer(secretKey);
        SecureDatabase  db        = new SecureDatabase(dbUrl, dbUser, dbPass);

        List<SearchResult> allMatches = new ArrayList<>();

        try {
            db.connect();

            int totalRecords = db.getPatientCount();
            int totalPages   = (int) Math.ceil((double) totalRecords / pageSize);

            if (totalRecords == 0) {
                System.out.println("  [!] No patient records found.");
                System.out.println(
                    "  Add some: securematch multi-add --name \"...\" --dob \"...\""
                );
                return;
            }

            System.out.println("  [i] Total records     : " + totalRecords);
            System.out.println("  [i] Batch size        : " + pageSize);
            System.out.println("  [i] Total batches     : " + totalPages);
            System.out.println("  [i] Server receives   : [encrypted trapdoors only]");
            System.out.println();
            System.out.println("  Scanning encrypted records...");
            System.out.println();

            for (int page = 0; page < totalPages; page++) {

                List<PatientRecord> batch = db.getPatientPage(page, pageSize);

                for (PatientRecord record : batch) {

                    SearchResult result = compareRecord(
                        record, tokenizer
                    );

                    if (result.combinedScore >= THRESHOLD_LOW
                            || result.fraudAlert) {
                        allMatches.add(result);
                    }
                }

                int progress = (int) (((double)(page + 1) / totalPages) * 20);
                String bar   = "[" + repeat("=", progress)
                             + repeat(" ", 20 - progress) + "]";
                System.out.printf(
                    "  %s Batch %d/%d | Matches: %d%n",
                    bar, page + 1, totalPages, allMatches.size()
                );

                batch.clear();
            }

        } finally {
            db.close();
        }

        allMatches.sort((a, b) ->
            Double.compare(b.combinedScore, a.combinedScore)
        );

        printResults(allMatches);

        int fieldsProvided = 0;
        if (name   != null) fieldsProvided++;
        if (phone  != null) fieldsProvided++;
        if (dob    != null) fieldsProvided++;
        if (policy != null) fieldsProvided++;

        double topScore = allMatches.isEmpty()
            ? 0.0
            : allMatches.get(0).combinedScore;

        AuditLogger.logSearch(
            fieldsProvided,
            allMatches.size(),
            topScore,
            true
        );

        for (SearchResult r : allMatches) {
            if (r.fraudAlert) {
                AuditLogger.logFraudAlert(fieldsProvided, r.combinedScore);
            }
        }

        System.out.println("  [AUDIT] Action logged to audit.log");
        System.out.println();
    }

    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private SearchResult compareRecord(
            PatientRecord record,
            SecureTokenizer tokenizer) {

        SearchResult result = new SearchResult();
        result.id = record.id;

        result.nameScore    = -1.0;
        result.phoneScore  = -1.0;
        result.dobScore    = -1.0;
        result.policyScore = -1.0;

        double totalWeight = 0.0;

        if (name   != null && !name.isEmpty())   totalWeight += WEIGHT_NAME;
        if (phone  != null && !phone.isEmpty())  totalWeight += WEIGHT_PHONE;
        if (dob    != null && !dob.isEmpty())    totalWeight += WEIGHT_DOB;
        if (policy != null && !policy.isEmpty()) totalWeight += WEIGHT_POLICY;

        if (totalWeight == 0.0) {
            result.combinedScore = 0.0;
            return result;
        }

        double weightedSum = 0.0;

        if (name != null && !name.isEmpty()) {
            List<String> trapdoor = tokenizer.generateTrapdoor(
                name, record.nameSalt
            );
            result.nameScore = tokenizer.similarity(
                trapdoor, record.nameTokens
            );
            double adjustedWeight = WEIGHT_NAME / totalWeight;
            weightedSum += result.nameScore * adjustedWeight;
        }

        if (phone != null && !phone.isEmpty()) {
            List<String> trapdoor = tokenizer.generateTrapdoor(
                phone, record.phoneSalt
            );
            result.phoneScore = tokenizer.similarity(
                trapdoor, record.phoneTokens
            );
            double adjustedWeight = WEIGHT_PHONE / totalWeight;
            weightedSum += result.phoneScore * adjustedWeight;
        }

        if (dob != null && !dob.isEmpty()) {
            List<String> trapdoor = tokenizer.generateTrapdoor(
                dob, record.dobSalt
            );
            result.dobScore = tokenizer.similarity(
                trapdoor, record.dobTokens
            );
            double adjustedWeight = WEIGHT_DOB / totalWeight;
            weightedSum += result.dobScore * adjustedWeight;
        }

        if (policy != null && !policy.isEmpty()) {
            List<String> trapdoor = tokenizer.generateTrapdoor(
                policy, record.policySalt
            );
            result.policyScore = tokenizer.similarity(
                trapdoor, record.policyTokens
            );
            double adjustedWeight = WEIGHT_POLICY / totalWeight;
            weightedSum += result.policyScore * adjustedWeight;
        }

        // Calculate field completeness
        int fieldsProvided = 0;
        if (name != null && !name.isEmpty()) fieldsProvided++;
        if (phone != null && !phone.isEmpty()) fieldsProvided++;
        if (dob != null && !dob.isEmpty()) fieldsProvided++;
        if (policy != null && !policy.isEmpty()) fieldsProvided++;

        double fieldCompleteness = fieldsProvided / 4.0;

        // Store raw score and apply completeness penalty
        result.rawScore = weightedSum;
        result.fieldCompleteness = fieldCompleteness;
        result.fieldsProvided = fieldsProvided;
        result.combinedScore = weightedSum * fieldCompleteness;

        result.dobHardPass    = (dob    == null
                                 || result.dobScore    >= HARD_MIN_DOB);
        result.policyHardPass = (policy == null
                                 || result.policyScore >= HARD_MIN_POLICY);

        if (!result.dobHardPass || !result.policyHardPass) {
            result.combinedScore = 0.0;
        }

        result.fraudAlert = result.combinedScore > 0.0
                         && result.combinedScore < THRESHOLD_LOW;

        result.confidenceLabel =
            SearchResult.getConfidenceLabel(result.combinedScore);
        result.decision =
            SearchResult.getDecision(result.combinedScore);

        if (result.combinedScore >= THRESHOLD_MEDIUM) {
            try {
                result.decryptedName    =
                    tokenizer.decryptAES(record.nameEncrypted);
                result.decryptedPhone  =
                    tokenizer.decryptAES(record.phoneEncrypted);
                result.decryptedDob    =
                    tokenizer.decryptAES(record.dobEncrypted);
                result.decryptedPolicy =
                    tokenizer.decryptAES(record.policyEncrypted);
            } catch (Exception e) {
                result.decryptedName = "[decryption error]";
            }
        } else {
            result.decryptedName    = "*** HIDDEN ***";
            result.decryptedPhone  = "*** HIDDEN ***";
            result.decryptedDob    = "*** HIDDEN ***";
            result.decryptedPolicy = "*** HIDDEN ***";
        }

        return result;
    }

    private void printResults(List<SearchResult> matches) {

        if (matches.isEmpty()) {
            System.out.println();
            System.out.println("  [x] No matches found above threshold.");
            System.out.println();
            return;
        }

        // Limit to top N results
        int displayCount = Math.min(matches.size(), topResults);
        List<SearchResult> topMatches = matches.subList(0, displayCount);

        System.out.println();
        System.out.println("  ========================================");
        System.out.println("  SEARCH RESULTS");
        System.out.println("  ========================================");
        System.out.println();
        System.out.println("  Total matches found: " + matches.size());
        System.out.println("  Showing top " + displayCount + " results");
        System.out.println();

        System.out.println("  Confidence Guide:");
        System.out.println("  AUTO APPROVE        (>= 95%) : Process automatically");
        System.out.println("  HIGH CONFIDENCE     (>= 80%) : Submit 1 document");
        System.out.println("  MEDIUM CONFIDENCE   (>= 60%) : Submit 2 documents + supervisor");
        System.out.println("  PARTIALLY VERIFIED  (>= 50%) : Manual review required");
        System.out.println("  LOW CONFIDENCE      (>= 40%) : Reject - re-apply");
        System.out.println("  FRAUD ALERT         (< 40%)  : Security investigation");
        System.out.println();

        for (int i = 0; i < topMatches.size(); i++) {
            SearchResult result = topMatches.get(i);
            
            System.out.println("  ----------------------------------------");
            System.out.println("  Result #" + (i + 1));
            System.out.println("  ----------------------------------------");
            System.out.println("  Record ID          : " + result.id.substring(0, 16) + "...");
            System.out.println("  Field Completeness : " + formatPercent(result.fieldCompleteness) 
                + " (" + result.fieldsProvided + "/4 fields)");
            System.out.println("  Raw Score          : " + formatPercent(result.rawScore));
            System.out.println("  Adjusted Score     : " + formatPercent(result.combinedScore));
            System.out.println("  Match Strength     : " + getProgressBar(result.combinedScore));
            System.out.println("  Confidence         : " + result.confidenceLabel);
            System.out.println("  Decision           : " + result.decision);
            System.out.println();
            
            System.out.println("  Field Scores:");

            if (name != null) {
                System.out.println("    Name   : " + formatPercent(result.nameScore) + " " + getProgressBar(result.nameScore));
            } else {
                System.out.println("    Name   : [NOT PROVIDED]");
            }

            if (phone != null) {
                System.out.println("    Phone  : " + formatPercent(result.phoneScore) + " " + getProgressBar(result.phoneScore));
            } else {
                System.out.println("    Phone  : [NOT PROVIDED]");
            }

            if (dob != null) {
                System.out.println("    DOB    : " + formatPercent(result.dobScore) + " " + getProgressBar(result.dobScore)
                    + (result.dobHardPass ? "" : " [HARD FAIL]"));
            } else {
                System.out.println("    DOB    : [NOT PROVIDED]");
            }

            if (policy != null) {
                System.out.println("    Policy : " + formatPercent(result.policyScore) + " " + getProgressBar(result.policyScore)
                    + (result.policyHardPass ? "" : " [HARD FAIL]"));
            } else {
                System.out.println("    Policy : [NOT PROVIDED]");
            }

            long providedCount = 0;
            if (name   != null) providedCount++;
            if (phone  != null) providedCount++;
            if (dob    != null) providedCount++;
            if (policy != null) providedCount++;
            
            if (providedCount < 4) {
                System.out.println();
                System.out.println("    [!] " + (4 - providedCount) + " field(s) not provided");
                System.out.println("    [!] Score based on " + providedCount + "/4 fields only");
                System.out.println("    [!] Provide more fields for accuracy");
            }
            System.out.println();

            if (result.combinedScore >= THRESHOLD_MEDIUM) {
                System.out.println("  Matched Record:");
                System.out.println("    Name   : " + result.decryptedName);
                System.out.println("    Phone  : " + result.decryptedPhone);
                System.out.println("    DOB    : " + result.decryptedDob);
                System.out.println("    Policy : " + result.decryptedPolicy);
            } else {
                System.out.println("  Matched Record: [HIDDEN - score below 60%]");
            }
            System.out.println();

            if (result.fraudAlert) {
                System.out.println("  [!] FRAUD ALERT: Score below 40% - investigate");
                System.out.println();
            }
        }

        System.out.println("  ========================================");
        System.out.println();
        System.out.println("  Security Summary:");
        System.out.println("  [i] Server received : [encrypted trapdoors only]");
        System.out.println("  [i] Server learned  : NOTHING about query or stored data");
        System.out.println("  [i] Names revealed  : ONLY above 60% confidence");
        System.out.println("  [i] Hard minimums   : DOB >= 90%, Policy >= 85%");
        System.out.println("  [i] Pagination      : " + pageSize + " records per batch");
        System.out.println("  ========================================");
        System.out.println();
    }

    private String formatPercent(double score) {
        return String.format("%d%%", (int) Math.round(score * 100));
    }

    private String getProgressBar(double score) {
        int percentage = (int) Math.round(score * 100);
        int filled = percentage / 5; // 20 blocks for 100%
        int empty = 20 - filled;
        
        String bar = "[" + repeat("█", filled) + repeat("░", empty) + "]";
        
        // Color coding based on confidence level
        if (score >= THRESHOLD_AUTO_APPROVE) {
            return bar + " ✓"; // Auto approve
        } else if (score >= THRESHOLD_HIGH) {
            return bar + " ↑"; // High confidence
        } else if (score >= THRESHOLD_MEDIUM) {
            return bar + " →"; // Medium confidence
        } else if (score >= THRESHOLD_LOW) {
            return bar + " ↓"; // Low confidence
        } else {
            return bar + " ⚠"; // Fraud alert
        }
    }
}
