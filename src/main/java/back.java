import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import io.javalin.Javalin;


public class back{
    private boolean is_admin =false;
    public Connection conn; // inialision conn to the database so i can acces it form anywhere
    public back() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:./database/database.db");
    }
    public void close() throws SQLException {
        conn.close();
    }
// ----------------------- reset password -------------------------------------------
  public String reset_password(String username, String password, String new_pass) throws SQLException {
      PreparedStatement check = conn.prepareStatement( "SELECT * FROM users WHERE username = ? AND password = ?");
      check.setString(1, username);
      check.setString(2, password);
      ResultSet rs = check.executeQuery();
      
     if (rs.next() == false) {
         System.out.println("[log] failed reset password for "+ username);
         return "Username or password is wrong";
     }
    
      if (new_pass.equals(password)) {
       return "New password cannot equal old password";

       } 
    
      
   
     
     PreparedStatement update = conn.prepareStatement("UPDATE users SET password = ? WHERE username = ?");
      update.setString(1, new_pass);
      update.setString(2, username);
      update.executeUpdate();

      System.out.println("[log] Password for "+ username + " has been updated");
      return "Password updated!";
  }
    
   //------------------------------------login --------------------------------------------
    public String sign_up(String username, String password, int  admin) throws SQLException {

        String insertSql = "INSERT INTO users (username, password, admin) VALUES (?, ?, ?)";
        
        PreparedStatement addusr = conn.prepareStatement(insertSql);

        addusr.setString(1, username);
        addusr.setString(2, password);
        addusr.setInt(3, admin);

        addusr.executeUpdate();

        if (admin == 0){
             System.out.println("[log] User " + username + " created (non-admin)");
             return "User " + username + " has been added and is not admin";

        }else{
             System.out.println("[log] User " + username + " created (non-admin)");
             return "User " + username + " has been added and is not admin";

        }
    } 
    public void logout(){
        System.out.println("[log] admin has logged off");
        is_admin = false; 
    }
    public boolean login(String username, String password) throws SQLException{

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

    return success;
    }
    //---------------------------------------------------------------


    // ------------------------- remove fuction-----------------------
    public String remove(String entry) throws SQLException{
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
    public String add(String common_name, String sci_name, String family, String genus, String species_epithet, String care_level, String watering, String origin, String description) throws SQLException{

        if (is_admin == false){
            System.out.println("[log] login first");
            return "login as admin before adding plants";
        }

        PreparedStatement added = conn.prepareStatement(
        "INSERT INTO plants (common_name, sci_name, family, genus, species_epithet, care_level, watering, origin, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

        added.setString(1, common_name);
        added.setString(2, sci_name);
        added.setString(3, family);
        added.setString(4, genus);
        added.setString(5, species_epithet);
        added.setString(6, care_level);
        added.setString(7, watering);
        added.setString(8, origin);
        added.setString(9, description);
        added.executeUpdate();

        System.out.println("[log] added "+ common_name+ " to the database");

        added.close();
        return "\nadded "+ common_name+ " to the list";

    }
    //-------------------------------------------------------------------------------



    // -------------------- searchWithFilters ----------------------------------------
    // searches by name (partial match) then narrows by any of the following filters: 
    // state, light_requirement, water_requirement, plant_type
    public List<String[]> searchWithFilters(String name, String state, String light, String water, String plantType) throws SQLException {
        List<String[]> results = new ArrayList<>();
 
        // Build the query dynamically based on which filters are active
        StringBuilder sql = new StringBuilder(
                "SELECT symbol, scientific_name, common_name, state, light_requirement, water_requirement, plant_type, description " +            "FROM plants WHERE LOWER(common_name) LIKE LOWER(?)"
        );
 
        if (state != null && !state.isEmpty())     sql.append(" AND LOWER(state) = LOWER(?)");
        if (light != null && !light.isEmpty())     sql.append(" AND LOWER(light_requirement) = LOWER(?)");
        if (water != null && !water.isEmpty())     sql.append(" AND LOWER(water_requirement) = LOWER(?)");
        if (plantType != null && !plantType.isEmpty()) sql.append(" AND LOWER(plant_type) = LOWER(?)");
 
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            stmt.setString(idx++, "%" + (name != null ? name : "") + "%");
            if (state != null && !state.isEmpty())     stmt.setString(idx++, state);
            if (light != null && !light.isEmpty())     stmt.setString(idx++, light);
            if (water != null && !water.isEmpty())     stmt.setString(idx++, water);
            if (plantType != null && !plantType.isEmpty()) stmt.setString(idx++, plantType);
 
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new String[]{
                        rs.getString("symbol"),
                        rs.getString("scientific_name"),
                        rs.getString("common_name"),
                        rs.getString("state"),
                        rs.getString("light_requirement"),
                        rs.getString("water_requirement"),
                            rs.getString("plant_type"),
                            rs.getString("description")

                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in searchWithFilters: " + e.getMessage());
        }
 
        return results;
    }


    public boolean isAdmin() {
        return is_admin;
    }


    public static void main(String[] args) throws Exception {
        //connection logic
        back appLogic = new back();

        //start the Javalin Server
        Javalin server = Javalin.create(config -> {
            // Tells Javalin to look in src/main/resources/static for HTML/CSS
            config.staticFiles.add("/static");
        }).start(8080);

        System.out.println("--- Flora Catalogue Server Running ---");
        System.out.println("Go to: http://localhost:8080/signin.html");

        //handle the Login Form Submission
        server.post("/login-endpoint", ctx -> {
            System.out.println(">>> LOGIN ATTEMPT RECEIVED <<<");

            String user = ctx.formParam("username");
            String pass = ctx.formParam("password");

            if (appLogic.login(user, pass)) {
                ctx.sessionAttribute("currentUser", user);
                ctx.redirect("/index.html");

            } else {
                ctx.redirect("/signin.html?error=1");
            }
        });
        server.get("/logout", ctx ->{
            ctx.req().getSession().invalidate();
            appLogic.logout();
            ctx.redirect("/signin.html");
        });
       server.post("/add-plant", ctx -> {
               String symbol = ctx.formParam("symbol");
               String scientificName = ctx.formParam("scientific_name");
               String commonName = ctx.formParam("common_name");
               String state = ctx.formParam("state");
               String lightRequirement = ctx.formParam("light_requirement");
               String waterRequirement = ctx.formParam("water_requirement");
               String plantType = ctx.formParam("plant_type");
               String description = ctx.formParam("description");
               String result = appLogic.add(symbol, scientificName, commonName, state, lightRequirement, waterRequirement, plantType, description);
               ctx.result(result);        });

        server.post("/remove-plant", ctx -> {
            String commonName = ctx.formParam("common_name");

            String result = appLogic.remove(commonName);

            ctx.result(result);        });
        server.get("/search-plants", ctx -> {
            String query     = ctx.queryParam("q");
            String state     = ctx.queryParam("state");
            String light     = ctx.queryParam("light");
            String water     = ctx.queryParam("water");
            String plantType = ctx.queryParam("plant_type");
 
            System.out.println(">>> SEARCH — name: " + query
                + " | state: " + state + " | light: " + light
                + " | water: " + water + " | type: " + plantType);
 
            List<String[]> results = appLogic.searchWithFilters(query, state, light, water, plantType);
 
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                String[] p = results.get(i);
                json.append("[");
                for (int j = 0; j < p.length; j++) {
                    String cleaned = (p[j] == null) ? "" : p[j].replace("\\", "\\\\").replace("\"", "\\\"");
                    json.append("\"").append(cleaned).append("\"");
                    if (j < p.length - 1) json.append(",");
                }
                json.append("]");
                if (i < results.size() - 1) json.append(",");
            }
            json.append("]");
 
            ctx.contentType("application/json");
            ctx.result(json.toString());
        });

        //

        server.get("/get-user", ctx -> {
            String user = ctx.sessionAttribute("currentUser");
            if (user != null) {
                ctx.result(user);
            } else {
                ctx.status(401);
            }
        });

        server.get("/is-admin", ctx -> {
            String user = ctx.sessionAttribute("currentUser");
            if (user != null) {
                ctx.result(appLogic.isAdmin() ? "true" : "false");
            } else {
                ctx.status(401);
            }
        });

        //redirect from sign up to sign in
        server.post("/signup-endpoint", ctx -> {
            String user = ctx.formParam("username");
            String pass = ctx.formParam("password");

            try {
                // Register the user as a normal user (admin = 0)
                appLogic.sign_up(user, pass, 0);


                ctx.redirect("/signin.html?registered=true");


                // ctx.sessionAttribute("currentUser", user);
                // ctx.redirect("/index.html");

            } catch (SQLException e) {
                // Redirect back to signup with an error if the username is taken
                ctx.redirect("/signup.html?error=exists");
            }
        });
    }


//    public  static void  main(String[] args) throws  Exception{
//
//        Class.forName("org.sqlite.JDBC");
//
//        back app = new back();
//
//        System.out.println("Connected to db \n\n");
//        //app.login("admin","admin");
//        app.add("test","test","test","test");
//        app.remove("test");
//
//        app.login("admin","admin");
//
//        app.add("test","test","test","test");
//
//        app.remove("test");
//        // Scanner input = new Scanner(System.in);
//
//
//        /*
//          while (true){
//          input.nextLine();
//          if (input.equals("quit")){
//          break;
//
//          }else if (input.equals("add")){
//
//
//          }
//
//
//          }
//          input.close();
//
//        */
//
//        app.close();
//    }
}
