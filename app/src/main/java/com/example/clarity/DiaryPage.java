package com.example.clarity;

import java.util.ArrayList;
import java.util.List;

public class DiaryPage {
    private int pageId;
    private String pageTitle;
    private String pageDate;
    private List<Resource> content; // Stores both text blocks and images

    // Constructor
    public DiaryPage(int pageId, String pageTitle, String pageDate) {
        this.pageId = pageId;
        this.pageTitle = pageTitle;
        this.pageDate = pageDate;
        this.content = new ArrayList<>();
    }

    public DiaryPage(int id, String title, String date, ArrayList<Resource> resources) {
        this.pageId = id;
        this.pageTitle = title;
        this.pageDate = date;
        this.content = resources;
    }

    public DiaryPage(int pageId, String title) {
        this.pageId = pageId;
        this.pageTitle = title;
        this.pageDate = "";
        this.content = new ArrayList<>();
    }

    // Getters
    public int getPageId() {
        return pageId;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getPageDate() {
        return pageDate;
    }

    public List<Resource> getContent() {
        return content;
    }

    // Setters
    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public void setPageDate(String pageDate) {
        this.pageDate = pageDate;
    }

    public void setContent(List<Resource> content) {
        if (content == null) {
            this.content = new ArrayList<>();
        } else {
            this.content = content;
        }
    }

    // Add a resource (text block or image)
    public void addResource(Resource resource) {
        if (resource != null) {
            // Ensure order is set correctly
            if (resource.getOrder() == 0) {
                resource.setOrder(content.size() + 1);
            }
            this.content.add(resource);
        }
    }

    // Remove a resource (text block or image)
    public void removeResource(int resourceId) {
        content.removeIf(resource -> resource.getResourceId() == resourceId);
    }

    // ToString method for debugging
    @Override
    public String toString() {
        return "DiaryPage{" +
                "pageId=" + pageId +
                ", pageTitle='" + pageTitle + '\'' +
                ", pageDate='" + pageDate + '\'' +
                ", content=" + content +
                '}';
    }
}
