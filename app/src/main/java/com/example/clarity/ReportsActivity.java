package com.example.clarity;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;


import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import java.util.ArrayList;

public class ReportsActivity extends AppCompatActivity {

    private PieChart expenseChart;
    private BarChart tasksChart;
    private TextView totalPages, totalTasks, totalExpenses;
    private DiaryDatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // Initialize UI
        expenseChart = findViewById(R.id.expenseChart);
        tasksChart = findViewById(R.id.tasksChart);
        totalPages = findViewById(R.id.totalPages);
        totalTasks = findViewById(R.id.totalTasks);
        totalExpenses = findViewById(R.id.totalExpenses);

        // Initialize DB
        db = new DiaryDatabaseHelper(this);

        // Load Reports
        loadTaskReport();
        loadExpenseReport();
        loadPageReport();
    }

    private void loadTaskReport() {
        Cursor cursor = db.getAllTasks();
        int completed = 0;
        int pending = 0;

        if (cursor.moveToFirst()) {
            do {
                int completionIndex = cursor.getColumnIndex("completion");
                if (completionIndex != -1) {
                    String status = cursor.getString(completionIndex);
                    if (status.equals("Completed")) {
                        completed++;
                    } else {
                        pending++;
                    }
                }
            } while (cursor.moveToNext());
        }


        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, completed));
        entries.add(new BarEntry(1, pending));

        BarDataSet dataSet = new BarDataSet(entries, "Tasks Status");
        BarData data = new BarData(dataSet);
        tasksChart.setData(data);
        tasksChart.invalidate();

        totalTasks.setText("Total Tasks: " + (completed + pending));
    }

    private void loadExpenseReport() {
        Cursor cursor = db.getAllTransactions();
        float income = 0;
        float expense = 0;

        if (cursor.moveToFirst()) {
            do {
                int expenseIndex = cursor.getColumnIndex("isExpense");
                int amountIndex = cursor.getColumnIndex("amount");

                if (expenseIndex != -1 && amountIndex != -1) {
                    int isExpense = cursor.getInt(expenseIndex);
                    float amount = cursor.getFloat(amountIndex);

                    if (isExpense == 1) {
                        expense += amount;
                    } else {
                        income += amount;
                    }
                }
            } while (cursor.moveToNext());
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(income, "Income"));
        entries.add(new PieEntry(expense, "Expense"));

        PieDataSet dataSet = new PieDataSet(entries, "Income vs Expense");
        PieData data = new PieData(dataSet);
        expenseChart.setData(data);
        expenseChart.invalidate();

        totalExpenses.setText("Total Expenses: $" + expense);
    }

    private void loadPageReport() {
        int pageCount = db.getPageCount();
        totalPages.setText("Total Pages: " + pageCount);
    }
}
