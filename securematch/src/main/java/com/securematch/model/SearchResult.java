package com.securematch.model;

public class SearchResult {

    public String id;

    public double nameScore;
    public double phoneScore;
    public double dobScore;
    public double policyScore;

    public double rawScore;           // Score before completeness penalty
    public double fieldCompleteness;  // Percentage of fields provided (0.0 to 1.0)
    public int fieldsProvided;        // Count of fields provided (1-4)
    public double combinedScore;      // Final adjusted score

    public String decryptedName;
    public String decryptedPhone;
    public String decryptedDob;
    public String decryptedPolicy;

    public String confidenceLabel;
    public String decision;

    public boolean dobHardPass;
    public boolean policyHardPass;

    public boolean fraudAlert;

    public static String getConfidenceLabel(double score) {
        if (score >= 0.95) return "AUTO APPROVE";
        if (score >= 0.80) return "HIGH CONFIDENCE";
        if (score >= 0.60) return "MEDIUM CONFIDENCE";
        if (score >= 0.50) return "PARTIALLY VERIFIED";
        if (score >= 0.40) return "LOW CONFIDENCE";
        return "FRAUD ALERT";
    }

    public static String getDecision(double score) {
        if (score >= 0.95) return "Process claim automatically";
        if (score >= 0.80) return "Submit 1 supporting document";
        if (score >= 0.60) return "HOLD - Submit 2 documents + supervisor approval";
        if (score >= 0.50) return "MANUAL REVIEW - Provide additional fields";
        if (score >= 0.40) return "REJECT - Re-apply with fresh documents";
        return "FRAUD SUSPECTED - Security team alerted";
    }
}
