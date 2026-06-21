

import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;
import javax.swing.*;

public class MyConnection {

    public Connection getConnection() {
        try {
            
            Properties prop = new Properties();

            try (FileInputStream fis = new FileInputStream("config.properties")) {
                prop.load(fis);
            }

            String URL = prop.getProperty("db.url");
            String USER = prop.getProperty("db.user");
            String PASSWORD = prop.getProperty("db.password");
            
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            // jdbc:mysql://[host_name_or_IP_address]:[port_number]/[database_name]?[property_1]=[value_1]&[property_2]=[value_2]
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            return con;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.toString(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

}
