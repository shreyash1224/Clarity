package com.example.clarity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SwotFragment extends Fragment {

    private String type;
    private EditText editText;
    private TextView textView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_swot, container, false);
        editText = view.findViewById(R.id.editTextSwot);
        textView = view.findViewById(R.id.textViewSwot);

        if (getArguments() != null) {
            type = getArguments().getString("type");
            String content = getArguments().getString("content", "");
            textView.setText(type);
            editText.setText(content);  // ✅ Set content here to avoid delays
        }

        return view;
    }

    public String getContent() {
        return editText.getText().toString();
    }
}
