package com.securematch.model;

import java.util.List;

public class PatientRecord {

    public String id;

    public String nameSalt;
    public List<String> nameTokens;
    public String nameEncrypted;

    public String phoneSalt;
    public List<String> phoneTokens;
    public String phoneEncrypted;

    public String dobSalt;
    public List<String> dobTokens;
    public String dobEncrypted;

    public String policySalt;
    public List<String> policyTokens;
    public String policyEncrypted;

    public PatientRecord() {}

    public PatientRecord(String id) {
        this.id = id;
    }
}
