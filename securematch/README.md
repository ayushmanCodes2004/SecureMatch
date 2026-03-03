# 🔐 SecureMatch — Fuzzy Searchable Encryption CLI

**Team: One Step At A Time**  
**Category: Data Privacy and Protection**

SecureMatch is a CLI-based searchable encryption system that enables organizations to store sensitive strings (like names) fully encrypted in a PostgreSQL database and search through them using fuzzy matching — without the database ever seeing the actual data.

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 14+

### Installation

1. **Create PostgreSQL database:**
```bash
psql -U postgres -c "CREATE DATABASE securematch;"
```

2. **Configure environment:**
Edit the `.env` file with your database credentials:
```
SECRET_KEY=a8f3k2p9x7m4n1q6r5t8w2y0z3b6c4d7e5f8g1h2i3j4k5
DATABASE_URL=jdbc:postgresql://localhost:5432/securematch
DB_USER=postgres
DB_PASSWORD=yourpassword
```

3. **Build the project:**
```bash
mvn clean package
```

4. **Initialize the database:**
```bash
java -jar target/securematch-1.0.0.jar init
```

## 📖 Usage

### Initialize Database
```bash
java -jar target/securematch-1.0.0.jar init
```

### Add Encrypted Records
```bash
java -jar target/securematch-1.0.0.jar add --text "John Smith"
java -jar target/securematch-1.0.0.jar add --text "Venkateshwar Rao"
java -jar target/securematch-1.0.0.jar add --text "Mohammed Ali"
```

### Search with Fuzzy Matching
```bash
# Search with default threshold (0.3)
java -jar target/securematch-1.0.0.jar search --query "Jon Smith"

# Search with custom threshold
java -jar target/securematch-1.0.0.jar search --query "Venkateswara Rao" --threshold 0.2
```

### View Statistics
```bash
java -jar target/securematch-1.0.0.jar stats
```

## 🔐 How It Works

### 1. Trigram Tokenization
Text is broken into overlapping 3-character chunks:
```
"John Smith" → ["joh", "ohn", "hn ", "n s", " sm", "smi", "mit", "ith"]
```

### 2. HMAC-SHA256 Encryption
Each trigram is encrypted with a secret key and per-record salt:
```
HMAC(secret_key, "joh" + salt) → "a3f9b2c4d5e6f7a8"
```

### 3. Secure Storage
Only encrypted tokens are stored in PostgreSQL:
```
Database stores: {UUID, salt, [encrypted_tokens]}
Plaintext NEVER touches the database ✅
```

### 4. Fuzzy Search
Search uses Jaccard similarity on encrypted tokens:
```
Query: "Jon Smith" (with typo)
Matches: "John Smith" with 71% similarity ✅
Server never sees either name ✅
```

## 🛡️ Security Features

- **HMAC-SHA256**: Cryptographically secure encryption
- **Per-Record Salt**: Defeats rainbow table attacks
- **Zero-Knowledge Database**: Database never sees plaintext
- **Kerckhoffs's Principle**: Security depends on key, not algorithm
- **GDPR/DPDP Compliant**: Encrypted-at-rest storage

## 🏗️ Architecture

```
Main.java → Config.java (loads .env)
         → SecureMatchCLI.java (Picocli root)
            ├── InitCommand.java
            ├── AddCommand.java
            ├── SearchCommand.java
            └── StatsCommand.java

SecureTokenizer.java (crypto engine)
├── Trigram extraction
├── HMAC-SHA256 encryption
├── Salt generation
└── Jaccard similarity

SecureDatabase.java (PostgreSQL layer)
├── Schema initialization
├── Record storage
├── Record retrieval
└── Statistics
```

## 📊 Database Schema

```sql
CREATE TABLE secure_records (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salt      TEXT NOT NULL,
    token_set TEXT[] NOT NULL
);

CREATE INDEX idx_token_set ON secure_records USING GIN(token_set);
```

## 🎯 Use Cases

- Healthcare: Store patient names encrypted
- Finance: Secure customer data storage
- Enterprise: GDPR/DPDP compliance
- Indian Names: Handles transliteration variants

## 🔧 Tech Stack

- **Language**: Java 17
- **Build Tool**: Maven
- **CLI Framework**: Picocli
- **Database**: PostgreSQL 14+
- **Crypto**: javax.crypto (built-in)

## 📝 Example Session

```bash
# Initialize
$ java -jar target/securematch-1.0.0.jar init
✅ Secure database initialized!

# Add records
$ java -jar target/securematch-1.0.0.jar add --text "Venkateshwar Rao"
✅ Stored successfully!
🆔 Record ID  : 550e8400-e29b-41d4-a716-446655440000
🔐 Server NEVER saw: 'Venkateshwar Rao'

# Search with typo
$ java -jar target/securematch-1.0.0.jar search --query "Venkateswara Rao"
✅ 1 match(es) found:
  550e8400-e29b-41d4-a716-446655440000  ████████████████ 79%
🔐 Server never saw: 'Venkateswara Rao'

# View stats
$ java -jar target/securematch-1.0.0.jar stats
📊 Total Records: 1
✅ Plaintext stored: ZERO
```

## 🚨 Important Notes

- **Never commit .env file** to version control
- **Rotate SECRET_KEY** periodically in production
- **Backup database** with encryption
- **Use strong passwords** for PostgreSQL

## 📄 License

Built for Posidex Hackathon — Secured String Matching using Searchable Encryption

---

*"Search encrypted data by comparing encrypted tokens — never decrypting anything."*
