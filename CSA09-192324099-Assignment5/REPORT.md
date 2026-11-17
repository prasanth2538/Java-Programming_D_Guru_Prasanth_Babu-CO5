# Assignment 5 – CO5 Report
**Student:** D Guru Prasanth Babu  
**Register Number:** 192324099  
**Course:** CSA0902 – Programming in Java

# PART A – Persistence Design and Pseudocode

## A1. Design a file format

### A1.1 Record layouts
**Delimited text format:** one pass per line, fields in this order:
`passId,regNo,name,routeNo,validityDate,amountPaid`

Example:
`P1001,192311001,Arun,12,2027-03-31,4500.00`

Use UTF-8. A plain comma-separated format is not safe if a name contains a comma. A production CSV writer must quote fields and double embedded quotes, or the application can use a delimiter-escaping rule. The implementation in Task 1 uses a simple comma format and validates the field count; for unrestricted names, use a CSV library or reject/escape commas.

**Binary DataOutputStream format:** write fields in a fixed documented order:
- passId: `writeUTF` (length-prefixed modified UTF-8)
- regNo: `writeUTF`
- name: `writeUTF`
- routeNo: `writeInt`
- validityDate: `writeUTF` using ISO `yyyy-MM-dd`
- amountPaid: `writeDouble`

The binary record is variable-length because `writeUTF` stores a length prefix. A truly fixed-size record would require fixed-width fields and explicit padding.

### A1.2 Pseudocode for text operations
**ADD PASS**
1. Validate every field.
2. Open `BufferedWriter(new FileWriter(file, true))`.
3. Encode/escape each field and join with the delimiter.
4. Write the record and newline.
5. Close the writer using try-with-resources.
6. For stronger crash safety, write to a temporary file and atomically replace the original, or use a journal.

**FIND BY PASS ID**
1. If the file does not exist, report “no records yet” or return NOT_FOUND.
2. Open `BufferedReader(new FileReader(file))`.
3. While a line is available:
   - Split using the agreed format.
   - If the field count or field types are invalid, log the line number and continue.
   - If field 1 equals the requested pass ID, return the record.
4. Close the reader and return NOT_FOUND.

**LIST BY ROUTE**
1. Open a `BufferedReader`.
2. Read each line sequentially.
3. Validate the record; skip and report damaged lines.
4. Print records whose route number equals the requested route.
5. Close the reader.

`BufferedReader` and `BufferedWriter` reduce small read/write calls. `FileReader`/`FileWriter` are character streams; for explicit UTF-8 use `Files.newBufferedReader` and `Files.newBufferedWriter` with `StandardCharsets.UTF_8`. `FileInputStream`/`FileOutputStream` are byte streams and are suitable for binary data, not ordinary text parsing.

### A1.3 Damaged data
- Damaged line: validate field count, numeric route/amount and date; log line number and skip or quarantine it.
- Missing file: distinguish `NoSuchFileException`; treat it as an empty store only when creating a new store is intended.
- Incomplete write/power failure: a final line without newline or with invalid fields is rejected. For stronger protection, write a complete new file to a temporary path, flush/close it, then atomically rename it. Keep a backup or append-only journal for recovery.

**RandomAccessFile alternative:** use fixed-size records and seek to `i * recordSize` when the record index is known. Finding by ID still needs O(n) scanning unless a separate ID-to-offset index is maintained. Direct indexed access is O(1) after the offset is known. Fixed-width name/ID fields waste space when actual values are short; reject or truncate values that exceed field capacity. Text scan is O(n) time and O(1) extra memory; indexed fixed records use O(1) seek plus index lookup, with extra index storage.

## A2. Don't read what you don't need
### A2.1 Cost
For a 50 MiB file, byte-at-a-time code makes approximately 52,428,800 calls to `FileInputStream.read()` (50 × 1,048,576). Java method calls are not necessarily one OS call each because the operating system and runtime cache data, but the tiny read size causes high call/loop overhead.

