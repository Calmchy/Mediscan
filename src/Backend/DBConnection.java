package Backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author chyril
 */
public class DBConnection {
    private static String user = "root";
    private static String pass = "";
    private static String url = "jdbc:mysql://localhost:3306/mediscan_pos";
    
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        
        return DriverManager.getConnection(url, user, pass);
    }
}
