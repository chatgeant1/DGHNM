
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Server implements Runnable{
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private ExecutorService pool;
    
    private volatile boolean isShutdown = false; // Dùng làm cờ hiệu, dùng chung khắp các thread 

    private DBAccess dba;
    
    public Server(){
        connections = new ArrayList<>();
        isShutdown = false;
        dba = new DBAccess();
    }
    
    @Override
    public void run(){
        try {
            
            server = new ServerSocket(9999);
            System.out.println("Server running at 9999");
            pool = Executors.newCachedThreadPool();
            
            while(true){
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                pool.execute(handler);
            }
            
        } catch (IOException ex) {
            // if exception cause by shutting down, ignore
            if(!isShutdown){
                System.err.println(ex.getMessage());
                serverShutdown();
            }
        }
        
    }

    public void broadcast(String message, ConnectionHandler sender){
        for(ConnectionHandler ch : connections){
            if(ch != null && ch != sender) ch.sendMessage(message);
        }
    }
    
    private void addConnection(ConnectionHandler ch){
        this.connections.add(ch);
    }
    
    // Để Server không giữ lại các "kết nối rác" đã đóng, khi một Client shutdown
    private void removeConnection(ConnectionHandler ch){
        this.connections.remove(ch);
    }
    
    public void serverShutdown(){
        if (isShutdown) return;
        isShutdown = true;
        System.out.println("[System] Executing server shutdown...");

        try{
            if(pool != null) pool.shutdown();
            if(server != null && server.isClosed() == false){server.close();}
            for(ConnectionHandler ch : connections){
                ch.clientShutdown();
            }
        }
        catch(IOException e){
            System.out.println("Error: " + e.getMessage());
        }
        
    }
// ===========================================================================
// ===========================================================================
// ===========================================================================
    class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;
        private String msv;
        private String groupCode;
        private boolean login;
        private DAO.UserDAO udao;
        private DAO.MessageDAO mdao;
        
        public ConnectionHandler(Socket client){
            this.client = client;
            login = false;
            udao = new DAO.UserDAO(dba);
            mdao = new DAO.MessageDAO(dba);
        }
        
        @Override
        public void run(){
            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);
                
                while(!login){
                    out.println("Please enter your fullname: ");
                    name = in.readLine();

                    out.println("Please enter your student-id: ");
                    msv = in.readLine();

                    // name validation: blank, empty, space...

                    // Login/Insert DB
                    if (udao.studentExists(msv)){
                        if(name.equals(udao.getNameByStudentId(msv))){
                            login = true;
                        }
                        else{
                            out.println("Wrong name, please retry");
                        }
                    }
                    else{
                        udao.insertUser(name, msv);
                        login = true;
                    }
                }
                // Send client group code
                out.println("Welcome " + name);
                groupCode = "D22MMT01";
                out.println("GROUP_CODE:" + groupCode);
                
                // Print recent history
               ResultSet rs = mdao.loadHistory(groupCode, 20);
                if (rs != null) {
                    
                    boolean hasHistory = false;
                    
                    // Nếu ko có dòng nào: rs.next false
                    while (rs.next()) {
                        
                        if(!hasHistory){
                            out.println("===Last History===");
                            hasHistory = true;
                        }
                        
                        String studentId = rs.getString("full_name");
                        String content = rs.getString("content");
                        String time = rs.getString("created_at");
                        out.println(studentId + ": " + content + " [" + time + "]" );                        
                    }
                    if (hasHistory) {out.println("==================");}
                    
                    dba.closeQuery(rs);
                }
                
                // Add logged in user to ConnectionList
                addConnection(this);
                System.out.println(name + " connected!");
                
                // Broadcast to everyone that u joined:
                broadcast(name + " joined!", this);
                
                // Loop chat:
                String message;
                while((message = in.readLine()) != null){
// //TODO: /nick newnickname                    
//                    if(message.startsWith("/nick ")){
//                        String[] messageSplit = message.split(" ", 2);
//                        if(messageSplit.length == 2){
//                            // broadcast
//                            broadcast(nickname + " renamed themselves to " + messageSplit[1], this);
//                            // server
//                            System.out.println(nickname + " renamed themselves to " + messageSplit[1]);
//                            // client
//                            nickname = messageSplit[1];
//                            out.println("Successfully changed nickname to " + nickname);
//                        }
//                        else{
//                            out.println("No nickname provided!");
//                        }                                
//                    }
//                    
//                    else 
                    if (message.startsWith("/quit")){
                        broadcast(name + " left the chat!", this);
                        break;
                    }
                    
                    else{
                        // chat normally
                        if(mdao.saveMessage(msv, groupCode, message))
                            broadcast(name + ": " + message, this);
                    }
                }                
            } 
            catch (IOException e) {
                // Nếu có lỗi đường truyền (Client mất mạng đột ngột), chỉ in log 
                System.out.println(e.getMessage());

            } catch (SQLException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            finally {
                // Dù /quit hay bị lỗi IOException
                clientShutdown();
            }
        }
    
        public void sendMessage(String message){
            out.println(message);
        }
        public void clientShutdown(){
            try{
                removeConnection(this);
                // Tránh lỗi sập chương trình (NullPointerException) (->server.run<-)
                // khi các biến này chưa kịp khởi tạo mà hàm đóng kết nối đã bị gọi.
                if (in != null) in.close();
                if (out != null) out.close();
                if (client != null && !client.isClosed()) {
                    client.close();
                }
                
                System.out.println(name + " disconnected!");
                
            }catch(IOException e){
                System.err.println("Error: " + e.getMessage());
            }
        }

    } 
// =========================================================================== 
// ===========================================================================     
    public static void main(String[] args) {
        Server server = new Server();
        
        // Đăng ký 1 luồng khi chương trình bị tắt (bởi Ctrl+C hoặc nút Stop)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[System] Detecting shutdown signal! Cleaning up...");
            server.serverShutdown(); 
        }));
        
        server.run();
    }
}
