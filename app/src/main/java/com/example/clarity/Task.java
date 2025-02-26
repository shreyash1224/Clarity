package com.example.clarity;

public class Task {
    private int taskId;
    private String taskTitle;
    private String startTime;
    private String endTime;
    private String recurring;
    private String completion;

    public Task(int taskId, String taskTitle, String startTime, String endTime, String recurring, String completion) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.startTime = startTime;
        this.endTime = endTime;
        this.recurring = recurring;
        this.completion = completion;
    }

    // Getters
    public int getTaskId() { return taskId; }
    public String getTaskTitle() { return taskTitle; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRecurring() { return recurring; }
    public String getCompletion() { return completion; }

    // Setters
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setRecurring(String recurring) { this.recurring = recurring; }
    public void setCompletion(String completion) { this.completion = completion; }
}
