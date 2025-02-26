package com.example.clarity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.Calendar;
import java.util.Locale;

public class TaskDialog {
    public interface TaskDialogListener {
        void onTaskSaved(String title, String startTime, String endTime, boolean isRecurring);
    }

    public static void showTaskDialog(Context context, TaskDialogListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.task_dialog, null);
        builder.setView(view);

        EditText editTaskTitle = view.findViewById(R.id.editTaskTitle);
        Button buttonStartTime = view.findViewById(R.id.buttonStartTime);
        Button buttonEndTime = view.findViewById(R.id.buttonEndTime);
        CheckBox checkRecurring = view.findViewById(R.id.checkRecurring);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);
        Button buttonSaveTask = view.findViewById(R.id.buttonSaveTask);

        final String[] startTime = {""};
        final String[] endTime = {""};

        buttonStartTime.setOnClickListener(v -> {
            showDateTimePicker(context, (date) -> {
                startTime[0] = date;
                buttonStartTime.setText(date);
            });
        });

        buttonEndTime.setOnClickListener(v -> {
            showDateTimePicker(context, (date) -> {
                endTime[0] = date;
                buttonEndTime.setText(date);
            });
        });

        AlertDialog dialog = builder.create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonSaveTask.setOnClickListener(v -> {
            String title = editTaskTitle.getText().toString().trim();
            boolean isRecurring = checkRecurring.isChecked();

            if (title.isEmpty() || startTime[0].isEmpty() || endTime[0].isEmpty()) {
                Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            listener.onTaskSaved(title, startTime[0], endTime[0], isRecurring);
            dialog.dismiss();
        });

        dialog.show();

    }

    private static void showDateTimePicker(Context context, OnDateSelectedListener listener) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(context, (timeView, hourOfDay, minute) -> {
                String dateTime = String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d",
                        year, month + 1, dayOfMonth, hourOfDay, minute);
                listener.onDateSelected(dateTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    interface OnDateSelectedListener {
        void onDateSelected(String date);
    }
}
