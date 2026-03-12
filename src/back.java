import java.sql.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class back{
    private Connection conn;

    public back() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:./database/plants.db");
    }
    public void close() throws SQLException {
        conn.close();
    }

   private void remove(String entry) throws SQLException{

        PreparedStatement removed = conn.prepareStatement(
                    "DELETE FROM plants Where common_name = ?" );

        removed.setString(1,entry);


        int done = removed.executeUpdate();


        System.out.println("removed " + done + " entr" + (done == 1 ? "y" : "ies"));

        removed.close();
    }

    private void add(String Symbol,String SciName,String CommonName, String Region) throws SQLException{

        PreparedStatement added = conn.prepareStatement(
                                                        "INSERT INTO plants (symbol, scientific_name, common_name, state) VALUES (?, ?, ?, ?)" );

        added.setString(1, Symbol);
        added.setString(2, SciName);
        added.setString(3, CommonName);
        added.setString(4, Region);
        added.executeUpdate();

        System.out.println("added "+CommonName +" to the list");

        added.close();
    }

    public List<String[]> searchByName(String name) throws SQLException { // function that searches for a plant by name
    List<String[]> results = new ArrayList<>();
    String query = "SELECT symbol, scientific_name, common_name, state FROM plants WHERE LOWER(common_name) LIKE LOWER(?)";

    try (PreparedStatement searched = conn.prepareStatement(query)) {
        searched.setString(1, "%" + name + "%"); // search for partial matches to what the user entered
        
        try (ResultSet rs = searched.executeQuery()) { // execute the query and get the results
            while (rs.next()) {
                String[] plantData = {
                    rs.getString("symbol"),
                    rs.getString("scientific_name"),
                    rs.getString("common_name"),
                    rs.getString("state")
                };
                results.add(plantData); 
            }
        }
        return results;
    } catch (SQLException e) {
        System.out.println("Error searching for plant: " + e.getMessage());
        return results; // returns an empty list if an error occurs
    }
}

    public List<String[]> searchByState(String state) throws SQLException { // function that searches for a plant by state
        List<String[]> results = new ArrayList<>();
        String query = "SELECT * FROM plants WHERE LOWER(state) = LOWER(?)";

        try (PreparedStatement searched = conn.prepareStatement(query)) {
            searched.setString(1, state);
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

        System.out.println("Connected to db \n\n tpye quit to stop");


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
