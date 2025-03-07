package com.example.clarity;

import android.app.DatePickerDialog;
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

    private TextView tvTotalBalance;
    private RecyclerView rvTransactions;
    private FloatingActionButton fabAddTransaction;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_manager);

        // Initialize UI Components
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        rvTransactions = findViewById(R.id.rvTransactions);
        fabAddTransaction = findViewById(R.id.fabAddTransaction);

        // Initialize RecyclerView
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);

        // Set up FAB Click Listener
        fabAddTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTransaction();
            }
        });

        // Date Filter Buttons
        Button btnToday = findViewById(R.id.btnToday);
        Button btnWeek = findViewById(R.id.btnWeek);
        Button btnMonth = findViewById(R.id.btnMonth);
        Button btnYear = findViewById(R.id.btnYear);

        btnToday.setOnClickListener(v -> filterTransactions("Today"));
        btnWeek.setOnClickListener(v -> filterTransactions("Week"));
        btnMonth.setOnClickListener(v -> filterTransactions("Month"));
        btnYear.setOnClickListener(v -> filterTransactions("Year"));
    }

    private void addTransaction() {
        // Create Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Initialize Dialog Views
        EditText etTitle = dialogView.findViewById(R.id.etTransactionTitle);
        EditText etAmount = dialogView.findViewById(R.id.etTransactionAmount);
        EditText etCategory = dialogView.findViewById(R.id.etTransactionCategory);
        EditText etDate = dialogView.findViewById(R.id.etTransactionDate);
        Switch switchExpense = dialogView.findViewById(R.id.switchExpense);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Date Picker
        etDate.setOnClickListener(v -> showDatePicker(etDate));

        // Cancel Button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Save Button
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
            if (isExpense) {
                amount = -amount;
            }

            // Create Transaction Object
            Transaction newTransaction = new Transaction(
                    transactionList.size() + 1,
                    title,
                    amount,
                    category,
                    date,
                    isExpense
            );

            // Add transaction and update UI
            transactionList.add(newTransaction);
            transactionAdapter.notifyItemInserted(transactionList.size() - 1);
            updateTotalBalance();

            // Close Dialog
            dialog.dismiss();
        });

        // Show Dialog
        dialog.show();
    }




    private void updateTotalBalance() {
        double total = 0.0;
        for (Transaction t : transactionList) {
            total += t.getAmount(); // No need to negate, expense is already negative
        }
        tvTotalBalance.setText("Total Balance: $" + String.format("%.2f", total));
    }




    private void filterTransactions(String filterType) {
        // Placeholder for filtering logic
        // For now, just log the selected filter
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


