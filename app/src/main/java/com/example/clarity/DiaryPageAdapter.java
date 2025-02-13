package com.example.clarity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

public class DiaryPageAdapter extends ArrayAdapter<DiaryPage> {
    private Context context;
    private ArrayList<DiaryPage> pages;

    public DiaryPageAdapter(Context context, ArrayList<DiaryPage> pages) {
        super(context, R.layout.list_item_diary_page, pages);
        this.context = context;
        this.pages = pages;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.list_item_diary_page, parent, false);
        }

        // Get data for this position
        DiaryPage page = pages.get(position);

        // Find TextViews in list_item_diary_page.xml
        TextView titleView = convertView.findViewById(R.id.tvDiplPageTitle);
        TextView dateView = convertView.findViewById(R.id.tvDiplPageDate);

        // Set text
        titleView.setText(page.getPageTitle());
        dateView.setText(page.getPageDate());

        return convertView;
    }
}
