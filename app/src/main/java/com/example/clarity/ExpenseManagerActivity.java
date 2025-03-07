package com.example.clarity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
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
        // Placeholder: Open Add Transaction Dialog
    }

    private void filterTransactions(String filterType) {
        // Placeholder: Filter transactions based on date
    }
}
