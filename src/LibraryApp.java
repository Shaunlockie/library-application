import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LibraryApp {
    public static boolean isRunning;
    public static boolean loggedIn;
    private static final Scanner in = new Scanner(System.in);
    private static final ArrayList<User> users = new ArrayList<>();
    private static final ArrayList<String> collections = new ArrayList<>();
    private final List<Collection> userCollections = new ArrayList<Collection>();
    private Statement sqlStatement;

    Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/librarydatabase", "root","password");

    public LibraryApp() throws SQLException, ClassNotFoundException {
        isRunning = true;
        loggedIn = false;
        Class.forName("com.mysql.cj.jdbc.Driver");
        this.sqlStatement = con.createStatement();
    }
    public void programLoop(User u) throws SQLException {
        while (isRunning && !loggedIn) {
            mainMenu(u);
            }
        while(isRunning && loggedIn) {
            userMenu(u);
        }
    }

    private User loginUser() throws SQLException {
        System.out.println("Input Username");
        String userAccountName = in.next();
        User u = null;

        try {
            PreparedStatement prepStatement = con.prepareStatement("SELECT * FROM useraccounts WHERE username = (?)");
            prepStatement.setString(1, userAccountName);
            ResultSet r1 = prepStatement.executeQuery();
            String usernameCounter;
            while (r1.next()) {
                u = new User();
                u.setId(r1.getInt("userId"));
                u.setUsername(r1.getString("username"));
                u.setEmail(r1.getString("email"));
                u.setCreatedDate(r1.getDate("createdDate").toLocalDate());

                //System.out.println(u.getId() + " " + u.getUsername()+ " " + u.getEmail()+ " " +  u.getCreatedDate());
                System.out.println("Welcome: " + u.getUsername());
            }
            loggedIn = true;
            userMenu(u);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return u;
    }

    public void createUser() throws SQLException{
        Scanner in = new Scanner(System.in);
        System.out.println("Please input a username");
        String userAccountName = in.nextLine();

        System.out.println("Please input a Email");
        String userEmail = in.nextLine();

        LocalDate userDateCreated = LocalDate.now();
        PreparedStatement prepStatement = con.prepareStatement("insert into userAccounts (username,email,createdDate)" + "values (?,?,?)");
        prepStatement.setString(1,userAccountName);
        prepStatement.setString(2,userEmail);
        prepStatement.setDate(3, Date.valueOf(userDateCreated));

        prepStatement.executeUpdate();
    }

    public void viewUsers() throws SQLException {
        ResultSet result = makeQuery("SELECT * FROM userAccounts");
        while(result.next()) {
            System.out.println(result.getString("username"));
        }
        }

    public void mainMenu(User u) throws SQLException {
        clearConsole();
        System.out.println("What would you like to do?: ");
        System.out.println("1. Create a new user");
        System.out.println("2. Log in to current user");
        System.out.println("3. View all users");
        System.out.println("4.Exit program!");
        int input = readInt("->", 4);
        if (input == 1) {
            createUser();
        }
        if (input == 2) {
            loginUser();
        }
        if (input == 3) {
            viewUsers();
        }
        if (input == 4) {
            System.exit(1);
            }
        }


    public void userMenu(User u) throws SQLException {
        System.out.println("What would you like to do?: ");
        System.out.println("1. Create a new collection");
        System.out.println("2. View collections");
        System.out.println("3. Settings");
        System.out.println("4. logout");
        int input = readInt("->", 4);
        if (input == 1) {
            createCollection(u);
        }
        if (input == 2) {
            for (Collection col : userCollections) {
                System.out.println("Collection: " + col);
            }
        }
        if (input == 3) {
            settingsMenu(u);
        }
        if (input == 4) {
            LogoutUser(u);
            programLoop(u);

        }

    }
    public void settingsMenu(User u) throws SQLException {
        System.out.println("Settings:");
        System.out.println("2. Change Username");
        System.out.println("2. Change Email");
        System.out.println("3. Go back");
        int input = readInt("->", 4);
        if (input == 1){
            changeUsername(u);
        }if (input == 2){
            changeEmail(u);
        }if(input == 3){
            userMenu(u);
        }
    }

    public static int readInt(String prompt, int userChoices) {
        int input;
        do {
            System.out.println(prompt);
            try {
                input = Integer.parseInt(in.next());
            } catch (Exception e) {
                input = -1;
                System.out.println("Please input an integer!");
            }
        } while (input < 1 || input > userChoices);
        return input;
    }

    public void clearConsole() {
        for (int i = 0; i < 1; i++) {
            System.out.println();
        }
    }

    public void LogoutUser(User u) throws SQLException {
        u = null;
        loggedIn = false;
        System.out.println("You are now logged out");
    }

    public void changeUsername(User u) throws SQLException {
        System.out.println("Choose a new username: ");
        String newUsername = in.next();
        int updateId = u.getId();
        PreparedStatement prepStatement = con.prepareStatement("update userAccounts set username=? where userId=?");
        prepStatement.setString(1,newUsername);
        prepStatement.setInt(2, updateId);
        prepStatement.executeUpdate();
        System.out.println("Username changed!  ");
        userMenu(u);
    }
    private void changeEmail(User u) throws SQLException {
        System.out.println("Choose a new email: ");
        String newEmail = in.next();
        int updateId = u.getId();
        PreparedStatement prepStatement = con.prepareStatement("update userAccounts set email=? where userId=?");
        prepStatement.setString(1,newEmail);
        prepStatement.setInt(2, updateId);
        prepStatement.executeUpdate();
        System.out.println("Email changed!  ");
        userMenu(u);
    }
    public void createCollection(User u) throws SQLException {
        Collection col = new Collection();
        col.createCollection();
        col.showCollectionInfo();
        userCollections.add(col);
        serializeCollection(u,col);

    }
    public void serializeCollection(User u, Collection col){
        byte[] data = null;
        int userId = u.getId();
        String collName = col.getName();
        String collDesc = col.getDesc();
        LocalDate collCreatedDate = LocalDate.now();
        try{
            ByteArrayOutputStream byteColl = new ByteArrayOutputStream();
            ObjectOutputStream objColl = new ObjectOutputStream(byteColl);
            objColl.flush();
            objColl.close();
            byteColl.close();
            data = byteColl.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try{
            PreparedStatement prepStatement = con.prepareStatement("insert into bookcollections (userId,collectionName,collectionDesc,collectionCreatedDate,collectionSerialize)" + "values (?,?,?,?,?)");
            prepStatement.setInt(1, userId);
            prepStatement.setString(2, collName);
            prepStatement.setString(3,collDesc);
            prepStatement.setDate(4, Date.valueOf(collCreatedDate));
            prepStatement.setObject(5, data);
            prepStatement.executeUpdate();
            System.out.println("THis is running");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    public ResultSet makeQuery(String givenStatement) throws SQLException {
        setSqlStatement(givenStatement);
        return sqlStatement.executeQuery(givenStatement);
    }
    private void setSqlStatement(String givenStatement) {
        this.sqlStatement = sqlStatement;
    }
}


