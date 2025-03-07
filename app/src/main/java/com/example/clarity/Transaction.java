package com.example.clarity;

public class Transaction {
    private int id;
    private String title;
    private double amount;
    private String category;
    private String date;
    private boolean isExpense;

    public Transaction(int id, String title, double amount, String category, String date, boolean isExpense) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.isExpense = isExpense;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public boolean isExpense() { return isExpense; }
    public void setExpense(boolean expense) { isExpense = expense; }



}
