
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javax.net.ssl.SSLSocket;

public class Client implements Runnable{
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader keyboard;

// ===========================================================================    
    @Override 
    public void run(){
        try{
            
            // Input SERVER(IP:PORT)
            keyboard = new BufferedReader( new InputStreamReader (System.in));

            while(true){
                System.out.print("IP: ");
                String ip = keyboard.readLine();
                System.out.print("Port: ");
                int port = Integer.parseInt(keyboard.readLine());
                try{
                    client = new Socket(ip, port);
                    System.out.println("Connected!");
                    break;
                }
                catch(IOException e){
                    System.out.println("Cannot connect. Try again.");
                }
            }
            
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);
            
            InputHandler ih = new InputHandler();
            Thread t = new Thread(ih);
            t.start(); // inputhandler.run()
            
            String inMessage;
            while((inMessage = in.readLine()) != null){
                System.out.println(inMessage);
            }
        }
        catch(IOException e){
            System.out.println("Error: " + e.getMessage());
            shutdown();
        }   
    }
    
    public void shutdown(){
        try{
            
            if(in != null) in.close();
            if(out != null) out.close();
            if(client != null && client.isClosed() != true){
                client.close();
            }
        }catch(IOException e){
            System.out.println("Error: " + e.getMessage());
        }
    }

// ===========================================================================    
    // waiting for keyboard and throw to server
    class InputHandler implements Runnable{
        private String message;
        
        @Override
        public void run(){
            try {
                
                while(true){
                    
                    // Luồng chờ nhập từ phím
                    message = keyboard.readLine();
                    
                    if(message.startsWith("/quit")){
                        out.println(message);
                        keyboard.close();
                        break;
                    }
                    else{
                        out.println(message);
                    }
                    
                }
                
            } 
            catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
            finally{
                System.out.println("Client has been shutdown");
                shutdown();
            }
        }
    }
// ===========================================================================
    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
