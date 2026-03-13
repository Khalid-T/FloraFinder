# Plant Catalogue System

## Course

CSCI 2040U – Software Design and Analysis

## Team Members

* Sara 
* Clinton
* Fahad
* Kevin
* Khalid
---

## Project Overview

The Plant Catalogue System is a web application that lets administrators upload and manage plant information while users search and filter plants by characteristics (e.g., light, soil, region, price). The goal is to help people quickly locate plants suited to their needs without scrolling large catalogues.

## Dependencies

- slf4j-api-2.0.9.jar
- slf4j-simple-2.0.9.jar
- sqlite-jdbc-3.45.1.0.jar

---

## Core Features

### Admin Features

* Add and update plants (names, descriptions, photos)
* Remove or hide plants no longer available
* View basic user info for account support

### User Features

* Search plants by name or region
* Filter and sort plant results
* Login and logout functionality
* Wishlist system and recommendations (planned)

---

## Development Iterations

### Iteration Overview
- Iteration 1 (Labs 5–7): Core demo — admin uploads plants; users search/filter; basic login/logout; keep it responsive.
- Iteration 2 (Labs 8–10): UX and quality — confirmations, richer filters/sorting, remove/hide plants, password reset, UI polish, wishlists.
- Iteration 3 (Labs 11–12): Final delivery — recommendations, purchasing flow, wishlist improvements, final testing.

---

## Build & Run
```
mvn clean package
mvn exec:java -Dexec.mainClass="back"
```

---

## Technologies (Planned)

* Frontend: HTML / CSS / JavaScript
* Backend:  Java 
* Version Control: Git + GitHub
* Database: SQL (e.g., MySQL / SQLite)

---


## Current Status

Login/search baseline works; add/remove and logout are underway; later iteration features (password reset, purchasing, recommendations) are still pending.
