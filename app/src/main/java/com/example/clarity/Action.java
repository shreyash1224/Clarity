package com.example.clarity;

import android.view.View;

class Action {
    enum ActionType { ADD, DELETE }

    ActionType type;
    View view; // Stores the affected view
    int position; // Stores position in contentLayout

    public Action(ActionType type, View view, int position) {
        this.type = type;
        this.view = view;
        this.position = position;
    }
}