### A2.2 Buffered redesign
Use `BufferedInputStream` or a byte array of 8 KiB/64 KiB. Read a block, scan bytes in memory and count `'\n'`. The program makes roughly fileSize/bufferSize bulk reads instead of one Java read call per byte. Complexity is O(n) time and O(B) memory, where B is buffer size.

### A2.3 When buffering helps less
If the data is already in memory, or the source returns data in large blocks and processing dominates I/O, another buffer may add little benefit.

**Files.readAllLines alternative:** it reads all lines into a list, with O(n) time and O(n) memory; for 50 MB, actual heap use can be much larger than 50 MB because strings and list objects have overhead. Buffered streaming uses O(B) extra memory and is better for large files or constrained memory. `readAllLines` is convenient for small files that comfortably fit in memory.

## A3. Discover the data-access design pattern
### A3.1 Common pattern
Repeated steps: obtain connection, prepare SQL, bind values, execute, map ResultSet rows to objects, handle errors and close resources. Differences: entity/table name, columns, keys, SQL statements and row-to-object mapping.

### A3.2 Generic DAO
```java
interface Dao<T, K> {
    void insert(T value) throws Exception;
    T findById(K id) throws Exception;
    List<T> findAll() throws Exception;
    void update(T value) throws Exception;
    void delete(K id) throws Exception;
}
```
`StudentDao implements Dao<Student, Integer>` and contains student-specific SQL and mapping. The service calls the interface rather than writing SQL.

### A3.3 Object-table mapping
`Student.registerNo` → `student.register_no` (PRIMARY KEY); `Student.name` → `student.name`; `Student.email` → `student.email`. DAO maps ResultSet columns into a Student object.

**SQL in service/GUI alternative:** SQL and mapping become duplicated across screens/services. A renamed column may require edits in many locations, increasing maintenance risk. DAO centralizes table access. Both designs have essentially the same database operation complexity (for example indexed lookup O(log n) or database-dependent index cost); DAO adds only a small constant method-call overhead and does not change asymptotic time/space.

## A4. What should be serialized?
Mark the live `Connection` field `transient`: a database connection is an external resource, not serializable application state, and cannot be safely restored from a byte stream. Mark `password` transient as a security precaution; do not persist plaintext credentials. The Hostel → Rooms → Residents object graph is traversed recursively; every reachable non-transient object must be serializable or serialization throws `NotSerializableException`.

When a new phone field is added, an older stream normally loads the new field with its default value (null for String) if the class has a compatible explicit `serialVersionUID`. If the computed UID changes and no matching UID exists, `InvalidClassException` occurs. Define and manage `serialVersionUID`, and add migration/defaulting logic.

**CSV/JSON alternative:** text is portable and easier to inspect, but requires explicit parsing/mapping. Both approaches save/load n residents in O(n) time and O(n) memory if all residents are loaded at once. Binary serialization is often smaller/faster for Java object graphs but is Java-specific and brittle across class changes. CSV/JSON is more interoperable and can tolerate added optional fields with defaults, but may be larger and require parsing. Use secure formats and never store plaintext passwords.

## A5. Design for correctness and safety
### A5.1 Transaction pseudocode
1. Get connection and set autoCommit(false).
2. Update hostel wallet: subtract 500 only if balance >= 500.
3. Check exactly one row was updated; otherwise throw an error.
4. Update canteen wallet: add 500.
5. Check exactly one row was updated.
6. Commit.
7. On any exception, rollback and report failure.
8. Restore the previous auto-commit setting.

The two updates are one atomic unit: both commit or neither does.

### A5.2 SQL injection
String concatenation lets an attacker alter SQL syntax. For example, a username input containing `' OR '1'='1' -- ` can make a condition true and comment out the remaining password check, depending on query construction. Use `PreparedStatement`:
```java
String sql = "SELECT 1 FROM users WHERE id=? AND pwd_hash=?";
try (PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setString(1, id);
    ps.setString(2, passwordHash);
    try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
    }
}
```
Store salted password hashes, not plaintext passwords.

