package com.securematch.database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecureDatabase {

    private Connection connection;

    private final String url;
    private final String user;
    private final String password;

    public SecureDatabase(String url, String user, String password) {
        this.url      = url;
        this.user     = user;
        this.password = password;
    }

    public void connect() {
        try {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "[X] PostgreSQL JDBC driver not found.\n" +
                "   Check pom.xml has postgresql dependency.", e
            );
        } catch (SQLException e) {
            throw new RuntimeException(
                "[X] Cannot connect to PostgreSQL.\n" +
                "   URL: " + url + "\n" +
                "   Make sure PostgreSQL is running.\n" +
                "   Run: psql -U postgres -c \"CREATE DATABASE securematch;\"\n" +
                "   Error: " + e.getMessage(), e
            );
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("[!] Warning: Could not close DB connection: " + e.getMessage());
        }
    }

    public void setupPatientTable() {
        String createTable =
            "CREATE TABLE IF NOT EXISTS patient_records (" +
            "    id               UUID    PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    name_salt         TEXT    NOT NULL," +
            "    name_tokens       TEXT[]  NOT NULL," +
            "    name_encrypted    TEXT    NOT NULL," +
            "    phone_salt       TEXT    NOT NULL," +
            "    phone_tokens     TEXT[]  NOT NULL," +
            "    phone_encrypted  TEXT    NOT NULL," +
            "    dob_salt         TEXT    NOT NULL," +
            "    dob_tokens       TEXT[]  NOT NULL," +
            "    dob_encrypted    TEXT    NOT NULL," +
            "    policy_salt      TEXT    NOT NULL," +
            "    policy_tokens    TEXT[]  NOT NULL," +
            "    policy_encrypted TEXT    NOT NULL" +
            ")";

        String nameIndex    =
            "CREATE INDEX IF NOT EXISTS idx_name_tokens " +
            "ON patient_records USING GIN(name_tokens)";
        String phoneIndex  =
            "CREATE INDEX IF NOT EXISTS idx_phone_tokens " +
            "ON patient_records USING GIN(phone_tokens)";
        String dobIndex    =
            "CREATE INDEX IF NOT EXISTS idx_dob_tokens " +
            "ON patient_records USING GIN(dob_tokens)";
        String policyIndex =
            "CREATE INDEX IF NOT EXISTS idx_policy_tokens " +
            "ON patient_records USING GIN(policy_tokens)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
            stmt.execute(nameIndex);
            stmt.execute(phoneIndex);
            stmt.execute(dobIndex);
            stmt.execute(policyIndex);
        } catch (SQLException e) {
            throw new RuntimeException(
                "[X] Failed to create patient_records table: "
                + e.getMessage(), e
            );
        }
    }

    public void insertPatientRecord(com.securematch.model.PatientRecord record) {
        String sql =
            "INSERT INTO patient_records (" +
            "    id, " +
            "    name_salt, name_tokens, name_encrypted, " +
            "    phone_salt, phone_tokens, phone_encrypted, " +
            "    dob_salt, dob_tokens, dob_encrypted, " +
            "    policy_salt, policy_tokens, policy_encrypted" +
            ") VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, record.id);

            stmt.setString(2, record.nameSalt);
            stmt.setArray(3, connection.createArrayOf(
                "text", record.nameTokens.toArray(new String[0])
            ));
            stmt.setString(4, record.nameEncrypted);

            stmt.setString(5, record.phoneSalt);
            stmt.setArray(6, connection.createArrayOf(
                "text", record.phoneTokens.toArray(new String[0])
            ));
            stmt.setString(7, record.phoneEncrypted);

            stmt.setString(8, record.dobSalt);
            stmt.setArray(9, connection.createArrayOf(
                "text", record.dobTokens.toArray(new String[0])
            ));
            stmt.setString(10, record.dobEncrypted);

            stmt.setString(11, record.policySalt);
            stmt.setArray(12, connection.createArrayOf(
                "text", record.policyTokens.toArray(new String[0])
            ));
            stmt.setString(13, record.policyEncrypted);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                "[X] Failed to insert patient record: "
                + e.getMessage(), e
            );
        }
    }

    public List<com.securematch.model.PatientRecord> getPatientPage(
            int pageNumber,
            int pageSize) {

        int offset = pageNumber * pageSize;

        String sql =
            "SELECT id::text, " +
            "name_salt, name_tokens, name_encrypted, " +
            "phone_salt, phone_tokens, phone_encrypted, " +
            "dob_salt, dob_tokens, dob_encrypted, " +
            "policy_salt, policy_tokens, policy_encrypted " +
            "FROM patient_records " +
            "ORDER BY id " +
            "LIMIT ? OFFSET ?";

        List<com.securematch.model.PatientRecord> records = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                com.securematch.model.PatientRecord rec = 
                    new com.securematch.model.PatientRecord();
                rec.id = rs.getString("id");

                rec.nameSalt      = rs.getString("name_salt");
                rec.nameEncrypted = rs.getString("name_encrypted");
                rec.nameTokens    = arrayToList(rs.getArray("name_tokens"));

                rec.phoneSalt      = rs.getString("phone_salt");
                rec.phoneEncrypted = rs.getString("phone_encrypted");
                rec.phoneTokens    = arrayToList(rs.getArray("phone_tokens"));

                rec.dobSalt      = rs.getString("dob_salt");
                rec.dobEncrypted = rs.getString("dob_encrypted");
                rec.dobTokens    = arrayToList(rs.getArray("dob_tokens"));

                rec.policySalt      = rs.getString("policy_salt");
                rec.policyEncrypted = rs.getString("policy_encrypted");
                rec.policyTokens    = arrayToList(rs.getArray("policy_tokens"));

                records.add(rec);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                "[X] Failed to fetch patient page: "
                + e.getMessage(), e
            );
        }

        return records;
    }

    public int getPatientCount() {
        String sql = "SELECT COUNT(*) FROM patient_records";
        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(
                "[X] Failed to count patient records: "
                + e.getMessage(), e
            );
        }
    }

    private List<String> arrayToList(Array pgArray) throws SQLException {
        if (pgArray == null) return new ArrayList<>();
        String[] arr = (String[]) pgArray.getArray();
        return new ArrayList<>(Arrays.asList(arr));
    }

    public void setup() {
        String createTable =
            "CREATE TABLE IF NOT EXISTS secure_records (" +
            "    id             UUID    PRIMARY KEY DEFAULT gen_random_uuid()," +
            "    salt           TEXT    NOT NULL," +
            "    token_set      TEXT[]  NOT NULL," +
            "    encrypted_data TEXT    NOT NULL DEFAULT ''" +
            ")";

        String createIndex =
            "CREATE INDEX IF NOT EXISTS idx_token_set " +
            "ON secure_records USING GIN(token_set)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
            try {
                stmt.execute(
                    "ALTER TABLE secure_records " +
                    "ADD COLUMN IF NOT EXISTS encrypted_data TEXT NOT NULL DEFAULT ''"
                );
            } catch (SQLException ignored) {
            }
            stmt.execute(createIndex);
        } catch (SQLException e) {
            throw new RuntimeException("[X] Failed to create table: " + e.getMessage(), e);
        }
    }

    public void insertRecord(
            String id,
            String salt,
            List<String> tokens,
            String encryptedData) {

        String sql =
            "INSERT INTO secure_records (id, salt, token_set, encrypted_data) " +
            "VALUES (?::uuid, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, salt);

            Array pgArray = connection.createArrayOf(
                "text", tokens.toArray(new String[0])
            );
            stmt.setArray(3, pgArray);
            stmt.setString(4, encryptedData);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("[X] Failed to insert record: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getAllRecords() {
        String sql =
            "SELECT id::text, salt, token_set, encrypted_data " +
            "FROM secure_records";

        List<Map<String, Object>> records = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();

                record.put("id",             rs.getString("id"));
                record.put("salt",           rs.getString("salt"));
                record.put("encrypted_data", rs.getString("encrypted_data"));

                Array pgArray = rs.getArray("token_set");
                if (pgArray != null) {
                    String[] arr = (String[]) pgArray.getArray();
                    record.put("token_set", new ArrayList<>(Arrays.asList(arr)));
                } else {
                    record.put("token_set", new ArrayList<String>());
                }

                records.add(record);
            }

        } catch (SQLException e) {
            throw new RuntimeException("[X] Failed to fetch records: " + e.getMessage(), e);
        }

        return records;
    }

    public int getCount() {
        String sql = "SELECT COUNT(*) FROM secure_records";

        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("[X] Failed to count records: " + e.getMessage(), e);
        }
    }

    public void clearAll() {
        try (Statement stmt = connection.createStatement()) {
            int deleted = stmt.executeUpdate("DELETE FROM secure_records");
            System.out.println("[DEL] Cleared " + deleted + " records.");
        } catch (SQLException e) {
            throw new RuntimeException("[X] Failed to clear records: " + e.getMessage(), e);
        }
    }
}