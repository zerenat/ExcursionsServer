import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;

//Database server
public class DataServer extends Thread {
    java.sql.Connection connection = null;
    Statement statement = null;
    ResultSet results = null;
    private String cabinNo;
    private Socket socket;
    private BufferedReader readInput;
    private PrintWriter writeOutput;
    private static ArrayList <String> usersOnline = new ArrayList<>();
    private static ArrayList <ArrayList> cruises = new ArrayList<>();
    private static ArrayList <String> cruise10 = new ArrayList<>();
    private static ArrayList <String> cruise20 = new ArrayList<>();

    public DataServer(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run() {
        boolean b = true;
        while (b) {
            try {

                //Declare new reader and writer to communicate with the client
                readInput = new BufferedReader((new InputStreamReader(socket.getInputStream())));
                writeOutput = new PrintWriter(socket.getOutputStream(), true);
                String storeInput = readInput.readLine();
                if(storeInput != null) {
                    connectToDb(storeInput);
                }
                socket.getInputStream().read();
                //b = false;
            }
            catch (IOException e) {
                System.out.println("IO exception: "+e.getMessage());
            }
            catch (NullPointerException e){
                System.out.println("nullpointer exception: "+e.getMessage());
            }
            catch (SQLException e){
                System.out.println("SQL Exception: "+e.getMessage());
            }
        }
    }

    //Method that takes the readInput string and sorts it into a String array in String chunks
    public String [] processInput(String input){
        String [] processedInput = input.split(",");
        return processedInput;
    }
    //Method that checks if a user account is logged in
    public boolean checkUsersOnline(String input){
        for(int i = 0; i < usersOnline.size(); i++){
            //System.out.println(usersOnline.get(i));
            if(usersOnline.get(i).equals(input)){//readInput.equals(usersOnline.get(i))
                System.out.println("input is: "+input);
                return true;
            }
        }
        return false;
    }
    public String [] processDbInfo(String input) {
        String[] storeInfo;
        storeInfo = input.split("|");
        for (int x = 0; x < storeInfo.length; x++)
            System.out.println(storeInfo[x]);




        return storeInfo;
    }

    public Connection makeConnection()throws SQLException {
        String username = "Client";
        String password = "ClientAccess";
        String databaseName = "Cruise";
        String databasePath = "jdbc:mysql://localhost:3306/"+databaseName+"?autoReconnect=true&useSSL=false";
        //String schoolDataBase = studentnet.cst.beds.ac.uk; //school DB
        //localhost:3306 //local DB

        //Try to make a connection to DB
        try{
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(databasePath, username, password);
            statement = connection.createStatement();
        }
        catch (Exception e){
            System.out.println(e);
        }
        return connection;
    }

    //Connection method that connects to a DB and performs tasks on behalf of a client
    public void connectToDb(String input) throws SQLException{
        //First line takes a String and cuts it into chunks using processInput method
        //The first chunk in every message is a key that is later used by a switch statement
        //The key is essential for the program to know which action to perform

        String [] storeInput = processInput(input);
        try {
            makeConnection();
            connection.createStatement();

            //Creating a switch statement that processes the incoming message from the client
            //Using the value of the first element of the Array, the switch statement takes appropriate action
            switch (storeInput[0]) {
                case "1":
                    //Register user
                    //statement.executeUpdate("INSERT INTO customer (cabinNo, email, userpw) VALUE ("+storeInput[1]+",'"+storeInput[2]+"','"+storeInput[3]+"');");
                    boolean exists = false;
                    results = statement.executeQuery("SELECT * FROM customers;");
                    while(results.next()) {
                        if (storeInput[1].equals(results.getString("cabinNo"))) {
                            System.out.println("Cabin number has already been used");
                            exists = true;
                        }
                    }
                    if(!exists) {
                        PreparedStatement prepStatementRegister = connection.prepareStatement
                                ("INSERT INTO customers (cabinNo, email, userpw, cruiseId) VALUES (?, ?, ?, ?)");
                        prepStatementRegister.setString(1, storeInput[1]);
                        prepStatementRegister.setString(2, storeInput[2]);
                        prepStatementRegister.setString(3, storeInput[3]);
                        prepStatementRegister.setString(4, storeInput[4]);
                        prepStatementRegister.executeUpdate();
                        System.out.println("Cabin registered successfully.");
                    }
                    break;
                case "2":
                    //Log in
                    //PreparedStatement prepStatementLogIn = connection.prepareStatement("SELECT * FROM customers;");
                    results = statement.executeQuery("SELECT * FROM customers;");
                    while(results.next()){
                        if(storeInput[1].equals(results.getString("cabinNo"))){
                            if(storeInput[2].equals(results.getString("userpw")) && checkUsersOnline(storeInput[1])==false){
                                //System.out.println("val: "+storeInput[1]);
                                usersOnline.add(storeInput[1]);
                                for(int x= 0; x < usersOnline.size();x++){
                                    System.out.println(usersOnline.get(x));
                                }
                                System.out.println("logged in");
                            }
                            else{
                                System.out.println("user already online");
                            }
                        }
                    }
                    break;
                case "3":
                    //Log out
                    cabinNo = storeInput[1];
                    usersOnline.remove(cabinNo);
                    System.out.println("User logged out");
                    break;
                case "4":
                    //View available excursions
                    String cruiseId;
                    results = statement.executeQuery("SELECT * FROM customers WHERE cabinNo = "+storeInput[1]);
                    while(results.next()) {
                        cruiseId = results.getString(5);
                        results = statement.executeQuery("SELECT * FROM excursions WHERE cruiseId = " + cruiseId);
                        while (results.next()) {
                            System.out.println("Excursion ID - " + results.getString(1) + " | Excursion name - " + results.getString(4));
                        }
                    }
                    break;
                case "5":
                    //Book excursions
                    //storeInput[1] = cabin number; storeInput[2] = excursion id; storeInput[3] = number of required seats
                    String excursionId;
                    String portId;
                    String excursionNames;
                    String customerExcursions;
                    String excursionBookings;
                    int excursionAvailibility = 35;
                    results = statement.executeQuery("SELECT * FROM excursions WHERE excursionId = "+storeInput[2]);
                    while(results.next()){
                        //Store fetched info in variables
                        excursionId = results.getString(1);
                        portId = results.getString(3);
                        excursionNames = results.getString(4);
                        excursionBookings = results.getString(5);
                        results = statement.executeQuery("SELECT * FROM customers WHERE cabinNo = "+storeInput[1]);
                        while(results.next()) {
                            //Store and previous entries from customer's bookings. If there is none, set the value empty
                            customerExcursions = results.getString(4);
                            if(customerExcursions == null){
                                customerExcursions = "";
                            }
                            //Check if excursion ID exists. If it does, update relevant tables
                            if (excursionId.equals(storeInput[2])) {
                                //Prepared statement update @ excursions - excursionBookings
                                PreparedStatement updateExcursionBookings = connection.prepareStatement("UPDATE excursions SET excursionBookings = ? WHERE excursionId = ?");
                                updateExcursionBookings.setString(1, excursionBookings + (" | " + storeInput[1] + " - " + storeInput[3]));
                                //updateExcursionBookings.setString(1, String.valueOf((excursionBookings + Integer.parseInt(storeInput[3]))));
                                updateExcursionBookings.setString(2, excursionId);
                                updateExcursionBookings.executeUpdate();

                                //Prepared statement update @ customers - bookings
                                System.out.println(storeInput[1]);
                                PreparedStatement updateCustomerBookings = connection.prepareStatement("UPDATE customers SET bookings = ? WHERE cabinNo = ?");
                                updateCustomerBookings.setString(1, (customerExcursions + "Port ID: " + portId + " - Excursion: " + excursionNames +"; "));
                                updateCustomerBookings.setString(2, storeInput[1]);
                                updateCustomerBookings.executeUpdate();
                                System.out.println("updated");
                            } else {
                                System.out.println("Invalid Excursion ID");
                            }
                        }
                    }
                    //Booking excursions
                    break;
                case "6":
                    //Update bookings
                    ArrayList<String> storeInfo = new ArrayList<>();
                    results = statement.executeQuery("SELECT * FROM cruiseinfo WHERE cruiseId = 10;");
                    String info = "";
                    while(results.next()){
                        info = info + results.getString(5);
                        //storeInfo.add(results.getString(5));
                    }
                    //System.out.println(info);
                    processDbInfo(info);
                    /*for(int x=0; x<storeInfo.size();x++){
                        System.out.println(storeInfo.get(x));
                    }*/


                    //Updating excursions
                case "7":
                    //TEST
                    String email = "10";//storeInput[1];
                    PreparedStatement stmn = connection.prepareStatement("SELECT excursions.excursionName FROM cruiseInfo " +
                            "INNER JOIN excursions ON cruiseInfo.excursionId = excursions.excursionId WHERE cruiseId = ?");
                    stmn.setString(1,email);
                    results = stmn.executeQuery();
                    while (results.next()) {
                        info = results.getString("excursionName") +",";
                        System.out.println(info);
                    }
            }
        }
        catch(SQLException e){
            System.out.println("SQL exception"+e);
        }
        catch(ArrayIndexOutOfBoundsException e){
            System.out.println("Array Index out of bounds");
        }
        finally{
            if(statement!=null){
                try{
                    statement.close();
                }
                catch (SQLException e){
                    System.out.println("Exception at statemnet.close()");
                }
            }
            if(results!=null){
                try{
                    results.close();
                }
                catch(SQLException e){
                    System.out.println("Exception at results.close()");
                }
            }
            if(connection!=null){
                try{
                    connection.close();
                }
                catch(SQLException e){
                    System.out.println("Exception at connection.close()");
                }
            }
        }
    }
}


/*
    public Threader(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run(){
        try {
            BufferedReader readInput = new BufferedReader((new InputStreamReader(socket.getInputStream())));
            PrintWriter writeOutput = new PrintWriter(socket.getOutputStream(), true);
            while(true){
                String echoString = readInput.readLine();
                System.out.println("Received client readInput: "+echoString);
                if(echoString.equals("exit")){
                    break;
                }
                */
/*try{
                    Thread.sleep(15000);
                }
                catch(InterruptedException e){
                    System.out.println("Thread interrupted");
                }
                writeOutput.println(echoString);*//*

            }

        }catch(IOException e){
            System.out.println("Oops: " + e.getMessage());
        }finally{
            try{
                socket.close();
            }catch(IOException e){
                //later!
            }
        }
    }
}
*/
