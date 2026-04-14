# User Guide - FloraFinder

## Intro

FloraFinder is an online plant and flower catalogue designed to help you explore, search, and filter a database of botanical entries. Whether you are a hobbyist gardener, a nursery professional, or simply curious about plants, this program gives you fast access to plant information of over 100 plants; including scientific names, native regions, light and water requirements, and detailed descriptions.

The application has two types of accounts:

- **User account** - can browse, search, and filter the plant catalogue.
- **Admin account** - has all user features, and the ability to add and remove plants from the catalogue and promote other users.

---

## Getting Started

### Creating an Account

1. Run the application and navigate to the Sign Up page.
2. Enter a username and password of your choice.
3. Click **Sign Up**. You will be redirected to the Sign In page once registration is complete.

### Signing In

1. Go to the Sign In page.
2. Enter your username and password.
3. Click **Sign In**. You will be taken to the main catalogue page.

### Signing Out

Click the **Logout** link in the navigation bar at the top of the page. You will be returned to the Sign In page and your session will be cleared.

### Resetting Your Password

1. Navigate to the password reset section of the site.
2. Enter your username, your current password, and your new password.
3. Submit the form. If successful, sign in again with your new password.

---

## Browsing the Catalogue

After signing in, you will land on the main catalogue page. This is where you can explore all available plant entries. Each plant card displays the plant's common name, scientific name, native region, and key care information.

---

## Searching for Plants

Use the search bar at the top of the catalogue page to find plants by name.

1. Type part or all of a plant's common name into the search bar (e.g., typing "maple" will return RED MAPLE and any other maple entries).
2. Click the **Explore** button or press **Enter**.
3. Matching plant cards will appear below. If no plants match your search, a message will indicate that no results were found.

---

## Filtering the Catalogue

Filters let you narrow down results to plants that fit specific criteria. You can apply any combination of the following filters:

| Filter            | What it does                                  | Example values          |
| ----------------- | --------------------------------------------- | ----------------------- |
| Country           | Shows only plants native to a specific region | Japan, Brazil           |
| Light Requirement | Filters by how much sunlight a plant needs    | Full Sun, Partial Shade |
| Water Requirement | Filters by watering needs                     | Low, Moderate, High     |
| Plant Type        | Filters by plant category                     | Tree, Herb, Shrub       |

To apply filters:

1. Open the Filters panel on the catalogue page.
2. Select the values you want from the available filter options.
3. Filter chips will appear to show your active selections - click the **X** on a chip to remove that filter.
4. Click **Clear Filters** to remove all active filters at once.

---

## Admin Features

If you are signed in with an administrator account, you will see an **Admin Tools** button on the catalogue page. Regular user accounts do not have access to these features.

### Adding a Plant

1. Click **Admin Tools** to open the admin panel.
2. Fill in all required plant fields: Symbol, Scientific Name, Common Name, Region/State, Light Requirement, Water Requirement, Plant Type, and Description.
3. Click **Add Plant**. A confirmation message will appear if the plant was added successfully.
4. The new plant will immediately be searchable in the catalogue.

### Removing a Plant

1. Click **Admin Tools** to open the admin panel.
2. Enter the exact common name of the plant you wish to remove.
3. Click **Remove Plant**. A confirmation message will appear.
4. The plant will no longer appear in search results.

---

## Features Delivered | Iteration Summary

FloraFinder was developed across weekly deliverable sprints. The following table summarizes what was delivered in each iteration:

| Iteration   | Features Delivered                                                                                                                                          |
| ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Iteration 1 | User login/logout, admin login, add plants to catalogue, search by plant name, basic filtering by region, performance baseline established                  |
| Iteration 2 | Remove plants (admin), advanced filters (light, water, plant type), password reset, admin support tools, confirmation messages, significant UI improvements |
| Iteration 3 | Wishlist, plant recommendations, sorting, update plant descriptions/photos, purchasing system, final testing and polish                                     |

---

## Frequently Asked Questions

**I forgot my password. What do I do?**

Use the password reset feature. You will need to know your current password to set a new one. If you are completely locked out, contact an administrator.

**Why can't I see the Admin Tools button?**

Admin Tools are only visible when you are logged in with an administrator account. Regular user accounts do not have access to this panel.

**I searched for a plant but got no results. Why?**

Try using a shorter or simpler search term, for example, "maple" instead of "Red Maple Tree". Search matches on the common name field. Also check that no conflicting filters are active by clicking **Clear Filters**.

**Can I save plants I am interested in?**

A wishlist feature is implemented. It allows you to save and manage favourite plants directly from your account.

**How many plants are in the catalogue?**

The catalogue currently has ~105 plants.

---

## Demo

[View Demo (youtube)](https://www.youtube.com/watch?v=LeQyZTU1WAY)
