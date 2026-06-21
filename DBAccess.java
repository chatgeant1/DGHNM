
import java.sql.*;

public class DBAccess {

    private Connection con;
    

    public DBAccess() {
        try {
            
        // connect with MySQL
        MyConnection myCon = new MyConnection();
        con = myCon.getConnection();
        System.out.println("Successfully connected to DB!");
        
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    // GET connection
    public Connection getConnection() {
        return con;
    }
    
    // CRUD (mở rộng PreparedStatement sau):
    public synchronized int Update(String str) {
        try (Statement stmt = con.createStatement()) {
            
            // cơ chế Try-with-resources: Tự động đóng stmt khi chạy xong hàm này
            System.out.println("Execute Update: " + str);
            return stmt.executeUpdate(str);
            
        } catch (SQLException e) {
            System.err.println("Lỗi SQL Update: " + e.getMessage());
            return -1;
        }
    }
    
    // SELECT
    public synchronized ResultSet Query(String str){
        try {
            
            // Với Query, KHÔNG tự đóng Statement, nếu ko ResultSet sẽ bị chết theo.
            // Nếu đóng stmt ở đây, ResultSet trả về sẽ trống rỗng (lỗi)!
            Statement stmt = con.createStatement();
            return stmt.executeQuery(str);

        } catch (SQLException e) {
            System.err.println("Lỗi SQL Query: " + e.getMessage());
            return null;
        }
    }
    
    // Đóng resultset khi sử dụng xong (đối tượng giữ kết quả truy vấn và con trỏ đọc dữ liệu)
    public void closeQuery(ResultSet rs){
        try {
            if(rs != null){
                Statement stmt = rs.getStatement();
                rs.close();
                if(stmt != null) stmt.close();
            }
        }
        catch(SQLException e){e.printStackTrace();}
    }
    
     // Dùng để đóng kết nối tổng khi tắt Server
    public void close() {
        try {   
            if (con != null && !con.isClosed()) {
                con.close();
                System.out.println("Đã đóng kết nối Database an toàn.");
            } 
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }    
}
