# Sprint 6 Deliverable

## Test Admin Accounts

- bank.teller / password123

- bank.manager / password123

## New Features

1. **Admin Log In with Spring Security integration** - A bank teller or bank manager can log in and carry out instructions (created in the previous sprint) such as creating, updating and deleting customers and accounts.
2. **Customer Log In with Spring Security integration** - A customer can register themselves and log in. No customer functions have been implemented yet except the dashboard.
3. **Customer-Account Integration** - Customers registered will be added to the admin's list of customers. Customer created by the admin must be registered by the customer and then approved by admin as a "customer claim" to create a customer account.
4. **Customer Dashboard** - Customers can view their details (such as address, phone number, email) and the list of accounts connected to them.

## Business Outcomes

- **Reduce risk** - Spring Security and authentication services provides secure sessions and prohibited access (as a customer you cannot access admin pages and vice versa).
- **Improve user experience** - Customer and admin workflows are separated
- **Reduce stress** - No duplicate customers between customers registering themselves and an admin creating a customer
