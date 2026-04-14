# FloraFinder — Plant Catalogue System

## Course

CSCI 2040U – Software Design and Analysis

## Team Members

- Sara
- Clinton
- Fahad
- Kevin
- Khalid

---

## Project Overview

FloraFinder is a full-stack web application for browsing, searching, and managing a botanical plant database. Users can register, sign in, search and filter plants, save favourites to a personal collection, and receive plant recommendations. Administrators have access to an inline management dashboard for adding, removing, and editing plant records and user accounts.

---

## Features

### User Features

- **Registration & Login** — Sign up for an account and sign in with username and password.
- **Password Reset** — Reset your password by providing your current credentials and a new one.
- **Plant Search** — Search by common name or scientific name with partial and case-insensitive matching.
- **Smart Filtering** — Filter results by family, care level, watering needs, and origin.
- **Plant Collection (Wishlist)** — Save plants to a personal collection and remove them at any time.
- **Recommendations** — Receive automated plant suggestions based on your collection history.

### Admin Features

- **Add / Remove Plants** — Insert new plant records or delete existing ones from the database.
- **Edit Plant Details** — Update a plant's description or full record inline.
- **Upload Plant Images** — Replace a plant's photo through the admin panel.
- **User Management** — View all registered users, remove accounts, and promote users to admin.

---

## Prerequisites

- JDK 17+
- Maven 3.9+

Project dependencies are managed via `pom.xml` and resolved automatically by Maven.

---

## Build & Run

### Option 1: Maven (recommended)

```bash
git clone https://github.com/Khalid-T/FloraFinder
cd FloraFinder
mvn clean compile exec:java
```

Then open your browser at:

```
http://localhost:8080/signin.html
```

Press `Ctrl+C` to stop the server.

### Option 2: VS Code

1. Open the project folder in VS Code.
2. Navigate to `src/main/java/back.java`.
3. Click **Run** above the `main` method (or press `F5`).
4. Open `http://localhost:8080/signin.html` in your browser.

### Option 3: Windows batch script

Double-click `build_project.bat` in the project root. It will compile the project, copy dependencies into `lib/`, and start the server on port 8080.

> **Note:** `build_project.bat` copies dependencies on the first run only — subsequent runs skip that step automatically.

---

## Running Tests

```bash
mvn test
```

The test suite contains 86 tests across three levels:

| Type              | Count | Status                                                          |
| ----------------- | ----- | --------------------------------------------------------------- |
| Unit tests        | 64    | Automated — pass with no server required                        |
| Integration tests | 12    | Require the server to be running on port 8080 before `mvn test` |
| System tests      | 10    | `@Disabled` — executed manually through the browser             |

To run integration tests, start the server first (`mvn clean compile exec:java` in one terminal), then run `mvn test` in a second terminal.

---

## Technologies

- **Frontend:** HTML, CSS, JavaScript
- **Backend:** Java (Javalin framework)
- **Database:** SQLite (via JDBC)
- **Testing:** JUnit 5, in-memory SQLite for unit tests, Java HttpClient for integration tests
- **Build:** Maven
- **Version Control:** Git + GitHub

---

### Documentation

[Developer Documentation](developer_documentation.md)

[User Guide](user_guide.md)
