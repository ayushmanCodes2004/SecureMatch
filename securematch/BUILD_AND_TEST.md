# Build and Test Instructions

## Prerequisites Check

```bash
# Check Java version (must be 17+)
java -version

# Check Maven version (must be 3.8+)
mvn -version

# Check PostgreSQL is running
psql -U postgres -c "SELECT version();"
```

## Step 1: Create Database

```bash
# Create the securematch database
psql -U postgres -c "CREATE DATABASE securematch;"

# Verify it was created
psql -U postgres -c "\l" | grep securematch
```

## Step 2: Configure Environment

Edit the `.env` file with your actual PostgreSQL credentials:

```bash
SECRET_KEY=a8f3k2p9x7m4n1q6r5t8w2y0z3b6c4d7e5f8g1h2i3j4k5
DATABASE_URL=jdbc:postgresql://localhost:5432/securematch
DB_USER=postgres
DB_PASSWORD=yourpassword
```

## Step 3: Build the Project

```bash
cd securematch

# Clean and build
mvn clean package

# Verify JAR was created
ls -lh target/securematch-1.0.0.jar
```

Expected output:
```
BUILD SUCCESS
target/securematch-1.0.0.jar  (~5MB)
```

## Step 4: Initialize Database

```bash
java -jar target/securematch-1.0.0.jar init
```

Expected output:
```
🔧 Initializing SecureMatch database...

✅ Secure database initialized!
🔒 Table     : secure_records
🔒 Columns   : id (UUID) | salt (TEXT) | token_set (TEXT[])
🔒 Index     : GIN index on token_set
🔒 Plaintext : ZERO stored
```

## Step 5: Add Test Records

```bash
java -jar target/securematch-1.0.0.jar add --text "John Smith"
java -jar target/securematch-1.0.0.jar add --text "Jane Doe"
java -jar target/securematch-1.0.0.jar add --text "Venkateshwar Rao"
java -jar target/securematch-1.0.0.jar add --text "Mohammed Ali"
java -jar target/securematch-1.0.0.jar add --text "Priya Krishnamurthy"
```

Expected output for each:
```
🔐 Encrypting record...

✅ Stored successfully!
🆔 Record ID  : 550e8400-e29b-41d4-a716-446655440000
🧂 Salt       : xK92mPqR  (stored openly, useless without key)
🔒 Tokens     : 16 encrypted tokens stored
🔐 Server NEVER saw: 'John Smith'
```

## Step 6: Test Fuzzy Search

### Test 1: Exact Match
```bash
java -jar target/securematch-1.0.0.jar search --query "John Smith"
```

Expected: Should find "John Smith" with ~100% similarity

### Test 2: Typo Handling
```bash
java -jar target/securematch-1.0.0.jar search --query "Jon Smith"
```

Expected: Should find "John Smith" with ~60-70% similarity

### Test 3: Indian Name Transliteration
```bash
java -jar target/securematch-1.0.0.jar search --query "Venkateswara Rao"
```

Expected: Should find "Venkateshwar Rao" with ~70-80% similarity

### Test 4: Lower Threshold
```bash
java -jar target/securematch-1.0.0.jar search --query "Mohammad Ali" --threshold 0.2
```

Expected: Should find "Mohammed Ali" with similarity above 20%

### Test 5: No Matches
```bash
java -jar target/securematch-1.0.0.jar search --query "Robert Johnson"
```

Expected: "No matches found" message

## Step 7: View Statistics

```bash
java -jar target/securematch-1.0.0.jar stats
```

Expected output:
```
📊 SecureMatch — Database Statistics
   ════════════════════════════════════════

   Total Records         : 5

   Security Guarantees:
   ✅ Plaintext stored    : ZERO
   ✅ Names in DB         : NONE
   ✅ Secret key in DB    : NEVER
   ✅ Timestamps          : NONE
   ✅ Meaningful labels   : NONE

   Crypto Details:
   🔒 Algorithm           : HMAC-SHA256
   🔒 Tokenization        : Character Trigrams (n=3)
   🔒 Per-record salt     : YES
   🔒 Similarity metric   : Jaccard Index
   🔒 Kerckhoffs          : COMPLIANT ✅

   DB breach value        : ₹0 (zero useful data) 🔐
```

## Step 8: Verify Database Contents

```bash
# Connect to database
psql -U postgres -d securematch

# View table structure
\d secure_records

# View encrypted data (should see NO plaintext)
SELECT id, salt, token_set FROM secure_records LIMIT 1;

# Exit
\q
```

You should see:
- Random UUIDs
- Random salts (8-character strings)
- Arrays of encrypted tokens (hexadecimal strings)
- NO plaintext names anywhere

## Troubleshooting

### Error: Connection refused to PostgreSQL
```bash
# Linux
sudo service postgresql start

# macOS
brew services start postgresql

# Windows
net start postgresql-x64-14
```

### Error: database "securematch" does not exist
```bash
psql -U postgres -c "CREATE DATABASE securematch;"
```

### Error: Required config 'SECRET_KEY' not found
Create `.env` file in the `securematch/` directory (same folder as pom.xml) with:
```
SECRET_KEY=a8f3k2p9x7m4n1q6r5t8w2y0z3b6c4d7
DATABASE_URL=jdbc:postgresql://localhost:5432/securematch
DB_USER=postgres
DB_PASSWORD=yourpassword
```

### Error: BUILD FAILURE
```bash
# Check Java version (must be 17+)
java -version

# Check Maven version (must be 3.8+)
mvn -version

# Clean and rebuild
mvn clean package
```

### Error: No matches found
Try lowering the threshold:
```bash
java -jar target/securematch-1.0.0.jar search --query "..." --threshold 0.2
```

Or verify records exist:
```bash
java -jar target/securematch-1.0.0.jar stats
```

## Performance Test

Test with 100 records:
```bash
# Add 100 records (you can create a script for this)
for i in {1..100}; do
  java -jar target/securematch-1.0.0.jar add --text "Test Name $i"
done

# Search should complete within 5 seconds
time java -jar target/securematch-1.0.0.jar search --query "Test Name 50"
```

## Clean Up

To reset the database:
```bash
psql -U postgres -d securematch -c "DELETE FROM secure_records;"
```

Or drop and recreate:
```bash
psql -U postgres -c "DROP DATABASE securematch;"
psql -U postgres -c "CREATE DATABASE securematch;"
java -jar target/securematch-1.0.0.jar init
```

## Success Criteria

✅ All commands execute without errors  
✅ Search finds exact matches with ~100% similarity  
✅ Search finds fuzzy matches with typos  
✅ Indian name transliterations work  
✅ Database contains NO plaintext data  
✅ Stats show correct record count  
✅ Performance is acceptable (< 5 seconds for 1000 records)

## Next Steps

- Add more test records
- Experiment with different threshold values
- Test with your own data
- Review the code in `src/main/java/com/securematch/`
- Read the design document in `.kiro/specs/secure-match/design.md`
