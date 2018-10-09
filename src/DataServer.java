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
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private static ArrayList <String> usersOnline = new ArrayList<>();

    public DataServer(Socket socket) {
        this.socket = socket;
    }
    @Override

    public void run() {
        boolean active = true;
        while (active) {
            try {
                input = new BufferedReader((new InputStreamReader(socket.getInputStream())));
                output = new PrintWriter(socket.getOutputStream(), true);
                String storeInput = input.readLine();
                if(storeInput != null) {
                    connectToDb(storeInput);
                }

            } catch (IOException e) {
                System.out.println("IO exception: "+e.getMessage());
                e.printStackTrace();

            } catch (SQLException e){
                System.out.println("SQL Exception: "+e.getMessage());
                e.printStackTrace();

            } catch (NullPointerException e){
                System.out.println("NullPointer Exception: "+e.getMessage());
                e.printStackTrace();

            } finally {
                active = false;
            }
        }
    }

    //Method that takes the input string and sorts it into a String array in String chunks
    public String [] processInput(String input){
        String [] processedInput = input.split(",");
        return processedInput;
    }
    //Method that checks if a user account is logged in
    public boolean checkUsersOnline(String input){
        for(int i = 0; i < usersOnline.size(); i++){
            if(usersOnline.get(i).equals(input)){
                System.out.println("input is: "+input);
                return true;
            }
        }
        return false;
    }

    //Make a database connection
    public Connection makeConnection()throws SQLException {
        String username = "Client";
        String password = "ClientAccess";
        String databaseName = "groupproject";
        String databasePath = "jdbc:mysql://localhost:3306/"+databaseName+"?autoReconnect=true&useSSL=false";

        try{
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(databasePath, username, password);
        }
        catch (Exception e){
            System.out.println(e);
            e.printStackTrace();
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
            Statement statement = connection.createStatement();

            //Creating a switch statement that processes the incoming message from the client
            //Using the value of the first element of the Array, the switch statement takes appropriate action

            switch (storeInput[0]) {
                case "1":
                    //Register user
                    String name = storeInput[1];
                    String customerEmail = storeInput[2];
                    String cabinNo = storeInput[3];
                    String password = storeInput[4];
                    String cruiseID = storeInput[5];
                    boolean exists = false;
                    boolean cruiseExists = false;

                    results = statement.executeQuery("SELECT customer.Email, customer.CabinNo FROM customer ");
                    while(results.next()) {
                        if (customerEmail.equals(results.getString("Email")) && cabinNo.equals(results.getString("CabinNo"))) {
                            exists = true;
                            output.println("Failed to register. Email and cabin already registered for this cruise");
                        }
                    }
                    results = statement.executeQuery("SELECT cruise_excursion.CruiseID FROM cruise_excursion");
                    while(results.next()) {
                        if (cruiseID.equals(results.getString("CruiseID"))) {
                            cruiseExists = true;
                        }
                    }
                    if(!cruiseExists){
                    output.println("Entered Cruise ID doesn't exist");
                    }
                    if(!exists && cruiseExists) {
                        PreparedStatement prepStatementRegister = connection.prepareStatement
                                ("INSERT INTO customer (Name, Email, CabinNo, Password, CruiseID) VALUES (?, ?, ?, ?, ?)");
                        prepStatementRegister.setString(1, name);
                        prepStatementRegister.setString(2, customerEmail);
                        prepStatementRegister.setString(3, cabinNo);
                        prepStatementRegister.setString(4, password);
                        prepStatementRegister.setString(5, cruiseID);
                        prepStatementRegister.executeUpdate();
                        output.println("Cabin registered successfully.");
                    }
                    break;
                case "2":
                    //Log in

                    customerEmail = storeInput[1];
                    password = storeInput[2];

                    PreparedStatement prepStatement = connection.prepareStatement("SELECT customer.Email, customer.Password, customer.cruiseId FROM customer;");
                    results = prepStatement.executeQuery();
                    while(results.next()){
                        if(results.getString("Email").equals(customerEmail)) {
                            String cruiseId = results.getString("cruiseId");
                            if (password.equals(results.getString("Password"))){
                                if(checkUsersOnline(customerEmail) == false) {
                                usersOnline.add(storeInput[1]);
                                for (int x = 0; x < usersOnline.size(); x++) {
                                    System.out.println(usersOnline.get(x));
                                }
                                output.println("Logged in successfully" + "," + cruiseId);
                                } else if(usersOnline.contains(customerEmail)){
                                    output.println("Login failed, user already logged in.");
                                }

                                else{
                                    output.println("Login failed, incorrect user email or password");
                                }
                            }else{

                            }
                        }
                    }
                    output.println("Invalid cabin number or password");
                    break;
                case "3":
                    //Log out
                    cabinNo = storeInput[1];
                    usersOnline.remove(cabinNo);
                    output.println("User logged out");
                    System.out.println("User logged out");
                    break;
                case "4":
                    //View available excursions
                    String cruiseId = storeInput[1];
                    if(cruiseId.equals("none")){
                        prepStatement = connection.prepareStatement("SELECT excursion.ExcursionName, excursion.Seats, excursion.ExcursionID " +
                                "FROM cruise_excursion INNER JOIN excursion ON cruise_excursion.PortID = excursion.PortID");
                        results = prepStatement.executeQuery();
                        System.out.println("Running first excursion retrieval");
                    }else {
                        prepStatement = connection.prepareStatement("SELECT excursion.ExcursionName, excursion.Seats, excursion.ExcursionID " +
                                "FROM cruise_excursion INNER JOIN excursion ON cruise_excursion.PortID = excursion.PortID " +
                                "WHERE cruise_excursion.CruiseID = ?");
                        prepStatement.setString(1, cruiseId);
                        results = prepStatement.executeQuery();
                        System.out.println("Running second excursion retrieval");
                    }
                    String result="";
                    while(results.next()){
                        result += results.getString("ExcursionName")+"_"+results.getString("Seats")+"_"+
                                results.getString("Excursion.Seats")+"_"+results.getString("ExcursionID")+",";
                    }
                    System.out.println(result);
                    System.out.println(result);
                    output.println("result string is: "+result);
                    results.close();
                    break;
                case "5":
                    //Book excursions
                    String excursionId = storeInput[1];
                    customerEmail = storeInput[2];
                    String requestedSeats = storeInput[3];
                    String availableSeats = "";
                    System.out.println(excursionId);
                    System.out.println(customerEmail);
                    System.out.println("req"+requestedSeats);

                    prepStatement = connection.prepareStatement("SELECT excursion.Seats FROM excursion WHERE excursion.ExcursionID = ?");
                    prepStatement.setString(1, excursionId);
                    results = prepStatement.executeQuery();
                    while(results.next()){
                        availableSeats = results.getString("excursion.Seats");
                        System.out.println("Available seats:"+availableSeats);
                    }

                    if(Integer.parseInt(availableSeats) >= Integer.parseInt(requestedSeats)){
                        availableSeats = String.valueOf(Integer.parseInt(availableSeats) - Integer.parseInt(requestedSeats));

                        prepStatement = connection.prepareStatement("INSERT INTO excursion_customer VALUES (?, ?, ?)");
                        prepStatement.setString(1, excursionId);
                        prepStatement.setString(2, customerEmail);
                        prepStatement.setString(3, requestedSeats);
                        prepStatement.executeUpdate();

                        prepStatement = connection.prepareStatement("UPDATE excursion SET excursion.Seats = ? WHERE excursion.ExcursionID = ?");
                        prepStatement.setString(1, availableSeats);
                        prepStatement.setString(2, excursionId);
                        prepStatement.executeUpdate();
                        output.println("Booking successful");
                    }else{
                        output.println("Not enough seats available");
                    }
                    break;
                case "6":
                    //Get booking info
                    customerEmail = storeInput[1];
                    result = "";
                    prepStatement = connection.prepareStatement("SELECT excursion.ExcursionName, excursion.Seats, excursion_customer.NoOfSeats " +
                            ", excursion_customer.ExcursionID FROM excursion INNER JOIN excursion_customer ON excursion.ExcursionID = excursion_customer.ExcursionID " +
                            "WHERE excursion_customer.Email = ?");
                    prepStatement.setString(1, customerEmail);
                    results = prepStatement.executeQuery();
                    if(results.next()) {
                        while (results.next()) {
                            result += results.getString("excursion.ExcursionName") + "_" + results.getString("excursion.Seats") + "_"
                                    + results.getString("excursion_customer.NoOfSeats") + "_" + results.getString("excursion_customer.ExcursionID") + ",";
                        }
                        output.println(result);
                    }
                    else{
                        output.println("No information found");
                    }
                    break;
                case "7":
                    //Update booking
                    //Selection code - ExcursionID - Customer email - Requested seats - Modified requested seats
                    excursionId = storeInput[1];
                    customerEmail = storeInput[2];
                    requestedSeats = storeInput[3];
                    availableSeats = "";
                    String originallyBookedSeats = storeInput[4];
                    String newAvailableSeats = "";

                    prepStatement = connection.prepareStatement("SELECT excursion.Seats FROM excursion WHERE excursion.ExcursionID = ?");
                    prepStatement.setString(1, excursionId);
                    results = prepStatement.executeQuery();
                    while(results.next()){
                        availableSeats = results.getString("excursion.Seats");
                    }
                    System.out.println("Amount of available seats before addition: "+availableSeats);
                    availableSeats = Integer.toString(Integer.parseInt(availableSeats)+ Integer.parseInt(originallyBookedSeats));
                    System.out.println("Amount of available seats after addition: "+availableSeats);

                    if(Integer.parseInt(availableSeats)>= Integer.parseInt(requestedSeats)){
                        newAvailableSeats = Integer.toString(Integer.parseInt(availableSeats)-Integer.parseInt(requestedSeats));

                        prepStatement = connection.prepareStatement("UPDATE excursion_customer SET excursion_customer.NoOfSeats = ?" +
                                "WHERE excursion_customer.ExcursionID = ? AND excursion_customer.Email = ?;");
                        prepStatement.setString(1, requestedSeats);
                        prepStatement.setString(2, excursionId);
                        prepStatement.setString(3, customerEmail);
                        prepStatement.executeUpdate();

                        prepStatement = connection.prepareStatement("UPDATE excursion SET excursion.Seats = ? " +
                                "WHERE excursion.ExcursionID = ?;");
                        prepStatement.setString(1, newAvailableSeats);
                        prepStatement.setString(2, excursionId);
                        prepStatement.executeUpdate();
                        output.println("Booking changed successfully");
                    }else{
                        output.println("Failed to change booking");
                        System.out.println("FAILED");
                    }
                    break;
                case "8":
                    //Cancel booking
                    excursionId = storeInput[1];
                    String bookedSeats = storeInput[2];
                    availableSeats = storeInput[3];

                    availableSeats = Integer.toString(Integer.parseInt(bookedSeats) + Integer.parseInt(availableSeats));

                    prepStatement = connection.prepareStatement("DELETE FROM excursion_customer WHERE ExcursionID = ?");
                    prepStatement.setString(1, excursionId);
                    prepStatement.executeUpdate();

                    prepStatement = connection.prepareStatement("UPDATE excursion SET excursion.Seats = ? " +
                            "WHERE excursion.ExcursionID = ?");
                    prepStatement.setString(1,availableSeats);
                    prepStatement.setString(2,excursionId);
                    prepStatement.executeUpdate();

                    output.println("Booking deleted successfully");
                    break;
            }
        }
        catch(SQLException e){
            System.out.println("SQL exception"+e);
            e.printStackTrace();
        }
        catch(ArrayIndexOutOfBoundsException e){
            System.out.println("Array Index out of bounds");
            e.printStackTrace();
        } finally{
            if(statement!=null){
                try{
                    statement.close();
                }
                catch (SQLException e){
                    System.out.println("Exception at statemnet.close()");
                    e.printStackTrace();
                }
            }
            if(results!=null){
                try{
                    results.close();
                }
                catch(SQLException e){
                    System.out.println("Exception at results.close()");
                    e.printStackTrace();
                }
            }
            if(connection!=null){
                try{
                    connection.close();
                }
                catch(SQLException e){
                    System.out.println("Exception at connection.close()");
                    e.printStackTrace();
                }
            }
        }
    }
}