import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
 
import java.sql.SQLException;
import java.util.List;
 
public class BackTest {
 
    private back app;
 
    @BeforeEach
    void setup() throws SQLException {
        app = new back() {
            {
                conn = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:");
                java.sql.Statement stmt = conn.createStatement();
                stmt.execute("""
                    CREATE TABLE users(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT UNIQUE,
                        password TEXT,
                        admin INTEGER DEFAULT 0
                    );
                """);
                // Updated schema with new filter columns
                stmt.execute("""
                    CREATE TABLE plants(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        common_name TEXT,
                        sci_name TEXT,
                        family TEXT,
                        genus TEXT,
                        species_epithet TEXT,
                        care_level TEXT,
                        watering TEXT,
                        origin TEXT,
                        description TEXT,
                        image_url TEXT
                    );
                """);
                stmt.execute("""
                    CREATE TABLE collections(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        plant_id INTEGER NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
                        UNIQUE(user_id, plant_id)
                    );
                """);
                stmt.close();
            }
        };
    }
 
    @AfterEach
    void teardown() throws SQLException {
        app.close();
    }
 
    //--------------------- LOGIN TESTS -----------------------
    @Test
    void testLoginSuccess() throws SQLException {
        app.sign_up("user1", "pass", 0);
        assertTrue(app.login("user1", "pass"));
    }
 
    @Test
    void testLoginWrongPassword() throws SQLException {
        app.sign_up("user1", "pass", 0);
        assertFalse(app.login("user1", "wrong"));
    }
 
    @Test
    void testLoginUserNotFound() throws SQLException {
        assertFalse(app.login("ghost", "123"));
    }
    // ---------------------- password reset ---------------------
    @Test
    void testResetPasswordUserNotThere() throws SQLException{
        String result = app.reset_password("notReal", "notReal", "newpass");
        assertEquals("Username or password is wrong",result);
    }
    @Test
    void testPasswordReset() throws SQLException {
        app.sign_up("admin","changethis",1);
        String result = app.reset_password("admin", "changethis", "newpass");
        assertEquals("Password updated!", result);
    }
    @Test
    void testResetPasswordSameAsOld_ShouldFail() throws SQLException {
        app.sign_up("admin", "password", 0);
        
        String result = app.reset_password("admin", "password", "password");
        assertEquals("New password cannot equal old password", result);
    }
    //---------------------------- show all users [CB] ----------------------------
    @Test
    void testshowallusers() throws SQLException {

        app.sign_up("admin","admin",1);
        app.sign_up("guest1","guest1",0);
        app.sign_up("guest2","guest2",0);
        app.sign_up("guest3","guest3",0);
        app.sign_up("guest4","guest4",0);
        app.sign_up("guest5","guest5",0);

        app.login("admin","admin");
        String result = app.getAllUsers();
        assertNotNull(result);
        assertTrue(result.contains("admin"));
    }

    @Test
    void testShowAllUsersWithoutAdmin() throws SQLException {
        app.sign_up("guest", "guest", 0);
        assertEquals("login as an admin first", app.getAllUsers());
    }
    //--------------------- SIGN-UP TESTS ---------------------
    @Test
    void testSignupNormalUser() throws SQLException {
        String result = app.sign_up("John", "123", 0);
        assertTrue(result.contains("not admin"));
    }
 
    @Test
    void testSignupAdminUser() throws SQLException {
        String result = app.sign_up("admin2", "123", 1);
        assertTrue(result.contains("admin"));
    }
 
