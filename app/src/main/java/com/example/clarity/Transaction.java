package com.example.clarity;

public class Transaction {
    private int id;
    private int userId;  // NEW: User ID to associate transactions with users
    private String title;
    private double amount;
    private String category;
    private String date;
    private boolean isExpense;

    public Transaction(int id, int userId, String title, double amount, String category, String date, boolean isExpense) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.isExpense = isExpense;
    }

    // Getters and Setters
    public int getId() { return id; }
    public int getUserId() { return userId; }  // NEW: Getter for userId
    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public boolean isExpense() { return isExpense; }
}
