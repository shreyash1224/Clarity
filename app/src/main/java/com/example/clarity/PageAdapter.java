package com.example.clarity;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;

public class PageAdapter extends ArrayAdapter<String> {
    private List<String> originalList;
    private List<String> filteredList;

    public PageAdapter(Context context, int resource, List<String> pages) {
        super(context, resource, pages);
        this.originalList = new ArrayList<>(pages);
        this.filteredList = new ArrayList<>(pages);
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public String getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<String> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(originalList);  // Show all pages if no search query
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (String item : originalList) {
                        if (item.toLowerCase().contains(filterPattern)) {
                            filtered.add(item); // Match any substring
                        }
                    }
                }
                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList.clear();
                filteredList.addAll((List<String>) results.values);
                notifyDataSetChanged();
            }
        };
    }
}
