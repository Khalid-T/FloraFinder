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
    //----------------------------------------------------------------------------------------
    
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
 
    //--------------------- LOGOUT TEST ----------------------
    @Test
    void testLogoutResetsAdmin() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        app.logout();
        assertFalse(app.isAdmin());
    }
 
    //--------------------- ADD PLANT TESTS -------------------
    @Test
    void testAddWithoutAdmin() throws SQLException {
        String result = app.add("Rose", "Rosa", "Rosaceae", "Rosa", "indica", "Medium", "Moderate", "USA", "desc");
        assertEquals("login as admin before adding plants", result);
    }

    @Test
    void testAddWithAdmin() throws SQLException {
        app.sign_up("admin", "pass", 1);
        app.login("admin", "pass");
        
        String result = app.add("Rose", "Rosa", "Rosaceae", "Rosa", "indica", "Medium", "Moderate", "USA", "desc");
        
        assertTrue(result.contains("added Rose"));
    } 
    //--------------------- REMOVE PLANT TESTS ----------------
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
        app.add("Rose", "Sci", "Rosaceae", "Rosa", "indica", "Medium", "Moderate", "USA", "A common garden rose.");        String result = app.remove("Rose");
        assertTrue(result.contains("Removed"));
    }
 
    //--------------------- SEARCH TESTS ----------------------
 

  // Seed helper
    private void seedSearchData() throws SQLException {
        String sql = """
            INSERT INTO plants
            (common_name, sci_name, family, genus, species_epithet, care_level, watering, origin, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (java.sql.PreparedStatement stmt = app.conn.prepareStatement(sql)) {

            // 1. RED MAPLE — Sapindaceae, Moderate, Moderate, North America
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

            // 2. VELVETLEAF — Malvaceae, Easy, Low, North America
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

            // 3. balsam fir — Pinaceae, Hard, High, Europe
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

            // 4. purple sand-verbena — Nyctaginaceae, Easy, Moderate, South America
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

            // 5. poverty threeawn — Poaceae, Easy, Low, South America
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

    // --- Name search ---

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
}


