package com.example.clarity;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExpenseManagerActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private TextView tvTotalBalance;
    private RecyclerView rvTransactions;
    private FloatingActionButton fabAddTransaction;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;
    private DiaryDatabaseHelper dbHelper;
    private int userId = 1; // Replace with actual logged-in user ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_manager);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        userId = sharedPreferences.getInt("userId", -1);

        dbHelper = new DiaryDatabaseHelper(this);

        // Initialize UI Components
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        rvTransactions = findViewById(R.id.rvTransactions);
        fabAddTransaction = findViewById(R.id.fabAddTransaction);

        // Load transactions from database
        loadTransactions();

        // Set up FAB Click Listener
        fabAddTransaction.setOnClickListener(v -> addTransaction());

        // Date Filter Buttons
        Button btnToday = findViewById(R.id.btnToday);
        Button btnWeek = findViewById(R.id.btnWeek);
        Button btnMonth = findViewById(R.id.btnMonth);
        Button btnYear = findViewById(R.id.btnYear);

        btnToday.setOnClickListener(v -> filterTransactions("Today"));
        btnWeek.setOnClickListener(v -> filterTransactions("Week"));
        btnMonth.setOnClickListener(v -> filterTransactions("Month"));
        btnYear.setOnClickListener(v -> filterTransactions("Year"));

        updateTotalBalance();
    }

    private void loadTransactions() {
        // Load transactions from the database for the current user
        transactionList = dbHelper.getTransactionsForUser(userId);

        // Set up the RecyclerView adapter with the loaded transactions
        transactionAdapter = new TransactionAdapter(transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
    }

    private void addTransaction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etTitle = dialogView.findViewById(R.id.etTransactionTitle);
        EditText etAmount = dialogView.findViewById(R.id.etTransactionAmount);
        EditText etCategory = dialogView.findViewById(R.id.etTransactionCategory);
        EditText etDate = dialogView.findViewById(R.id.etTransactionDate);
        Switch switchExpense = dialogView.findViewById(R.id.switchExpense);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        etDate.setOnClickListener(v -> showDatePicker(etDate));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            boolean isExpense = switchExpense.isChecked();

            if (title.isEmpty() || amountStr.isEmpty() || category.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            Transaction newTransaction = new Transaction(0, userId, title, amount, category, date, isExpense);
            dbHelper.addTransaction(newTransaction, userId);

            // Debug: Check database count
            List<Transaction> debugList = dbHelper.getTransactionsForUser(userId);
            System.out.println("Total Transactions in DB: " + debugList.size());
            for (Transaction t : debugList) {
                System.out.println("Transaction: " + t.getTitle() + ", Amount: " + t.getAmount());
            }

            // Refresh RecyclerView
            transactionList.clear();
            transactionList.addAll(dbHelper.getTransactionsForUser(userId));
            transactionAdapter.notifyDataSetChanged();

            // Force UI update
            runOnUiThread(() -> {
                transactionAdapter.notifyDataSetChanged();
                rvTransactions.setAdapter(null);
                rvTransactions.setAdapter(transactionAdapter);
            });
            loadTransactions();
            updateTotalBalance();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateTotalBalance() {
        // Reload the transactions from the database
        transactionList = dbHelper.getTransactionsForUser(userId);

        double totalBalance = 0.0;
        for (Transaction t : transactionList) {
            // If the transaction is an expense, subtract it; otherwise, add it
            if (t.isExpense()) {
                totalBalance -= t.getAmount(); // Deduct expense
            } else {
                totalBalance += t.getAmount(); // Add income
            }
        }

        // Update the total balance on the UI
        tvTotalBalance.setText("Total Balance: $" + String.format("%.2f", totalBalance));
    }


    private void filterTransactions(String filterType) {
        // Placeholder for filtering logic
        System.out.println("Filtering transactions by: " + filterType);
    }

    private void showDatePicker(EditText etDate) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = year1 + "-" + (month1 + 1) + "-" + dayOfMonth;
                    etDate.setText(selectedDate);
                }, year, month, day);

        datePickerDialog.show();
    }



}
