# Programming in Java – Assignment 5 (CO5)
## Data Persistence Engineering – Files and Databases

**Name:** D Guru Prasanth Babu  
**Register Number:** 192324099  
**Course:** CSA0902 – Programming in Java  
**Assignment:** 5 – CO5

## Project structure
- `Task01` – Bus-pass file format design, pseudocode and file-store implementation
- `Task02` – Buffered and unbuffered line counting
- `Task03` – Generic DAO design
- `Task04` – Object serialization and transient fields
- `Task05` – JDBC transactions and SQL injection demonstration
- `Task06` – Statement, PreparedStatement and batch insert comparison
- `Task07` – File-to-database migration and DatabaseMetaData
- `REPORT.md` – Part A answers, experiment plan, result interpretation and production decision
- `sql/schema.sql` – MySQL database and table setup

## Requirements
- JDK 11 or later
- MySQL 8.x for Tasks 5–7 (optional for file-only Tasks 1–4)
- MySQL Connector/J 8.x JAR for JDBC tasks

## Compile and run (Windows PowerShell)
Open PowerShell in this project folder. Example for Task 1:

```powershell
javac -d out Task01\PassStore.java
java -cp out PassStore
```

Task 2:
```powershell
javac -d out Task02\LineCounter.java
java -cp out LineCounter
```

Task 3:
```powershell
javac -d out Task03\DaoDemo.java
java -cp out DaoDemo
```

Task 4:
```powershell
javac -d out Task04\SerializationDemo.java
java -cp out SerializationDemo
```

Task 5 (set database environment variables first):
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/transport_db"
$env:DB_USER="root"
$env:DB_PASSWORD="your_password"
javac -cp ".;mysql-connector-j-8.x.x.jar" -d out Task05\JdbcSafetyDemo.java
java -cp ".;out;mysql-connector-j-8.x.x.jar" JdbcSafetyDemo
```

Task 6:
```powershell
javac -cp ".;mysql-connector-j-8.x.x.jar" -d out Task06\PassLoader.java Task06\BatchBenchmark.java
java -cp ".;out;mysql-connector-j-8.x.x.jar" BatchBenchmark
```

Task 7:
```powershell
javac -cp ".;mysql-connector-j-8.x.x.jar" -d out Task07\MigrationTool.java
java -cp ".;out;mysql-connector-j-8.x.x.jar" MigrationTool passes.csv
```

## MySQL setup
1. Start MySQL Server.
2. Execute `sql/schema.sql` in MySQL Workbench or the MySQL command line.
3. Add MySQL Connector/J to the classpath.
4. Set `DB_URL`, `DB_USER`, and `DB_PASSWORD`. Do not hard-code real passwords in source files.

## Results
The programs are designed to demonstrate file scanning, buffering, serialization, safe parameterized SQL, transactions, batch inserts, migration validation and metadata inspection. Record actual timings from your own computer in `evidence/measurements.csv`; do not copy sample timings as measured results.

## Screenshots
Create screenshots of your own program runs and place them in the matching `TaskXX/screenshots/` folders. The folders contain a short README explaining the required evidence.

## GitHub
Create a repository named `CSA09-192324099-Assignment5`. Commit each task separately as you complete and test it.
