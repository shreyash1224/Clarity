package com.example.clarity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.List;

public class TaskAdapter extends BaseAdapter {
    private Context context;
    private List<Task> taskList;
    private OnTaskDeleteListener deleteListener;

    public TaskAdapter(Context context, List<Task> taskList, OnTaskDeleteListener deleteListener) {
        this.context = context;
        this.taskList = taskList;
        this.deleteListener = deleteListener;
    }

    public TaskAdapter(Context context, List<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
    }

    @Override
    public int getCount() {
        return taskList.size();
    }

    @Override
    public Object getItem(int position) {
        return taskList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.task_block, parent, false);
            holder = new ViewHolder();
            holder.taskTitle = convertView.findViewById(R.id.taskTitle);
            holder.taskTime = convertView.findViewById(R.id.taskTime);
            holder.taskRecurring = convertView.findViewById(R.id.taskRecurring);
            holder.taskCompletion = convertView.findViewById(R.id.taskCompletion);
            holder.deleteTaskResource = convertView.findViewById(R.id.deleteTaskResource);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Task task = taskList.get(position);
        holder.taskTitle.setText(task.getTaskTitle());
        holder.taskTime.setText(task.getStartTime() + " - " + task.getEndTime());
        holder.taskRecurring.setText("Recurring: " + (task.getRecurring().equals("Yes") ? "Yes" : "No"));
        holder.taskCompletion.setChecked(task.getCompletion().equals("Completed"));

        // Handle completion checkbox change
        holder.taskCompletion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompletion(isChecked ? "Completed" : "Pending");
            // TODO: Update task completion status in the database
        });

        // Handle delete button click
        holder.deleteTaskResource.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onTaskDelete(task);
            }
        });

        return convertView;
    }

    static class ViewHolder {
        TextView taskTitle, taskTime, taskRecurring;
        CheckBox taskCompletion;
        ImageButton deleteTaskResource;
    }

    public interface OnTaskDeleteListener {
        void onTaskDelete(Task task);
    }
}
