import java.sql.*;
import java.util.*;

public class BatchBenchmark {
    static Connection connect() throws SQLException {
        return DriverManager.getConnection(
            System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/transport_db"),
            System.getenv().getOrDefault("DB_USER", "root"),
            System.getenv().getOrDefault("DB_PASSWORD", ""));
    }

    static List<String[]> makeRows(int n) {
        List<String[]> rows = new ArrayList<>();
        for (int i=1; i<=n; i++)
            rows.add(new String[]{"P"+i, "REG"+i, "Student"+i, String.valueOf((i%20)+1), "4500.0"});
        return rows;
    }

    static void clear(Connection c) throws SQLException {
        try (Statement s=c.createStatement()) { s.executeUpdate("DELETE FROM bus_pass"); }
    }

    static long preparedBatch(Connection c, List<String[]> rows) throws SQLException {
        clear(c);
        long start=System.nanoTime();
        PassLoader.loadAll(c, rows);
        return (System.nanoTime()-start)/1_000_000;
    }

    static long preparedOneByOne(Connection c, List<String[]> rows) throws SQLException {
        clear(c);
        String sql="INSERT INTO bus_pass(pass_id,reg_no,name,route_no,amount) VALUES(?,?,?,?,?)";
        long start=System.nanoTime();
        c.setAutoCommit(false);
        try (PreparedStatement p=c.prepareStatement(sql)) {
            for (String[] r: rows) {
                p.setString(1,r[0]); p.setString(2,r[1]); p.setString(3,r[2]);
                p.setInt(4,Integer.parseInt(r[3])); p.setDouble(5,Double.parseDouble(r[4]));
                p.executeUpdate();
            }
            c.commit();
        } catch(SQLException e) { c.rollback(); throw e; }
        finally { c.setAutoCommit(true); }
        return (System.nanoTime()-start)/1_000_000;
    }

    static long statementOneByOne(Connection c, List<String[]> rows) throws SQLException {
        clear(c);
        long start=System.nanoTime();
        c.setAutoCommit(false);
        try (Statement s=c.createStatement()) {
            for (String[] r: rows) {
                String sql="INSERT INTO bus_pass(pass_id,reg_no,name,route_no,amount) VALUES('"
                    +r[0]+"','"+r[1]+"','"+r[2]+"',"+r[3]+","+r[4]+")";
                s.executeUpdate(sql);
            }
            c.commit();
        } catch(SQLException e) { c.rollback(); throw e; }
        finally { c.setAutoCommit(true); }
        return (System.nanoTime()-start)/1_000_000;
    }

    public static void main(String[] args) throws Exception {
        try (Connection c=connect()) {
            for (int n : new int[]{10, 1000, 10000, 100000}) {
                List<String[]> rows=makeRows(n);
                System.out.println("Rows: "+n);
                System.out.println("Statement ms: "+statementOneByOne(c,rows));
                System.out.println("PreparedStatement ms: "+preparedOneByOne(c,rows));
                System.out.println("Prepared batch ms: "+preparedBatch(c,rows));
            }
        }
    }
}
