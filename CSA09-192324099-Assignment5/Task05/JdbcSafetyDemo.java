import java.sql.*;

public class JdbcSafetyDemo {
    static Connection connect() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/transport_db");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, pass);
    }

    public static void main(String[] args) throws Exception {
        try (Connection con = connect()) {
            String unsafeId = "admin' -- ";
            String unsafeSql = "SELECT * FROM demo_users WHERE user_id='" + unsafeId
                    + "' AND pwd='wrong-password'";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(unsafeSql)) {
                System.out.println("Unsafe query accepted row: " + rs.next());
            }

            String safeSql = "SELECT * FROM demo_users WHERE user_id=? AND pwd=?";
            try (PreparedStatement ps = con.prepareStatement(safeSql)) {
                ps.setString(1, unsafeId);
                ps.setString(2, "wrong-password");
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("PreparedStatement accepted row: " + rs.next());
                }
            }

            con.setAutoCommit(false);
            try {
                try (PreparedStatement debit = con.prepareStatement(
                        "UPDATE wallets SET balance=balance-500 WHERE wallet_id=? AND balance>=500");
                     PreparedStatement credit = con.prepareStatement(
                        "UPDATE wallets SET balance=balance+500 WHERE wallet_id=?")) {
                    debit.setInt(1, 1);
                    if (debit.executeUpdate() != 1) throw new SQLException("Insufficient balance");
                    credit.setInt(1, 2);
                    if (credit.executeUpdate() != 1) throw new SQLException("Destination wallet missing");
                }
                con.commit();
                System.out.println("Transfer committed.");
            } catch (SQLException ex) {
                con.rollback();
                System.out.println("Transfer rolled back: " + ex.getMessage());
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
