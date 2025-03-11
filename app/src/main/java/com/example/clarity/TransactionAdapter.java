package com.example.clarity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList = new ArrayList<>();

    private Context context;

    private DiaryDatabaseHelper dbHelper;

    public TransactionAdapter(Context context, List<Transaction> transactionList, DiaryDatabaseHelper dbHelper) {
        this.context = context;
        this.transactionList = transactionList;
        this.dbHelper = dbHelper;
    }




    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder. tvTitle.setText(transaction.getTitle());
        holder.tvAmount.setText(String.format("$%.2f", transaction.getAmount()));
//        holder.tvDate.setText(transaction.getDate());
        holder.btnDelete.setTag(transaction);

        if (transaction.isExpense()) {
            holder.tvAmount.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.tvAmount.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
        }

        holder.itemView.setOnClickListener(v -> {
            // Print "Hello" when an item is clicked
            Log.d("RecyclerView", "Hello");

            showEditTransactionDialog(transaction);


        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }


    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAmount, tvDate;
        FloatingActionButton btnDelete;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTransactionDescription);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
//            tvDate = itemView.findViewById(R.id.tvTransactionDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private void showEditTransactionDialog(Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etTitle = dialogView.findViewById(R.id.etTransactionTitle);
        EditText etAmount = dialogView.findViewById(R.id.etTransactionAmount);
        EditText etCategory = dialogView.findViewById(R.id.etTransactionCategory);
        EditText etDate = dialogView.findViewById(R.id.etTransactionDate);
        Switch switchExpense = dialogView.findViewById(R.id.switchExpense);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Set the existing values in the dialog fields
        etTitle.setText(transaction.getTitle());
        etAmount.setText(String.format("%.2f", transaction.getAmount()));
        etCategory.setText(transaction.getCategory());
        etDate.setText(transaction.getDate());
        switchExpense.setChecked(transaction.isExpense());

        // Show the date picker when clicking on the date field
        etDate.setOnClickListener(v -> showDatePicker(etDate));



        // Handle cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Handle save button
        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String date = etDate.getText().toString().trim().split(" ")[0];
            Log.d("DateDebug",""+date);
            boolean isExpense = switchExpense.isChecked();

            if (title.isEmpty() || amountStr.isEmpty() || category.isEmpty() || date.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert date to the correct format (add time to it)
            String formattedDate = convertToDateWithTime(date);
            if (formattedDate == null) {
                Toast.makeText(context, "Invalid date format", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            // Update the transaction in the database
            transaction.setTitle(title);
            transaction.setAmount(amount);
            transaction.setCategory(category);
            transaction.setDate(formattedDate);
            transaction.setExpense(isExpense);

            dbHelper.updateTransaction(transaction); // Ensure dbHelper is initialized

            // Refresh the RecyclerView
            int position = transactionList.indexOf(transaction);
            if (position >= 0) {
                transactionList.set(position, transaction);
                notifyItemChanged(position);
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDatePicker(EditText etDate) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = year1 + "-" + (month1 + 1) + "-" + dayOfMonth;
                    etDate.setText(selectedDate);
                }, year, month, day);

        datePickerDialog.show();
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


}
