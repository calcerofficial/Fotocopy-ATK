import java.sql.*;

public class DBConnection {

    public Connection conn;
    public Statement stat;
    public ResultSet result;
    public PreparedStatement pstat;

    public DBConnection(){
        try{
            String url = "jdbc:sqlserver://kelompok-5.database.windows.net:1433;database=FotoCopyATK;user=hilmi;password=Kelompok5;trustServerCertificate=true;";
            conn = DriverManager.getConnection(url);
            stat = conn.createStatement();
        } catch (Exception e) {
            System.out.println("Eror saat connect database : "+e);
        }
    }

    public Connection getConnection() {
        return this.conn;
    }

    public static void main(String[] args) {
        DBConnection connect = new DBConnection();
        System.out.println("Connection berhasil");
    }
}