import java.sql.*;
import java.util.Scanner;

public class back{
    private Connection conn;

    public back() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:database/plants.db");
    }
    public void close() throws SQLException {
        conn.close();
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


    public  static void  main(String[] args) throws  Exception{

        Class.forName("org.sqlite.JDBC");

        back app = new back();

        System.out.println("Connected to db \n\n tpye quit to stop");


        app.add("test","test","test","test");
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
