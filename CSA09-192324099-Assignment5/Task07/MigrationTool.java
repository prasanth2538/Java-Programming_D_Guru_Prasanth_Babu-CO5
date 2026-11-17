import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class MigrationTool {
    static Connection connect() throws SQLException {
        return DriverManager.getConnection(
            System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/transport_db"),
            System.getenv().getOrDefault("DB_USER", "root"),
            System.getenv().getOrDefault("DB_PASSWORD", ""));
    }

    public static void main(String[] args) throws Exception {
        Path input = Paths.get(args.length > 0 ? args[0] : "passes.csv");
        List<String[]> valid = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        int lineNo=0;
        try (BufferedReader r=Files.newBufferedReader(input)) {
            String line;
            while ((line=r.readLine()) != null) {
                lineNo++;
                String[] f=line.split(",",-1);
                try {
                    if (f.length != 5) throw new IllegalArgumentException("expected 5 fields");
                    Integer.parseInt(f[3]);
                    Double.parseDouble(f[4]);
                    valid.add(f);
                } catch (RuntimeException ex) {
                    failures.add("Line "+lineNo+": "+line+" ("+ex.getMessage()+")");
                }
            }
        }
        try (Connection c=connect()) {
            int inserted=PassLoader.loadAll(c, valid);
            System.out.println("Inserted valid records: "+inserted);
            DatabaseMetaData md=c.getMetaData();
            System.out.println("Database: "+md.getDatabaseProductName());
            System.out.println("Driver: "+md.getDriverName()+" "+md.getDriverVersion());
            try (ResultSet tables=md.getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) {
                System.out.println("Tables:");
                while(tables.next()) System.out.println(" - "+tables.getString("TABLE_NAME"));
            }
        }
        Files.write(Paths.get("migration-errors.txt"), failures);
        System.out.println("Rejected records: "+failures.size()+" (see migration-errors.txt)");
    }
}
