package com.example.clarity;

import android.view.View;

public class Action {
    public enum ActionType { ADD, DELETE }

    public ActionType type;
    public View view;
    public int position;
    public UndoRedoHandler undoRedoHandler;

    // ✅ Constructor WITHOUT UndoRedoHandler (for simple tasks)
    public Action(ActionType type, View view, int position) {
        this.type = type;
        this.view = view;
        this.position = position;
        this.undoRedoHandler = null;
    }

    // ✅ Constructor WITH UndoRedoHandler (for tasks needing undo/redo)
    public Action(ActionType type, View view, int position, UndoRedoHandler undoRedoHandler) {
        this.type = type;
        this.view = view;
        this.position = position;
        this.undoRedoHandler = undoRedoHandler;
    }

    public interface UndoRedoHandler {
        void undo();
        void redo();
    }
}
