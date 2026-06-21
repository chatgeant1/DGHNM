
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DAO {
    public static class UserDAO {

    private DBAccess db;

    public UserDAO(DBAccess db) {
        this.db = db;
    }

    public boolean studentExists(String studentId) {
        try {
            String sql = "SELECT * FROM users WHERE student_id='" + studentId + "'";
            ResultSet rs = db.Query(sql);

            boolean exists = rs.next();

            db.closeQuery(rs);

            return exists;

        } catch(Exception e) {return false;}
    }

    public String getNameByStudentId(String studentId) {
        try {
            ResultSet rs = db.Query(
                "SELECT full_name FROM users WHERE student_id='" +
                studentId + "'"
            );

            String name = null;

            if(rs.next()) {
                name = rs.getString("full_name");
            }

            db.closeQuery(rs);

            return name;

        } catch(Exception e) {return null;}
    }

    public void insertUser(String name, String studentId) {
        db.Update(
            "INSERT INTO users(full_name, student_id) VALUES ('"
            + name + "','" + studentId + "')"
        );
    }
}

    public static class MessageDAO {
        private DBAccess db;

        public MessageDAO(DBAccess db) {
            this.db = db;
        }

        // Lưu tin nhắn mới
        public boolean saveMessage(String studentId, String groupCode, String content) {
            String sql =
                "INSERT INTO messages " +
                "(student_id, group_code, content) " +
                "VALUES (?, ?, ?)";

            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {

                ps.setString(1, studentId);
                ps.setString(2, groupCode);
                ps.setString(3, content);

                return ps.executeUpdate() > 0;

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        

        // Lấy toàn bộ lịch sử của nhóm (cũ -> mới)
        public ResultSet loadHistory(String groupCode) {
            String sql =
                "SELECT * " +
                "FROM messages " +
                "WHERE group_code='" + groupCode + "' " +
                "ORDER BY id ASC";
            return db.Query(sql);
        }

        // Lấy X tin nhắn gần nhất (X dòng (mới -> cũ) ASC => cũ => mới)
        public ResultSet loadHistory(String groupCode, int limit) {
            String sql =
                "SELECT u.full_name, t.content, t.created_at " +
                "FROM (" +
                "    SELECT * " +
                "    FROM messages " +
                "    WHERE group_code='" + groupCode + "' " +
                "    ORDER BY id DESC " +
                "    LIMIT " + limit +
                ") t " +
                "JOIN users u ON t.student_id = u.student_id " +
                "ORDER BY t.id ASC";
            return db.Query(sql);            
                  
        }
    }
    
}
