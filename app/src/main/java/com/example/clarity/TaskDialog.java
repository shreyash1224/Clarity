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

        Calendar now = Calendar.getInstance();
        Calendar todayEnd = (Calendar) now.clone();
        todayEnd.set(Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(Calendar.MINUTE, 59);

        // Default Start & End Times
        final String[] startTime = {formatDateTime(now)};
        final String[] endTime = {formatDateTime(todayEnd)};

        buttonStartTime.setText(startTime[0]);
        buttonEndTime.setText(endTime[0]);

        buttonStartTime.setOnClickListener(v -> {
            showDateTimePicker(context, now, null, (date) -> {
                startTime[0] = date;
                buttonStartTime.setText(date);

                // Ensure End Time is after Start Time
                Calendar selectedStart = parseDateTime(date);
                Calendar selectedEnd = parseDateTime(endTime[0]);
                if (!selectedEnd.after(selectedStart)) {
                    selectedEnd = (Calendar) selectedStart.clone();
                    selectedEnd.add(Calendar.HOUR, 1); // Default to 1 hour after Start Time
                    endTime[0] = formatDateTime(selectedEnd);
                    buttonEndTime.setText(endTime[0]);
                }
            });
        });

        buttonEndTime.setOnClickListener(v -> {
            showDateTimePicker(context, now, parseDateTime(startTime[0]), (date) -> {
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

    private static void showDateTimePicker(Context context, Calendar minTime, Calendar minEndTime, OnDateSelectedListener listener) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(context, (timeView, hourOfDay, minute) -> {
                Calendar selectedTime = Calendar.getInstance();
                selectedTime.set(year, month, dayOfMonth, hourOfDay, minute);

                // Ensure start time is not before current time
                if (minTime != null && selectedTime.before(minTime)) {
                    Toast.makeText(context, "Start time cannot be in the past!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Ensure end time is after start time
                if (minEndTime != null && selectedTime.before(minEndTime)) {
                    Toast.makeText(context, "End time must be after start time!", Toast.LENGTH_SHORT).show();
                    return;
                }

                listener.onDateSelected(formatDateTime(selectedTime));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private static String formatDateTime(Calendar calendar) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
    }

    private static Calendar parseDateTime(String dateTime) {
        String[] parts = dateTime.split(" ");
        String[] dateParts = parts[0].split("-");
        String[] timeParts = parts[1].split(":");

        Calendar calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]) - 1,
                Integer.parseInt(dateParts[2]), Integer.parseInt(timeParts[0]),
                Integer.parseInt(timeParts[1]));

        return calendar;
    }

    interface OnDateSelectedListener {
        void onDateSelected(String date);
    }
}
