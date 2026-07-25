package Database;

import java.sql.*;

public class DBConnection {

    private static Connection conn;
    private Statement stat;
    private ResultSet result;
    private PreparedStatement pstat;

    public DBConnection() {
        try {
            String url = "jdbc:sqlserver://localhost:1433;databaseName=FotoCopyATK;encrypt=false;trustServerCertificate=true;";
            String user = "sa";
            String password = "PasswordAnda123";

            conn = DriverManager.getConnection(url, user, password);
            stat = conn.createStatement();

            System.out.println("Connection berhasil");
        } catch (Exception e) {
            System.out.println("Error saat connect database: " + e.getMessage());
        }
    }

    // Method static untuk mendapatkan koneksi
    public static Connection getConnection() {
        // Jika koneksi null atau closed, buat koneksi baru
        try {
            if (conn == null || conn.isClosed()) {
                new DBConnection(); // Panggil constructor untuk inisialisasi
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public Statement getStatement() {
        return stat;
    }

    public PreparedStatement getPreparedStatement(String sql) throws SQLException {
        if (conn == null || conn.isClosed()) {
            new DBConnection();
        }
        return conn.prepareStatement(sql);
    }

    public void closeConnection() {
        try {
            if (result != null) result.close();
            if (stat != null) stat.close();
            if (pstat != null) pstat.close();
            if (conn != null) conn.close();
            System.out.println("Connection closed");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DBConnection connect = new DBConnection();
        System.out.println("Connection berhasil");
    }
}