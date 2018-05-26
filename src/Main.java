import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//Server controller
/*
public class Main {
    public static void main(String[] args)throws SQLException {
        try(ServerSocket serverSocket = new ServerSocket(40450)){
            System.out.println("Server running.");
            while(true){
                //Following examples both do the same thing (accepting a socket and starting a connection in new thread
//                Socket socket = serverSocket.accept();
//                Threader threader = new Threader(socket);
//                threader.start();
               // System.out.println("ewfgerg");
                new DataServer(serverSocket.accept()).start();

            }
        }
        catch (IOException e){
            System.out.println("Server exception: "+ e.getMessage());
        }
    }
}
*/
public class Main {
    public static void main(String[] args) throws IOException {
        try {
            int portNumber = 40450;
            ServerSocket serverSocket = new ServerSocket(40450);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                DataServer dataserver = new DataServer(clientSocket);
                dataserver.start();
            }
        }
        catch(Exception e){
            System.out.println("Exception: "+e.getMessage());
            }
    }
}

/*
public class Main {
    private static String username = "student";
    private static String password = "student";
    private static String databaseName = "cruise";
    private static String databasePath = "jdbc:mysql://localhost:3306/"+databaseName+"?autoReconnect=true&useSSL=false";

    public static void main(String[] args) throws SQLException {
        java.sql.Connection connection = null;
        Statement statement = null;
        ResultSet results = null;


        try {
            Class.forName("com.mysql.jdbc.Driver");


            connection = DriverManager.getConnection(databasePath, username, password);
            System.out.println("Database connection successful!\n");

            //SQL statement
            statement = connection.createStatement();

            //Execute SQL query
            results = statement.executeQuery("select * from customer");

            //Process the result set
            while (results.next()) {
                System.out.println(results.getString("cabinNo") + ", " + results.getString("email"));
            }
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
        finally {
            if (results != null) {
                results.close();
            }

            if (statement != null) {
                statement.close();
            }

            if (connection != null) {
                connection.close();
            }
        }
    }
}
*/
