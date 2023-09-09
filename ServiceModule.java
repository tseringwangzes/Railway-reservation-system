import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.sql.*;

class QueryRunner implements Runnable {
    // Declare socket for client access
    protected Socket socketConnection;

    public QueryRunner(Socket clientSocket) {
        this.socketConnection = clientSocket;
    }

    public  void insert_train_instance(int t_id, String b_date, int ACcount, int SLcount) {
        Connection c = null;
        CallableStatement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5433/demo",
                            "postgres", "HK-tr-02");
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");
            String sql = "CALL add_new_train_instance(?,?,?,?)";
            stmt = c.prepareCall(sql);
            stmt.setInt(1, t_id);
            stmt.setString(2, b_date);
            stmt.setInt(3, ACcount);
            stmt.setInt(4, SLcount);
            stmt.executeUpdate();
            stmt.close();
            c.commit();
            c.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("insert successfully");

    }

    String responseQuery="";
    Connection c = null;
    CallableStatement stmt = null;
    public void booking(int num,int tid,String doj,String name,String coach) {
            try{
                Class.forName("org.postgresql.Driver");
                c = DriverManager
                        .getConnection("jdbc:postgresql://localhost:5432/db",
                                "postgres", "990699");
                c.setAutoCommit(false);
                System.out.println("Opened database successfully");
            String sql = "CALL check_seat_availability(?,?,?,?,?,?)";
            stmt = c.prepareCall(sql);
            stmt.setInt(1, num);
            stmt.setString(2, coach);
            stmt.setInt(3, tid);
            stmt.setString(4, doj);
            stmt.setString(5,name);
            stmt.setNull(6,Types.NULL);
            stmt.registerOutParameter(6,Types.VARCHAR);
            stmt.executeUpdate();
            responseQuery = stmt.getString(6);
            stmt.close();
            c.commit();
            c.close();
            }catch(Exception e){
            e.printStackTrace();
            try{
                if(c!=null) {c.rollback();responseQuery="Transaction rolled back";}
            }catch(SQLException e2){
                    e2.printStackTrace();
            }
            finally{
                try{
                    if(stmt!=null) stmt.close();
                }catch(SQLException e1){
                    e1.printStackTrace();
                }
            }
            }
        //System.out.println("insert successfully");

    }

    public void run() {
        try {
            // Reading data from client
            InputStreamReader inputStream = new InputStreamReader(socketConnection
                    .getInputStream());
            BufferedReader bufferedInput = new BufferedReader(inputStream);
            OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection
                    .getOutputStream());
            BufferedWriter bufferedOutput = new BufferedWriter(outputStream);
            PrintWriter printWriter = new PrintWriter(bufferedOutput, true);

            String clientCommand = "";
            String queryInput = "";
            int num = 0;
            int tnum = 0;
            String names="";
            String d_o_j = "";
            String coch = "";
            int id = 0;
            String bdate = "0";
            int ac_c = 0;
            int sl_c = 0;

            while (true) {
                // Read client query
                clientCommand = bufferedInput.readLine();
                // System.out.println("Recieved data <" + clientCommand + "> from client : "
                // + socketConnection.getRemoteSocketAddress().toString());

                // Tokenize here
                StringTokenizer tokenizer = new StringTokenizer(clientCommand);
                int present = tokenizer.countTokens();
                if(present==1){
                    
            String returnMsg = "Connection Terminated - client : "
                    + socketConnection.getRemoteSocketAddress().toString();
            System.out.println(returnMsg);
            inputStream.close();
            bufferedInput.close();
            outputStream.close();
            bufferedOutput.close();
            printWriter.close();
            socketConnection.close();
            return;
                }
                else if(present == 4){
                    int j = 0;
                    while (j < 4) {
                        queryInput = tokenizer.nextToken();
                        if (j == 0)
                            id = Integer.parseInt(queryInput);
                        else if (j == 1)
                            bdate = queryInput;
                        else if (j == 2)
                            ac_c = Integer.parseInt(queryInput);
                        else if (j == 3)
                            sl_c = Integer.parseInt(queryInput);
                        j++;
                    }
                    insert_train_instance(id, bdate, ac_c, sl_c);
    
                    responseQuery = "******* result ******";
    
                    printWriter.println(responseQuery);
    
                }
            
                
                else{
                    int j = 0;
                while (j < 5) {
                    queryInput = tokenizer.nextToken();

                    if (j == 0){
                        num=Integer.parseInt(queryInput);
                    }
                    else if (j == 1){
                        int cnt=num;
                    while(cnt>1){
                        queryInput=queryInput.substring(0, queryInput.length() - 1);
                        names = names+queryInput+" ";
                        --cnt;
                        queryInput = tokenizer.nextToken();
                    }
                    names = names+queryInput;
                    System.out.println(names+'\n');
                    }
                    else if (j == 2){
                        tnum=Integer.parseInt(queryInput);
                    }
                    else if (j == 3){
                        d_o_j=queryInput;
                    }
                    else if(j==4){
                        coch = queryInput;
                    }
                    j++;
                }
                booking(num, tnum, d_o_j, names,coch);

                // ----------------------------------------------------------------

                // Sending data back to the client

                printWriter.println(responseQuery);
                // System.out.println("\nSent results to client - "
                // + socketConnection.getRemoteSocketAddress().toString() );
                names="";

                }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
}

/**
 * Main Class to controll the program flow
 */
public class ServiceModule {
    static int serverPort = 7005;
    static int numServerCores = 2;

    // ------------ Main----------------------
    public static void main(String[] args) throws IOException {
        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);

        // Creating a server socket to listen for clients
        ServerSocket serverSocket = new ServerSocket(serverPort); // need to close the port
        Socket socketConnection = null;

        // Always-ON server
        while (true) {
            System.out.println("Listening port : " + serverPort
                    + "\nWaiting for clients...");
            socketConnection = serverSocket.accept(); // Accept a connection from a client
            System.out.println("Accepted client :"
                    + socketConnection.getRemoteSocketAddress().toString()
                    + "\n");
            // Create a runnable task
            Runnable runnableTask = new QueryRunner(socketConnection);

            // Submit task for execution
            executorService.submit(runnableTask);
        }
    }
}

