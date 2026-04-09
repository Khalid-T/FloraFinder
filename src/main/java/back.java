import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import io.javalin.Javalin;

public class back {
    private boolean is_admin = false;
    public Connection conn; // inialision conn to the database so i can access it form anywhere

    public back() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:./database/database.db");
        initTables();
    }

    private void initTables() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute(
                "CREATE TABLE IF NOT EXISTS collections (" +
                        "  id       INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "  user_id  INTEGER NOT NULL REFERENCES users(id)  ON DELETE CASCADE," +
                        "  plant_id INTEGER NOT NULL REFERENCES plants(id) ON DELETE CASCADE," +
                        "  UNIQUE(user_id, plant_id)" +
                        ")");
        stmt.close();
        System.out.println("[Log] Collections table verified/created");
    }

    public String addToCollection(String username, int plantId) throws SQLException {
        // Resolve username → user id
        PreparedStatement findUser = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ?");
        findUser.setString(1, username);
        ResultSet rs = findUser.executeQuery();
        if (!rs.next())
            return "User not found";
        int userId = rs.getInt("id");
        findUser.close();

        try {
            PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO collections (user_id, plant_id) VALUES (?, ?)");
            ins.setInt(1, userId);
            ins.setInt(2, plantId);
            ins.executeUpdate();
            ins.close();
            System.out.println("[log] " + username + " added plant " + plantId + " to collection");
            return "added";
        } catch (SQLException e) {
            // UNIQUE constraint violation — already in collection
            if (e.getMessage().contains("UNIQUE"))
                return "already";
            throw e;
        }
    }

    public String removeFromCollection(String username, int plantId) throws SQLException {
        PreparedStatement findUser = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ?");
        findUser.setString(1, username);
        ResultSet rs = findUser.executeQuery();
        if (!rs.next())
            return "User not found";
        int userId = rs.getInt("id");
        findUser.close();

        PreparedStatement del = conn.prepareStatement(
                "DELETE FROM collections WHERE user_id = ? AND plant_id = ?");
        del.setInt(1, userId);
        del.setInt(2, plantId);
        del.executeUpdate();
        del.close();
        System.out.println("[log] " + username + " removed plant " + plantId + " from collection");
        return "removed";
    }

    public List<String[]> getCollection(String username) throws SQLException {
        List<String[]> results = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT p.id, p.common_name, p.sci_name, p.family, p.genus, p.species_epithet, " +
                        "       p.care_level, p.watering, p.origin, p.description, p.image_url " +
                        "FROM plants p " +
                        "JOIN collections c ON c.plant_id = p.id " +
                        "JOIN users u ON u.id = c.user_id " +
                        "WHERE u.username = ?");
        stmt.setString(1, username);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            results.add(new String[] {
                    String.valueOf(rs.getInt("id")),
                    rs.getString("common_name"),
                    rs.getString("sci_name"),
                    rs.getString("family"),
                    rs.getString("genus"),
                    rs.getString("species_epithet"),
                    rs.getString("care_level"),
                    rs.getString("watering"),
                    rs.getString("origin"),
                    rs.getString("description"),
                    rs.getString("image_url")
            });
        }
        stmt.close();
        return results;
    }

    // finds plants not in users collection that share 2+ matching fields with any
    // collected plant. Returns up to 5 plants
    public List<String[]> getRecommendations(String username) throws SQLException {
        List<String[]> collection = getCollection(username);

        // Nothing to base recommendations on
        if (collection.isEmpty())
            return new ArrayList<>();

        // Fetch all plants not already in this user's collection
        PreparedStatement allPlants = conn.prepareStatement(
                "SELECT p.id, p.common_name, p.sci_name, p.family, p.genus, p.species_epithet, " +
                        "       p.care_level, p.watering, p.origin, p.description, p.image_url " +
                        "FROM plants p " +
                        "WHERE p.id NOT IN (" +
                        "  SELECT c.plant_id FROM collections c " +
                        "  JOIN users u ON u.id = c.user_id WHERE u.username = ?" +
                        ")");
        allPlants.setString(1, username);
        ResultSet rs = allPlants.executeQuery();

        List<String[]> candidates = new ArrayList<>();
        while (rs.next()) {
            candidates.add(new String[] {
                    String.valueOf(rs.getInt("id")),
                    rs.getString("common_name"),
                    rs.getString("sci_name"),
                    rs.getString("family"), // [3]
                    rs.getString("genus"), // [4]
                    rs.getString("species_epithet"),
                    rs.getString("care_level"), // [6]
                    rs.getString("watering"), // [7]
                    rs.getString("origin"), // [8]
                    rs.getString("description"),
                    rs.getString("image_url")
            });
        }
        allPlants.close();

        // Score each candidate: count how many fields match ANY collected plant
        // Comparable fields: care_level[6], watering[7], origin[8], family[3], genus[4]
        List<String[]> qualified = new ArrayList<>();
        for (String[] candidate : candidates) {
            int bestScore = 0;
            for (String[] collected : collection) {
                int score = 0;
                if (eq(candidate[3], collected[3]))
                    score++; // family
                if (eq(candidate[4], collected[4]))
                    score++; // genus
                if (eq(candidate[6], collected[6]))
                    score++; // care_level
                if (eq(candidate[7], collected[7]))
                    score++; // watering
                if (eq(candidate[8], collected[8]))
                    score++; // origin
                if (score > bestScore)
                    bestScore = score;
            }
            if (bestScore >= 2)
                qualified.add(candidate);
        }

        java.util.Collections.shuffle(qualified);
        return qualified.subList(0, Math.min(5, qualified.size()));
    }

    // equality helper
    private boolean eq(String a, String b) {
        if (a == null || b == null)
            return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    public void close() throws SQLException {
        conn.close();
    }

    // ----------------------- reset password
    // -------------------------------------------
    public String reset_password(String username, String password, String new_pass) throws SQLException {
        PreparedStatement check = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
        check.setString(1, username);
        check.setString(2, password);
        ResultSet rs = check.executeQuery();

        if (rs.next() == false) {
            System.out.println("[log] failed reset password for " + username);
            return "Username or password is wrong";
        }

        if (new_pass.equals(password)) {
            return "New password cannot equal old password";

        }

        PreparedStatement update = conn.prepareStatement("UPDATE users SET password = ? WHERE username = ?");
        update.setString(1, new_pass);
        update.setString(2, username);
        update.executeUpdate();

        System.out.println("[log] Password for " + username + " has been updated");
        return "Password updated!";
    }

    // ========================== Remove User ====================================
    public String removeusr(String username) throws SQLException {
        if (is_admin == false) {
            System.out.println("[log] login first");
            return "login as an admin first";
        }
        PreparedStatement removed = conn.prepareStatement(
                "DELETE FROM  users Where username = ?");

        removed.setString(1, username);
        removed.executeUpdate();
        removed.close();

        System.out.println("[log]  User Removed " + username);
        return username + " has been removed successfully.";
    }

    // ================================== SHOW ALL USERS =======================
    public String getAllUsers() throws SQLException {
        if (is_admin == false) {
            System.out.println("[log] login first");
            return "login as an admin first";
        }
        StringBuilder result = new StringBuilder();
        String sql = "SELECT * FROM users";

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            String row = rs.getInt("id") + "\t" +
                    rs.getString("username") + "\t" +
                    (rs.getInt("admin") == 1 ? "admin" : "guest");
            System.out.println(row);
            result.append(row).append("\n");
        }
        return result.toString();

    }

    // ================================ Promote ==================================
    public String promote(String username) throws SQLException {
        if (is_admin == false) {
            System.out.println("[log] login first");
            return "login as an admin first";
        }

        PreparedStatement promo = conn.prepareStatement("UPDATE users SET admin = ? WHERE username = ?");

        promo.setString(1, "1");
        promo.setString(2, username);

        promo.executeUpdate();
        promo.close();

        System.out.println("[log] " + username + " has been promoted");
        return username + " has been promoted";
    }

    public boolean isAdmin() {
        return is_admin;
    }

    // ================================= Sign Up ===================================
    public String sign_up(String username, String password, int admin) throws SQLException {

        String insertSql = "INSERT INTO users (username, password, admin) VALUES (?, ?, ?)";

        PreparedStatement addusr = conn.prepareStatement(insertSql);

        addusr.setString(1, username);
        addusr.setString(2, password);
        addusr.setInt(3, admin);

        addusr.executeUpdate();
        addusr.close();

        if (admin == 0) {
            System.out.println("[log] User " + username + " created (non-admin)");
            return "User " + username + " has been added and is not admin";

        } else {
            System.out.println("[log] User " + username + " created (admin)");
            return "User " + username + " has been added and is admin";

        }
    }

    public void logout() {
        if (is_admin == true) {
            System.out.println("[log] admin has logged off");
            is_admin = false;

        } else {
            System.out.println("[log] user has logged off");
            is_admin = false;

        }

    }

    // ------------------------------------login
    // --------------------------------------------
    public boolean login(String username, String password) throws SQLException {

        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
        stmt.setString(1, username);
        stmt.setString(2, password);

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

    // ------------------------- remove fuction-----------------------
    public String remove(String entry) throws SQLException {
        if (is_admin == false) {
            System.out.println("[log] login first");
            return "login as an admin first";
        }
        PreparedStatement removed = conn.prepareStatement(
                "DELETE FROM plants Where common_name = ?");

        removed.setString(1, entry);

        int done = removed.executeUpdate();

        System.out.println("[log] removed " + done + " entr" + (done == 1 ? "y" : "ies"));

        removed.close();
        return "Removed " + entry + " form the database";
    }

    public String updateDescription(String commonName, String newDescription) throws SQLException {
        if (is_admin == false) {
            return "login as admin first";
        }
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE plants SET description = ? WHERE common_name = ?");
        stmt.setString(1, newDescription);
        stmt.setString(2, commonName);
        int updated = stmt.executeUpdate();
        stmt.close();
        if (updated > 0) {
            return "Description updated for " + commonName;
        } else {
            return "Plant not found: " + commonName;
        }
    }
    public String updatePlant(String oldCommonName, String newCommonName, String sciName, String family,
                              String genus, String speciesEpithet, String careLevel, String watering,
                              String origin, String description) throws SQLException {
        if (is_admin == false) {
            return "login as admin first";
        }
        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE plants SET common_name = ?, sci_name = ?, family = ?, genus = ?, species_epithet = ?, " +
                        "care_level = ?, watering = ?, origin = ?, description = ? WHERE common_name = ?");
        stmt.setString(1, newCommonName);
        stmt.setString(2, sciName);
        stmt.setString(3, family);
        stmt.setString(4, genus);
        stmt.setString(5, speciesEpithet);
        stmt.setString(6, careLevel);
        stmt.setString(7, watering);
        stmt.setString(8, origin);
        stmt.setString(9, description);
        stmt.setString(10, oldCommonName);
        int updated = stmt.executeUpdate();
        stmt.close();
        if (updated > 0) {
            return "Plant updated";
        } else {
            return "Plant not found";
        }
    }

    // ------------------------------------ add plants to database
    // --------------------------
    public String add(String common_name, String sci_name, String family, String genus, String species_epithet,
                      String care_level, String watering, String origin, String description, String image_url) throws SQLException {

        if (is_admin == false) {
            System.out.println("[log] login first");
            return "login as admin before adding plants";
        }

        PreparedStatement added = conn.prepareStatement(
                "INSERT INTO plants (common_name, sci_name, family, genus, species_epithet, care_level, watering, origin, description, image_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        added.setString(1, common_name);
        added.setString(2, sci_name);
        added.setString(3, family);
        added.setString(4, genus);
        added.setString(5, species_epithet);
        added.setString(6, care_level);
        added.setString(7, watering);
        added.setString(8, origin);
        added.setString(9, description);
        added.setString(10, image_url);
        added.executeUpdate();

        System.out.println("[log] added " + common_name + " to the database");

        added.close();
        return "\nadded " + common_name + " to the list";
    }

    // -------------------- searchWithFilters
    // ----------------------------------------
    // searches by name (partial match) then narrows by any of the following
    // filters:
    // state, light_requirement, water_requirement, plant_type
    public List<String[]> searchWithFilters(String name, String careLevel, String watering, String origin)
            throws SQLException {
        List<String[]> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT id, common_name, sci_name, family, genus, species_epithet, care_level, watering, origin, description, image_url "
                        +
                        "FROM plants WHERE LOWER(common_name) LIKE LOWER(?)");

        if (careLevel != null && !careLevel.isEmpty())
            sql.append(" AND LOWER(care_level) = LOWER(?)");
        if (watering != null && !watering.isEmpty())
            sql.append(" AND LOWER(watering) = LOWER(?)");
        if (origin != null && !origin.isEmpty())
            sql.append(" AND LOWER(origin) LIKE LOWER(?)");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            stmt.setString(idx++, "%" + (name != null ? name : "") + "%");
            if (careLevel != null && !careLevel.isEmpty())
                stmt.setString(idx++, careLevel);
            if (watering != null && !watering.isEmpty())
                stmt.setString(idx++, watering);
            if (origin != null && !origin.isEmpty())
                stmt.setString(idx++, "%" + origin + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new String[] {
                            String.valueOf(rs.getInt("id")),
                            rs.getString("common_name"),
                            rs.getString("sci_name"),
                            rs.getString("family"),
                            rs.getString("genus"),
                            rs.getString("species_epithet"),
                            rs.getString("care_level"),
                            rs.getString("watering"),
                            rs.getString("origin"),
                            rs.getString("description"),
                            rs.getString("image_url")
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in searchWithFilters: " + e.getMessage());
        }

        return results;
    }

    public static void main(String[] args) throws Exception {
        // connection logic
        back appLogic = new back();

        // start the Javalin Server
        Javalin server = Javalin.create(config -> {
            config.staticFiles.add("/static");
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = "./uploads";
                staticFiles.location = io.javalin.http.staticfiles.Location.EXTERNAL;
                staticFiles.hostedPath = "/uploads";
            });
        }).start(8080);
        System.out.println("--- Flora Catalogue Server Running ---");
        System.out.println("Go to: http://localhost:8080/signin.html");

        // handle the Login Form Submission
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
        server.get("/logout", ctx -> {
            ctx.req().getSession().invalidate();
            appLogic.logout();
            ctx.redirect("/signin.html");
        });
        server.post("/add-plant", ctx -> {
            String commonName = ctx.formParam("common_name");
            String sciName = ctx.formParam("sci_name");
            String family = ctx.formParam("family");
            String genus = ctx.formParam("genus");
            String speciesEpithet = ctx.formParam("species_epithet");
            String careLevel = ctx.formParam("care_level");
            String watering = ctx.formParam("watering");
            String origin = ctx.formParam("origin");
            String description = ctx.formParam("description");
            String imageUrl = ctx.formParam("image_url");
            String result = appLogic.add(commonName, sciName, family, genus, speciesEpithet, careLevel, watering,
                    origin, description, imageUrl);
            ctx.result(result);
        });

        server.post("/remove-plant", ctx -> {
            String commonName = ctx.formParam("common_name");

            String result = appLogic.remove(commonName);

            ctx.result(result);
        });
        server.post("/update-description", ctx -> {
            String commonName = ctx.formParam("common_name");
            String description = ctx.formParam("description");
            String result = appLogic.updateDescription(commonName, description);
            ctx.result(result);
        });
        server.post("/update-plant", ctx -> {
            String oldName = ctx.formParam("old_common_name");
            String newName = ctx.formParam("common_name");
            String sciName = ctx.formParam("sci_name");
            String family = ctx.formParam("family");
            String genus = ctx.formParam("genus");
            String speciesEpithet = ctx.formParam("species_epithet");
            String careLevel = ctx.formParam("care_level");
            String watering = ctx.formParam("watering");
            String origin = ctx.formParam("origin");
            String description = ctx.formParam("description");
            String result = appLogic.updatePlant(oldName, newName, sciName, family, genus, speciesEpithet, careLevel, watering, origin, description);
            ctx.result(result);
        });
        server.post("/upload-image", ctx -> {
            if (!appLogic.isAdmin()) {
                ctx.status(401).result("admin only");
                return;
            }
            var file = ctx.uploadedFile("image");
            if (file == null) {
                ctx.status(400).result("no file");
                return;
            }
            String ext = file.extension().toLowerCase();
            if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png")) {
                ctx.status(400).result("only jpg/png allowed");
                return;
            }
            String safeName = file.filename().replaceAll("[^a-zA-Z0-9._-]", "_");
            String filename = System.currentTimeMillis() + "_" + safeName;
            java.nio.file.Path dest = java.nio.file.Paths.get("./uploads/" + filename);
            java.nio.file.Files.createDirectories(dest.getParent());
            try (var in = file.content()) {
                java.nio.file.Files.copy(in, dest);
            }
            System.out.println("[log] uploaded image: " + filename);
            ctx.result("/uploads/" + filename);
        });
        server.get("/search-plants", ctx -> {
            String query = ctx.queryParam("q");
            String careLevel = ctx.queryParam("care_level");
            String watering = ctx.queryParam("watering");
            String origin = ctx.queryParam("origin");

            System.out.println(">>> SEARCH — name: " + query
                    + " | care: " + careLevel + " | watering: " + watering
                    + " | origin: " + origin);

            List<String[]> results = appLogic.searchWithFilters(query, careLevel, watering, origin);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                String[] p = results.get(i);
                json.append("[");
                for (int j = 0; j < p.length; j++) {
                    String cleaned = (p[j] == null) ? "" : p[j].replace("\\", "\\\\").replace("\"", "\\\"");
                    json.append("\"").append(cleaned).append("\"");
                    if (j < p.length - 1)
                        json.append(",");
                }
                json.append("]");
                if (i < results.size() - 1)
                    json.append(",");
            }
            json.append("]");

            ctx.contentType("application/json");
            ctx.result(json.toString());
        });
        //
        server.get("/featured-plants", ctx -> {
            List<String[]> results = appLogic.searchWithFilters("", null, null, null);
            // Pick 10 random plants
            java.util.Collections.shuffle(results);
            List<String[]> featured = results.subList(0, Math.min(9, results.size()));

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < featured.size(); i++) {
                String[] p = featured.get(i);
                json.append("[");
                for (int j = 0; j < p.length; j++) {
                    String cleaned = (p[j] == null) ? "" : p[j].replace("\\", "\\\\").replace("\"", "\\\"");
                    json.append("\"").append(cleaned).append("\"");
                    if (j < p.length - 1)
                        json.append(",");
                }
                json.append("]");
                if (i < featured.size() - 1)
                    json.append(",");
            }
            json.append("]");

            ctx.contentType("application/json");
            ctx.result(json.toString());
        });
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

        // redirect from sign up to sign in
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
        // Add to collection (wishlist)
        server.post("/add-to-collection", ctx -> {
            String user = ctx.sessionAttribute("currentUser");
            if (user == null) {
                ctx.status(401).result("not logged in");
                return;
            }

            String plantIdStr = ctx.formParam("plant_id");
            if (plantIdStr == null) {
                ctx.status(400).result("missing plant_id");
                return;
            }

            int plantId = Integer.parseInt(plantIdStr);
            String result = appLogic.addToCollection(user, plantId);
            ctx.result(result);
        });
        // Remove from Collection
        server.post("/remove-from-collection", ctx -> {
            String user = ctx.sessionAttribute("currentUser");
            if (user == null) {
                ctx.status(401).result("not logged in");
                return;
            }

            String plantIdStr = ctx.formParam("plant_id");
            if (plantIdStr == null) {
                ctx.status(400).result("missing plant_id");
                return;
            }

            int plantId = Integer.parseInt(plantIdStr);
            String result = appLogic.removeFromCollection(user, plantId);
            ctx.result(result);
        });
        // Get Collection (returns same JSON format as /search-plants)
        server.get("/get-collection", ctx -> {
            String user = ctx.sessionAttribute("currentUser");
            if (user == null) {
                ctx.status(401).result("[]");
                return;
            }

            List<String[]> results = appLogic.getCollection(user);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                String[] p = results.get(i);
                json.append("[");
                for (int j = 0; j < p.length; j++) {
                    String cleaned = (p[j] == null) ? "" : p[j].replace("\\", "\\\\").replace("\"", "\\\"");
                    json.append("\"").append(cleaned).append("\"");
                    if (j < p.length - 1)
                        json.append(",");
                }
                json.append("]");
                if (i < results.size() - 1)
                    json.append(",");
            }
            json.append("]");

            ctx.contentType("application/json");
            ctx.result(json.toString());
        });

        server.get("/get-recommendations", ctx -> {
            String user = ctx.sessionAttribute("currentUser");
            if (user == null) {
                ctx.status(401).result("[]");
                return;
            }

            List<String[]> results = appLogic.getRecommendations(user);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < results.size(); i++) {
                String[] p = results.get(i);
                json.append("[");
                for (int j = 0; j < p.length; j++) {
                    String cleaned = (p[j] == null) ? "" : p[j].replace("\\", "\\\\").replace("\"", "\\\"");
                    json.append("\"").append(cleaned).append("\"");
                    if (j < p.length - 1)
                        json.append(",");
                }
                json.append("]");
                if (i < results.size() - 1)
                    json.append(",");
            }
            json.append("]");

            ctx.contentType("application/json");
            ctx.result(json.toString());
        });

        // reset password
        server.post("/reset-password", ctx -> {
            String user = ctx.formParam("username");
            String oldPass = ctx.formParam("old_password");
            String newPass = ctx.formParam("new_password");

            try {
                String result = appLogic.reset_password(user, oldPass, newPass);

                if (result.equals("Password updated!")) {
                    // Redirect to signin with a success message
                    ctx.redirect("/signin.html?reset=success");
                } else {
                    // Redirect back with the specific error message
                    ctx.redirect("/reset.html?error=" + java.net.URLEncoder.encode(result, "UTF-8"));
                }
            } catch (SQLException e) {
                ctx.redirect("/reset.html?error=Database error");
            }
        });
    }

}