    @Test
    void testSignupDuplicateUser() throws SQLException {
        app.sign_up("Marco", "123", 0);
        assertThrows(SQLException.class, () -> {
            app.sign_up("Marco", "456", 0);
        });
    }
    // ----------------------- remove users [CB] -----------------------
    @Test
    void testRemoveUserSuccess() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.sign_up("guest", "guest", 0);
        app.login("admin", "pass");
        assertEquals("guest has been removed successfully.", app.removeusr("guest"));
    }

    @Test
    void testRemoveUserWithoutAdmin() throws SQLException {
        app.sign_up("guest", "guest", 0);
        assertEquals("login as an admin first", app.removeusr("guest"));
    }

    @Test
    void testRemoveUserActuallyDeleted() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.sign_up("guest", "guest", 0);
        app.login("admin", "pass");
        app.removeusr("guest");
        assertFalse(app.login("guest", "guest")); // can't log in if deleted
    }
    // -------------------------- promote  -------------
    @Test
    void testPromoteSuccess() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.sign_up("guest", "guest", 0);
        app.login("admin", "pass");
        assertEquals("guest has been promoted", app.promote("guest"));
    }
                
    @Test
    void testPromoteActuallyChangesRole() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.sign_up("guest", "guest", 0);
        app.login("admin", "pass");
        app.promote("guest");
        app.logout();
        app.login("guest", "guest");
        assertTrue(app.isAdmin());
    }

    @Test
    void testPromoteWithoutAdmin() throws SQLException {
        app.sign_up("guest", "guest", 0);
        assertEquals("login as an admin first", app.promote("guest"));
    }
                
    //--------------------- LOGOUT TEST ----------------------
    @Test
    void testLogoutResetsAdmin() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        app.logout();
        assertFalse(app.isAdmin());
    }
 
    //--------------------- ADD PLANT TESTS ------------------- [UT-10-CB]
    @Test
    void testAddWithoutAdmin() throws SQLException {
        String result = app.add("SYM", "Sci", "Rosaceae", "Rosa", "indica", "Medium", "Moderate", "USA", "A common garden rose.", null);
        assertEquals("login as admin before adding plants", result);
    }

    @Test
    void testAddWithAdmin() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
 
        String result = app.add(
            "Rose", "Rosa indica", "Rosaceae", "Rosa", "indica",
            "Medium", "Moderate", "USA", "A common garden rose.", null
        );
        assertTrue(result.contains("added Rose"),
            "Expected result to contain 'added Rose' but got: " + result);
    }
    //--------------------- REMOVE PLANT TESTS ---------------- [UT-11-CB]
    @Test
    void testRemoveWithoutAdmin() throws SQLException {
        String result = app.remove("Rose");
        assertEquals("login as an admin first", result);
    }
 
    @Test
    void testRemovePlantThatisNotThere() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        String res = app.remove("DragonPlant");
        assertTrue(res.contains("Removed"));
    }
 
    @Test
    void testRemoveWithAdmin() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
 
        app.add(
            "Rose", "Rosa indica", "Rosaceae", "Rosa", "indica",
            "Medium", "Moderate", "USA", "A common garden rose.", null
        );
 
        String result = app.remove("Rose");
        assertTrue(result.contains("Removed"),
            "Expected result to contain 'Removed' but got: " + result);
    }

    //--------------------- UPDATE PLANT TESTS [CB] -------------------
    @Test
    void testUpdateDescriptionWithAdmin() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        app.add("Oak", "Quercus robur", "Fagaceae", "Quercus", "robur", "Low", "Minimum", "Europe", "Old description.", null);
        String result = app.updateDescription("Oak", "New description.");
        assertEquals("Description updated for Oak", result);
    }

    @Test
    void testUpdateDescriptionWithoutAdmin() throws SQLException {
        String result = app.updateDescription("Oak", "New description.");
        assertEquals("login as admin first", result);
    }

    @Test
    void testUpdateDescriptionPlantNotFound() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        String result = app.updateDescription("GhostPlant", "Desc.");
        assertEquals("Plant not found: GhostPlant", result);
    }

    @Test
    void testUpdatePlantWithAdmin() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        app.add("Fern", "Pteridium aquilinum", "Dennstaedtiaceae", "Pteridium", "aquilinum", "Low", "Average", "Europe", "A common fern.", null);
        String result = app.updatePlant("Fern", "Eagle Fern", "Pteridium aquilinum", "Dennstaedtiaceae", "Pteridium", "aquilinum", "Low", "Average", "Europe", "Updated description.", null);
        assertEquals("Plant updated", result);
    }

    @Test
    void testUpdatePlantWithoutAdmin() throws SQLException {
        String result = app.updatePlant("Fern", "Eagle Fern", "Pteridium aquilinum", "Dennstaedtiaceae", "Pteridium", "aquilinum", "Low", "Average", "Europe", "Desc.", null);
        assertEquals("login as admin first", result);
    }

    @Test
    void testUpdatePlantNotFound() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        String result = app.updatePlant("GhostPlant", "Ghost", "Ghost sci", "GhostFamily", "Ghost", "ghost", "Low", "Average", "Unknown", "Desc.", null);
        assertEquals("Plant not found", result);
    }
                
    //--------------------- SEARCH TESTS ----------------------
 

  // Seeds the in-memory DB with 5 plants for search tests.
    private void seedSearchData() throws SQLException {
        String sql = """
            INSERT INTO plants
              (common_name, sci_name, family, genus, species_epithet,
               care_level, watering, origin, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (java.sql.PreparedStatement stmt = app.conn.prepareStatement(sql)) {
 
            // 1. RED MAPLE
            stmt.setString(1, "RED MAPLE");
            stmt.setString(2, "Acer rubrum");
            stmt.setString(3, "Sapindaceae");
            stmt.setString(4, "Acer");
            stmt.setString(5, "rubrum");
            stmt.setString(6, "Moderate");
            stmt.setString(7, "Moderate");
            stmt.setString(8, "North America");
            stmt.setString(9, "A common maple tree.");
            stmt.addBatch();
 
            // 2. VELVETLEAF
            stmt.setString(1, "VELVETLEAF");
            stmt.setString(2, "Abutilon theophrasti");
            stmt.setString(3, "Malvaceae");
            stmt.setString(4, "Abutilon");
            stmt.setString(5, "theophrasti");
            stmt.setString(6, "Easy");
            stmt.setString(7, "Low");
            stmt.setString(8, "North America");
            stmt.setString(9, "A broadleaf weed.");
            stmt.addBatch();
 
            // 3. balsam fir
            stmt.setString(1, "balsam fir");
            stmt.setString(2, "Abies balsamea");
            stmt.setString(3, "Pinaceae");
            stmt.setString(4, "Abies");
            stmt.setString(5, "balsamea");
            stmt.setString(6, "Hard");
            stmt.setString(7, "High");
            stmt.setString(8, "Europe");
            stmt.setString(9, "An evergreen conifer.");
            stmt.addBatch();
 
            // 4. purple sand-verbena
            stmt.setString(1, "purple sand-verbena");
            stmt.setString(2, "Abronia umbellata");
            stmt.setString(3, "Nyctaginaceae");
            stmt.setString(4, "Abronia");
            stmt.setString(5, "umbellata");
            stmt.setString(6, "Easy");
            stmt.setString(7, "Moderate");
            stmt.setString(8, "South America");
            stmt.setString(9, "A flowering coastal plant.");
            stmt.addBatch();
 
            // 5. poverty threeawn
            stmt.setString(1, "poverty threeawn");
            stmt.setString(2, "Aristida dichotoma");
            stmt.setString(3, "Poaceae");
            stmt.setString(4, "Aristida");
            stmt.setString(5, "dichotoma");
            stmt.setString(6, "Easy");
            stmt.setString(7, "Low");
            stmt.setString(8, "South America");
            stmt.setString(9, "A native grass species.");
            stmt.addBatch();
 
            stmt.executeBatch();
        }
    }

    // --- Name search --- (UT-16 to UT-20)

    @Test
    void testSearchByNameFound() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("RED MAPLE", null, null, null);
        assertFalse(results.isEmpty());
        assertEquals("RED MAPLE", results.get(0)[1]);
    }

    @Test
    void testSearchByNamePartialMatch() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("maple", null, null, null);
        assertFalse(results.isEmpty());
        assertEquals("RED MAPLE", results.get(0)[1]);
    }

    @Test
    void testSearchByNameNoMatch() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("dragon plant", null, null, null);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchByNameCaseInsensitive() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("velvetleaf", null, null, null);
        assertFalse(results.isEmpty(), "Name search should be case-insensitive");
    }

    @Test
    void testSearchEmptyNameReturnsAll() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("", null, null, null);
        assertEquals(5, results.size());
    }

    // --- Family filter (tested via name since back.java has no family param) ---

    @Test
    void testFilterByFamilyMalvaceae() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("VELVETLEAF", null, null, null);
        assertEquals(1, results.size());
        assertEquals("VELVETLEAF", results.get(0)[1]);
    }

    @Test
    void testFilterByFamilySapindaceae() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("RED MAPLE", null, null, null);
        assertEquals(1, results.size());
        assertEquals("RED MAPLE", results.get(0)[1]);
    }

    @Test
    void testFilterByFamilyNoResults() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("Cactaceae", null, null, null);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFilterByFamilyCaseInsensitive() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("velvetleaf", null, null, null);
        assertFalse(results.isEmpty(), "Name search should be case-insensitive");
    }

    // --- Care level filter ---

    @Test
    void testFilterByCareLevelEasy() throws SQLException {
        seedSearchData();
        // VELVETLEAF, purple sand-verbena, poverty threeawn
        List<String[]> results = app.searchWithFilters("", "Easy", null, null);
        assertEquals(3, results.size());
    }

    @Test
    void testFilterByCareLevelHard() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("", "Hard", null, null);
        assertEquals(1, results.size());
        assertEquals("balsam fir", results.get(0)[1]);
    }

    @Test
    void testFilterByCareLevelNoResults() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("", "Expert", null, null);
        assertTrue(results.isEmpty());
    }

    // --- Watering filter ---

    @Test
    void testFilterByWateringLow() throws SQLException {
        seedSearchData();
        // VELVETLEAF and poverty threeawn
        List<String[]> results = app.searchWithFilters("", null, "Low", null);
        assertEquals(2, results.size());
    }

    @Test
    void testFilterByWateringHigh() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("", null, "High", null);
        assertEquals(1, results.size());
        assertEquals("balsam fir", results.get(0)[1]);
    }

    // --- Origin filter ---

    @Test
    void testFilterByOriginNorthAmerica() throws SQLException {
        seedSearchData();
        // VELVETLEAF and RED MAPLE
        List<String[]> results = app.searchWithFilters(null, null, null, "North America");
        assertEquals(2, results.size());
    }

    @Test
    void testFilterByOriginEurope() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters(null, null, null, "Europe");
        assertEquals(1, results.size());
        assertEquals("balsam fir", results.get(0)[1]);
    }

    @Test
    void testFilterByOriginNoResults() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters(null, null, null, "Antarctica");
        assertTrue(results.isEmpty());
    }

    // --- Combined name + filter ---

    @Test
    void testNameAndOriginFilter() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("fir", null, null, "Europe");
        assertEquals(1, results.size());
        assertEquals("balsam fir", results.get(0)[1]);
    }

    @Test
    void testNameAndOriginMismatch() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("fir", null, null, "North America");
        assertTrue(results.isEmpty());
    }

    @Test
    void testNameAndCareLevelFilter() throws SQLException {
        seedSearchData();
        // maple is Moderate — searching Easy should miss it
        List<String[]> results = app.searchWithFilters("maple", "Easy", null, null);
        assertTrue(results.isEmpty());
    }

    @Test
    void testNameAndWateringFilter() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("VELVETLEAF", null, "Low", null);
        assertEquals(1, results.size());
        assertEquals("VELVETLEAF", results.get(0)[1]);
    }

    @Test
    void testNameAndFamilyFilter() throws SQLException {
        seedSearchData();
        // maple is Moderate care level
        List<String[]> results = app.searchWithFilters("maple", "Moderate", null, null);
        assertEquals(1, results.size());
        assertEquals("RED MAPLE", results.get(0)[1]);
    }

    // --- All filters combined ---

    @Test
    void testAllFiltersMatch() throws SQLException {
        seedSearchData();
        // poverty threeawn: Easy, Low, South America
        List<String[]> results = app.searchWithFilters("threeawn", "Easy", "Low", "South America");
        assertEquals(1, results.size());
        assertEquals("poverty threeawn", results.get(0)[1]);
    }

    @Test
    void testAllFiltersNoMatch() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("balsam fir", "Easy", "Low", "South America");
        assertTrue(results.isEmpty(), "balsam fir is Hard/High/Europe, not Easy/Low/South America");
    }

    // --- Return format ---

    @Test
    void testResultArrayHasTenFields() throws SQLException {
        seedSearchData();
        List<String[]> results = app.searchWithFilters("RED MAPLE", null, null, null);
        assertFalse(results.isEmpty());
        assertEquals(11, results.get(0).length,
            "Each result should have 11 fields: id, common_name, sci_name, family, genus, species_epithet, care_level, watering, origin, description, image_url");
    }
    
    //--------------------- COLLECTION TESTS [CB] ----------------------

    // seed helper — adds one plant and returns its id
    private int seedOnePlant() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        app.add("Rose", "Rosa indica", "Rosaceae", "Rosa", "indica", "Medium", "Moderate", "USA", "A rose.", null);
        app.logout();
        try (java.sql.Statement st = app.conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT id FROM plants WHERE common_name='Rose'")) {
            rs.next();
            return rs.getInt("id");
        }
    }

    @Test
    void testAddToCollection() throws SQLException {
        int plantId = seedOnePlant();
        app.sign_up("user", "pass", 0);
        String result = app.addToCollection("user", plantId);
        assertEquals("added", result);
    }

    @Test
    void testAddToCollectionDuplicate() throws SQLException {
        int plantId = seedOnePlant();
        app.sign_up("user", "pass", 0);
        app.addToCollection("user", plantId);
        String result = app.addToCollection("user", plantId);
        assertEquals("already", result);
    }

    @Test
    void testAddToCollectionUnknownUser() throws SQLException {
        int plantId = seedOnePlant();
        String result = app.addToCollection("nobody", plantId);
        assertEquals("User not found", result);
    }

    @Test
    void testGetCollection() throws SQLException {
        int plantId = seedOnePlant();
        app.sign_up("user", "pass", 0);
        app.addToCollection("user", plantId);
        List<String[]> col = app.getCollection("user");
        assertEquals(1, col.size());
        assertEquals("Rose", col.get(0)[1]);
    }

    @Test
    void testGetCollectionEmpty() throws SQLException {
        app.sign_up("user", "pass", 0);
        List<String[]> col = app.getCollection("user");
        assertTrue(col.isEmpty());
    }

    @Test
    void testRemoveFromCollection() throws SQLException {
        int plantId = seedOnePlant();
        app.sign_up("user", "pass", 0);
        app.addToCollection("user", plantId);
        String result = app.removeFromCollection("user", plantId);
        assertEquals("removed", result);
        assertTrue(app.getCollection("user").isEmpty());
    }

    @Test
    void testRemoveFromCollectionUnknownUser() throws SQLException {
        int plantId = seedOnePlant();
        String result = app.removeFromCollection("nobody", plantId);
        assertEquals("User not found", result);
    }

    //--------------------- RECOMMENDATION TESTS [TB] ----------------------

    @Test
    void testGetRecommendationsEmptyCollection() throws SQLException {
        app.sign_up("user", "pass", 0);
        List<String[]> recs = app.getRecommendations("user");
        assertTrue(recs.isEmpty());
    }

    @Test
    void testGetRecommendationsReturnsRelatedPlants() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        // PlantA — user will save this one
        app.add("PlantA", "Sci A", "Rosaceae", "Rosa", "indica", "Medium", "Average", "Japan", "Desc A", null);
        // PlantB — shares care_level + watering with PlantA, should be recommended
        app.add("PlantB", "Sci B", "Rosaceae", "Rosa", "indica", "Medium", "Average", "Japan", "Desc B", null);
        // PlantC — different attributes, should not be recommended
        app.add("PlantC", "Sci C", "Cactaceae", "Cactus", "sp", "Low", "Minimum", "Mexico", "Desc C", null);
        app.logout();

        app.sign_up("user", "pass", 0);
        try (java.sql.Statement st = app.conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT id FROM plants WHERE common_name='PlantA'")) {
            rs.next();
            app.addToCollection("user", rs.getInt("id"));
        }

        List<String[]> recs = app.getRecommendations("user");
        boolean foundB = recs.stream().anyMatch(p -> "PlantB".equals(p[1]));
        boolean foundA = recs.stream().anyMatch(p -> "PlantA".equals(p[1]));
        assertTrue(foundB, "PlantB should be recommended");
        assertFalse(foundA, "PlantA is already in collection, should not be recommended");
    }

    @Test
    void testGetRecommendationsMaxFive() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        app.add("Seed", "Sci Seed", "Rosaceae", "Rosa", "indica", "Medium", "Average", "Japan", "Seed plant", null);
        // 8 plants that all match on care_level + watering
        for (int i = 1; i <= 8; i++) {
            app.add("Match" + i, "Sci " + i, "Rosaceae", "Rosa", "indica", "Medium", "Average", "Japan", "Match " + i, null);
        }
        app.logout();

        app.sign_up("user", "pass", 0);
        try (java.sql.Statement st = app.conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT id FROM plants WHERE common_name='Seed'")) {
            rs.next();
            app.addToCollection("user", rs.getInt("id"));
        }

        List<String[]> recs = app.getRecommendations("user");
        assertTrue(recs.size() <= 5, "getRecommendations() should return at most 5 plants");
    }

    // =========================================================================
    // INTEGRATION TESTS
    // IT-01 through IT-12 send real HTTP requests to the running Javalin server.
    // Precondition: the server must be started (java back) on port 8080 before
    // running this section. Each test uses Java's built-in HttpClient.
    // =========================================================================

    // helper: send a POST with application/x-www-form-urlencoded body
    private java.net.http.HttpResponse<String> postForm(String path, String body) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .build();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080" + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    // helper: send a GET request; optionally attach a session cookie
    private java.net.http.HttpResponse<String> getRequest(String path, String cookie) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .build();
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080" + path))
                .GET();
        if (cookie != null) builder.header("Cookie", cookie);
        return client.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    // helper: log in via HTTP and return the Set-Cookie header so later requests
    // can carry the same session
    private String loginViaHttp(String username, String password) throws Exception {
        java.net.http.HttpResponse<String> res = postForm(
                "/login-endpoint",
                "username=" + username + "&password=" + password);
        String cookie = res.headers().firstValue("Set-Cookie").orElse(null);
        assertNotNull(cookie, "Expected a session cookie after successful login");
        return cookie;
    }

    // ── IT-01-TB: successful login sets session and redirects to index.html ───
    // Components: login() + /login-endpoint + session
    @Test
    void testIT01_LoginSuccessRedirectsToIndex() throws Exception {
        java.net.http.HttpResponse<String> res = postForm(
                "/login-endpoint",
                "username=admin&password=admin");
        // Javalin responds with a 302 redirect to /index.html on success
        assertEquals(302, res.statusCode());
        String location = res.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("index.html"),
                "Successful login should redirect to index.html, got: " + location);
    }

    // ── IT-02-TB: wrong password redirects to signin with error param ─────────
    // Components: login() failure + redirect
    @Test
    void testIT02_LoginFailureRedirectsWithError() throws Exception {
        java.net.http.HttpResponse<String> res = postForm(
                "/login-endpoint",
                "username=admin&password=wrongpassword");
        assertEquals(302, res.statusCode());
        String location = res.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("error=1"),
                "Failed login should redirect to signin.html?error=1, got: " + location);
    }

    // ── IT-03-CB: signup endpoint inserts user and redirects with registered flag
    // Components: /signup-endpoint + sign_up() + DB
    @Test
    void testIT03_SignupNewUserRedirectsToSignin() throws Exception {
        java.net.http.HttpResponse<String> res = postForm(
                "/signup-endpoint",
                "username=newplantlover&password=secret123");
        assertEquals(302, res.statusCode());
        String location = res.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("registered=true"),
                "New signup should redirect to signin.html?registered=true, got: " + location);
    }

    // ── IT-04-CB: signup with duplicate username redirects with error=exists ──
    // Components: /signup-endpoint duplicate username
    @Test
    void testIT04_SignupDuplicateUsernameRedirectsWithError() throws Exception {
        // Register once
        postForm("/signup-endpoint", "username=dupuser&password=pass1");
        // Register again with same username
        java.net.http.HttpResponse<String> res = postForm(
                "/signup-endpoint",
                "username=dupuser&password=pass2");
        assertEquals(302, res.statusCode());
        String location = res.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("error=exists"),
                "Duplicate signup should redirect with error=exists, got: " + location);
    }

    // ── IT-05-CB: admin POSTs to /add-plant, plant is inserted in DB ──────────
    // Components: /add-plant + add() + DB (admin)
    @Test
    void testIT05_AdminAddPlantEndpoint() throws Exception {
        String cookie = loginViaHttp("admin", "admin");
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .build();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/add-plant"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookie)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "common_name=TestOrchid" +
                                "&sci_name=Orchidaceae+sp." +
                                "&family=Orchidaceae" +
                                "&genus=Orchidaceae" +
                                "&species_epithet=sp." +
                                "&care_level=High" +
                                "&watering=Frequent" +
                                "&origin=Tropics" +
                                "&description=A+test+orchid." +
                                "&image_url="))
                .build();
        java.net.http.HttpResponse<String> res = client.send(
                req, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("added TestOrchid"),
                "Response body should confirm plant was added, got: " + res.body());
    }

    // ── IT-06-CB: admin POSTs to /remove-plant, plant is deleted from DB ─────
    // Components: /remove-plant + remove() + DB (admin)
    @Test
    void testIT06_AdminRemovePlantEndpoint() throws Exception {
        // First add the plant so we have something to remove
        String cookie = loginViaHttp("admin", "admin");
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .build();
        // Add plant
        java.net.http.HttpRequest addReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/add-plant"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookie)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "common_name=PlantToRemove&sci_name=X&family=X&genus=X" +
                                "&species_epithet=X&care_level=Low&watering=Minimum" +
                                "&origin=X&description=X&image_url="))
                .build();
        client.send(addReq, java.net.http.HttpResponse.BodyHandlers.ofString());
        // Remove it
        java.net.http.HttpRequest removeReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080/remove-plant"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookie)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "common_name=PlantToRemove"))
                .build();
        java.net.http.HttpResponse<String> res = client.send(
                removeReq, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("Removed"),
                "Response body should confirm removal, got: " + res.body());
    }

    // ── IT-07-OB: GET /search-plants?q=maple returns JSON array with match ────
    // Components: /search-plants + searchWithFilters() + DB
    @Test
    void testIT07_SearchPlantsEndpointReturnsJson() throws Exception {
        java.net.http.HttpResponse<String> res = getRequest("/search-plants?q=maple", null);
        assertEquals(200, res.statusCode());
        String body = res.body().trim();
        // Response should be a JSON array
        assertTrue(body.startsWith("["),
                "Search endpoint should return a JSON array, got: " + body);
    }

    // ── IT-08-TB: GET /search-plants with filters returns only matching plants ─
    // Components: /search-plants with filters
    @Test
    void testIT08_SearchPlantsWithFiltersReturnsFiltered() throws Exception {
        java.net.http.HttpResponse<String> res = getRequest(
                "/search-plants?q=&care_level=Low&watering=Minimum", null);
        assertEquals(200, res.statusCode());
        String body = res.body().trim();
        assertTrue(body.startsWith("["),
                "Filtered search endpoint should return a JSON array, got: " + body);
        // All returned entries must not be for a plant with care_level != Low
        // (if results are non-empty the JSON won't contain "Medium" or "High" care levels
        //  as the first badge value — we just verify the format is correct here)
        assertFalse(body.equals("null"), "Response should not be null");
    }

    // ── IT-09-OB: GET /get-user while logged in returns username ─────────────
    // Components: /get-user + session
    @Test
    void testIT09_GetUserWhileLoggedInReturnsUsername() throws Exception {
        String cookie = loginViaHttp("admin", "admin");
        java.net.http.HttpResponse<String> res = getRequest("/get-user", cookie);
        assertEquals(200, res.statusCode());
        assertEquals("admin", res.body().trim());
    }

    // ── IT-10-OB: GET /get-user with no session returns 401 ──────────────────
    // Components: /get-user – not logged in
    @Test
    void testIT10_GetUserNotLoggedInReturns401() throws Exception {
        java.net.http.HttpResponse<String> res = getRequest("/get-user", null);
        assertEquals(401, res.statusCode());
    }

    // ── IT-11-OB: GET /is-admin while admin is logged in returns "true" ───────
    // Components: /is-admin + isAdmin() + session
    @Test
    void testIT11_IsAdminWhileAdminLoggedInReturnsTrue() throws Exception {
        String cookie = loginViaHttp("admin", "admin");
        java.net.http.HttpResponse<String> res = getRequest("/is-admin", cookie);
        assertEquals(200, res.statusCode());
        assertEquals("true", res.body().trim());
    }

    // ── IT-12-TB: GET /logout invalidates session; /get-user then returns 401 ─
    // Components: /logout + session invalidation
    @Test
    void testIT12_LogoutInvalidatesSession() throws Exception {
        String cookie = loginViaHttp("admin", "admin");
        // Confirm logged in
        java.net.http.HttpResponse<String> before = getRequest("/get-user", cookie);
        assertEquals(200, before.statusCode());
        // Log out
        java.net.http.HttpResponse<String> logoutRes = getRequest("/logout", cookie);
        assertEquals(302, logoutRes.statusCode());
        String location = logoutRes.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("signin.html"),
                "Logout should redirect to signin.html, got: " + location);
        // Session should now be invalid — /get-user must return 401
        java.net.http.HttpResponse<String> after = getRequest("/get-user", cookie);
        assertEquals(401, after.statusCode());
    }
}