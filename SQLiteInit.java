import java.sql.Connection;
import java.sql.DriverManager;

public class SQLiteInit {
    public static void main(String[] args) {
        try {
            String url = "jdbc:sqlite:searchengine.db";
            Connection conn = DriverManager.getConnection(url);
            if (conn != null) {
                System.out.println("Database created or opened successfully.");
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
