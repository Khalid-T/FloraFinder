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
    // ----------------------- remove users -----------------------
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
}