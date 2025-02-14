package com.example.clarity;

public class DiaryPage {
    private int pageId;
    private String pageTitle;
    private String pageDate;

    // Constructor
    public DiaryPage(int pageId, String pageTitle, String pageDate) {
        this.pageId = pageId;
        this.pageTitle = pageTitle;
        this.pageDate = pageDate;
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

    // ToString method for debugging
    @Override
    public String toString() {
        return "DiaryPage{" +
                "pageId=" + pageId +
                ", pageTitle='" + pageTitle + '\'' +
                ", pageDate='" + pageDate + '\'' +
                '}';
    }
}
