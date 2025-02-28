package com.example.clarity;


public class Page {
    private int pageId;
    private String title;

    public Page(int pageId, String title) {
        this.pageId = pageId;
        this.title = title;
    }

    public int getPageId() {
        return pageId;
    }

    public String getTitle() {
        return title;
    }
}

//Todo: Combine page adapter and page files
