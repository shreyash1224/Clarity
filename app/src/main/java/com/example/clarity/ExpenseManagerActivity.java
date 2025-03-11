package com.example.clarity;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        Button btnAll = findViewById(R.id.btnAll);

        btnToday.setOnClickListener(v -> {
            Toast.makeText(this, "Today", Toast.LENGTH_SHORT).show();
            // Get the current date in the required format (e.g., "YYYY-MM-DD")
            String currentDate = getCurrentDate();
            Log.d("Date", "Passing Date: " + currentDate);

            // Call loadTransactions() to filter by today's date
            loadTransactions(currentDate, currentDate);
        });

        btnWeek.setOnClickListener(v -> {
            Toast.makeText(this, "This Week", Toast.LENGTH_SHORT).show();
            // Get the current date and calculate the start and end dates of the current week
            String[] weekDates = getWeekDateRange();
            String startOfWeek = weekDates[0]; // Start date of the week
            String endOfWeek = weekDates[1];   // End date of the week
            Log.d("Date", "Week Start: " + startOfWeek + ", Week End: " + endOfWeek);

            // Call loadTransactions() to filter by this week's date range
            loadTransactions(startOfWeek, endOfWeek);
        });

        btnMonth.setOnClickListener(v -> {
            Toast.makeText(this, "This Month", Toast.LENGTH_SHORT).show();
            // Get the current date and calculate the start and end dates of the current month
            String[] monthDates = getMonthDateRange();
            String startOfMonth = monthDates[0]; // Start date of the month
            String endOfMonth = monthDates[1];   // End date of the month
            Log.d("Date", "Month Start: " + startOfMonth + ", Month End: " + endOfMonth);

            // Call loadTransactions() to filter by this month's date range
            loadTransactions(startOfMonth, endOfMonth);
        });

        btnYear.setOnClickListener(v -> {
            Toast.makeText(this, "This Year", Toast.LENGTH_SHORT).show();
            // Get the current date and calculate the start and end dates of the current year
            String[] yearDates = getYearDateRange();
            String startOfYear = yearDates[0]; // Start date of the year
            String endOfYear = yearDates[1];   // End date of the year
            Log.d("Date", "Year Start: " + startOfYear + ", Year End: " + endOfYear);

            // Call loadTransactions() to filter by this year's date range
            loadTransactions(startOfYear, endOfYear);
        });

        btnAll.setOnClickListener(v -> {
            loadTransactions();
        });




        updateTotalBalance();
    }

    private void loadTransactions() {
        // Load transactions from the database for the current user
        transactionList = dbHelper.getTransactionsForUser(userId);

        // Set up the RecyclerView adapter with the loaded transactions
        transactionAdapter = new TransactionAdapter(this,transactionList,dbHelper);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
    }

    private void loadTransactions(String startDate, String endDate) {
        // Load transactions from the database for the current user within the specified date range
        transactionList = dbHelper.getTransactionsForUserInDateRange(userId, startDate, endDate);

        // Set up the RecyclerView adapter with the filtered transactions
        transactionAdapter = new TransactionAdapter(this, transactionList,dbHelper);
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

            // Convert date to the correct format (add time to it)
            String formattedDate = convertToDateWithTime(date);
            if (formattedDate == null) {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            Transaction newTransaction = new Transaction(0, userId, title, amount, category, formattedDate, isExpense);
            dbHelper.addTransaction(newTransaction, userId);

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


    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String convertToDateWithTime(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            return outputFormat.format(parsedDate);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    // Get the start and end dates of the current week
    private String[] getWeekDateRange() {
        Calendar calendar = Calendar.getInstance();

        // Set the calendar to the first day of the week (Sunday, but it can be changed based on the locale)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

        // Get the start of the week (Sunday)
        String startOfWeek = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        // Get the end of the week (Saturday)
        calendar.add(Calendar.DAY_OF_WEEK, 6);  // Add 6 days to Sunday to get Saturday
        String endOfWeek = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        return new String[]{startOfWeek, endOfWeek};
    }



    // Get the start and end dates of the current month
    private String[] getMonthDateRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1); // Set to the first day of the month
        String startOfMonth = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        calendar.add(Calendar.MONTH, 1); // Add one month
        calendar.set(Calendar.DAY_OF_MONTH, 0); // Set to the last day of the previous month
        String endOfMonth = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        return new String[]{startOfMonth, endOfMonth};
    }

    // Get the start and end dates of the current year
    private String[] getYearDateRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.JANUARY); // Set to the first month of the year
        calendar.set(Calendar.DAY_OF_YEAR, 1); // Set to the first day of the year
        String startOfYear = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        calendar.add(Calendar.YEAR, 1); // Add one year
        calendar.set(Calendar.DAY_OF_YEAR, 0); // Set to the last day of the previous year
        String endOfYear = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        return new String[]{startOfYear, endOfYear};
    }

    public void deleteTransaction(View view) {
        // Retrieve the transaction associated with the clicked delete button
        Transaction transaction = (Transaction) view.getTag();
        int transactionId = transaction.getId();

        // Display a Toast with the transaction ID
        Toast.makeText(view.getContext(), "Deleting Transaction: " + transactionId, Toast.LENGTH_SHORT).show();

        dbHelper.deleteTransaction(transactionId);

        loadTransactions();
        updateTotalBalance();

    }

}
