import java.sql.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

// compile using ---- javac -cp ".;lib/*" src/back.java
// run using ---- java -cp ".;src;lib/*" back

public class back{
    private boolean is_admin =false;
    private Connection conn; // inialision conn to the database so i can acces it form anywhere
    public back() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:./database/database.db");
    }
    public void close() throws SQLException {
        conn.close();
    }


    //------------------------------------login --------------------------------------------

    private boolean login(String username, String password) throws SQLException{

        PreparedStatement stmt = conn.prepareStatement( "SELECT * FROM users WHERE username = ? AND password = ?");
         stmt.setString(1,username);
         stmt.setString(2,password);

         boolean success = false;

         ResultSet rs = stmt.executeQuery();
         if (rs.next()) {
             success = true;
             is_admin = rs.getInt("admin") == 1;
         }

         rs.close();

         stmt.close();
         if (success) {
             System.out.println("[log] " + username + " has logged in");

             if (is_admin) {
                 System.out.println("[log] user is admin");
             } else {
                 System.out.println("[log] user is normal user");
             }

         } else {
             System.out.println("[log] " + username + " login didn't work");
         }

    return success;    }
    //---------------------------------------------------------------



    // ------------------------- remove fuction-----------------------
    private String remove(String entry) throws SQLException{
        if (is_admin == false){
             System.out.println("[log] login first");
            return "login as an admin first";
        }
        PreparedStatement removed = conn.prepareStatement(
                    "DELETE FROM plants Where common_name = ?" );

        removed.setString(1,entry);

        int done = removed.executeUpdate();


        System.out.println("[log] removed " + done + " entr" + (done == 1 ? "y" : "ies"));

        removed.close();
        return "Removed " +entry+" form the database";
    }
    //-------------------------------------------------------------------------------------



    // ------------------------------------ add plants to database --------------------------
    private String  add(String Symbol,String SciName,String CommonName, String Region) throws SQLException{
        if (is_admin == false){
            System.out.println("[log] login first");
            return "login as admin before adding plants";
        }
        PreparedStatement added = conn.prepareStatement(
                                                        "INSERT INTO plants (symbol, scientific_name, common_name, state) VALUES (?, ?, ?, ?)" );

        added.setString(1, Symbol);
        added.setString(2, SciName);
        added.setString(3, CommonName);
        added.setString(4, Region);
        added.executeUpdate();

        System.out.println("[log] added "+ CommonName+ " to the database");

        added.close();
        return "\nadded "+ CommonName+ " to the list";

    }
    //-------------------------------------------------------------------------------



    //-------------------- SearchByName --------------------------------------------
    public List<String[]> searchByName(String name) throws SQLException {
        List<String[]> results = new ArrayList<>();
        String query = "SELECT symbol, scientific_name, common_name, state FROM plants WHERE LOWER(common_name) LIKE LOWER(?)";

        try (PreparedStatement searched = conn.prepareStatement(query)) {
            searched.setString(1, "%" + name + "%");  // adding % allows partial matches
            try (ResultSet rs = searched.executeQuery()) {
                while (rs.next()) {
                    results.add(new String[]{
                            rs.getString("symbol"), rs.getString("scientific_name"),
                            rs.getString("common_name"), rs.getString("state")
                        });
                }
            }
            return results;
        } catch (SQLException e) {
            System.out.println("Error searching by name: " + e.getMessage());
            return results;
        }
    }
    //-------------------------------------------------------------------------------



    //-------------------- SearchByState --------------------------------------------

    public List<String[]> searchByState(String state) throws SQLException {
        List<String[]> results = new ArrayList<>();
        // using LIKE instead of '=' makes it feel more like a flexible filter
        String query = "SELECT symbol, scientific_name, common_name, state FROM plants WHERE LOWER(state) LIKE LOWER(?)";

        try (PreparedStatement searched = conn.prepareStatement(query)) {
            searched.setString(1, "%" + state + "%"); // adding % allows partial matches
            try (ResultSet rs = searched.executeQuery()) {
                while (rs.next()) {
                    results.add(new String[]{
                            rs.getString("symbol"),
                            rs.getString("scientific_name"),
                            rs.getString("common_name"),
                            rs.getString("state")
                        });
                }
            }
        }
        return results;
    }


    public  static void  main(String[] args) throws  Exception{

        Class.forName("org.sqlite.JDBC");

        back app = new back();

        System.out.println("Connected to db \n\n");
        //app.login("admin","admin");
        app.add("test","test","test","test");
        app.remove("test");

        app.login("admin","admin");

        app.add("test","test","test","test");

        app.remove("test");
        // Scanner input = new Scanner(System.in);


        /*
          while (true){
          input.nextLine();
          if (input.equals("quit")){
          break;

          }else if (input.equals("add")){


          }


          }
          input.close();

        */

        app.close();
    }
}