**Auto-commit alternative:** with two independent auto-commit updates, a crash after the debit but before credit loses ₹500; reversing the order could create money. A transaction adds some logging/coordination overhead, but provides atomicity and correctness. Two statements may be slightly faster in a trivial benchmark, but are unsafe for a transfer.

# PART B – Implementation and Experimental Investigation

## B1. File store and buffering
`Task01/PassStore.java` completes TODO 1: reads line by line, skips lines with the wrong field count and returns the matching pass. A malformed P1002 line does not stop the search because the loop uses `continue` for invalid records.

Test cases:
| Test | Input | Expected |
|---|---|---|
| Existing ID | P1003 | Matching five fields |
| Missing ID | P9999 | null |
| Damaged line | P1002,DAMAGED LINE | skipped; search continues |

The implementation uses buffered character I/O and validates record shape. The simple CSV split assumes names do not contain commas; use a proper CSV parser or escaping for unrestricted names.

For q searches over n passes, repeated file scans cost O(q × n) time and O(1) extra memory. Loading once into a HashMap costs O(n) build time and O(n) memory; expected total for q searches is O(n + q), with expected O(1) lookup.

## B2. Adversarial inputs
`Task05/JdbcSafetyDemo.java` shows an unsafe concatenated query and a parameterized query. A classic test input is username `admin' -- ` with any password when the SQL is `... WHERE id='` + id + `' AND pwd='` + pwd + `'`; the comment may remove the password predicate. The PreparedStatement treats the input as data and rejects it unless it is a real matching credential. Run only against the local test database.

Damaged file tests should include:
- wrong field count: skip and report
- non-numeric amount: reject during numeric validation
- empty line: skip
A PreparedStatement is safer than quote escaping or regex validation because it separates SQL code from data. Validation is still useful for business rules but is not a substitute for parameter binding. Parameter binding has a small constant overhead; regex validation is O(length of input), but cannot reliably secure arbitrary SQL syntax.

## B3. Statement vs PreparedStatement vs batch
`Task06/BatchBenchmark.java` provides a local benchmark for 10, 1,000, 10,000 and 100,000 records (adjust sizes to match the assignment). It compares Statement, PreparedStatement and PreparedStatement batches. Results depend on hardware, MySQL configuration, network and indexes; record your actual output in `evidence/measurements.csv`.

Batching reduces round trips and is usually best for large imports. PreparedStatement is preferred for a single counter insert because it safely binds values and is simple. For a nightly 100,000-row import, use PreparedStatement batches inside a transaction (or evaluate MySQL bulk loading for controlled trusted files).

With auto-commit on, successful rows before a forced error may remain. With one transaction, rollback removes all rows from that transaction. `PassLoader` executes batches of 1,000 and commits once; any SQL/runtime failure triggers rollback and is re-thrown.

## B4. Final engineering decision
For 12 campuses and 100× the pass volume with simultaneous counters, use a central relational database (MySQL/InnoDB) rather than independent text or serialized files. It supports concurrent transactions, indexes for pass-ID and route searches, constraints, backups and centralized access control. Text files are useful for interchange/import and audit exports; serialization is not a suitable shared multi-campus production store.

Migration process: read each file row, validate field count and types, collect invalid rows in a failure report, add valid rows to PreparedStatement batches, commit in a transaction, and rollback on database failure. `DatabaseMetaData` reports database product, driver version and table names; this helps diagnose driver/server differences during deployment.

If each campus used its own serialized file, a search across all campuses would require querying 12 files (O(total passes) without indexes), and each campus would need locking/coordination for concurrent counters. Storage is approximately the sum of all files, but duplicate IDs and cross-campus consistency become difficult. A central database with transactions and indexes is the safer design.

## Evidence to collect
1. Run all Java programs on your own computer.
2. Capture screenshots showing commands and complete outputs.
3. Run line-count benchmarks for 1 MB, 10 MB and 50 MB; run pass lookup benchmarks for 1,000–100,000 rows.
4. Run JDBC benchmarks for 1,000, 10,000 and 100,000 rows.
5. Fill `evidence/measurements.csv` with your own measured values. Never claim example timings are measured results.
