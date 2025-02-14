package com.example.clarity;

public class DiaryPage {
    private int pageId;
    private String pageTitle;
    private String pageDate;
    private String content;

    // Constructor
    public DiaryPage(int pageId, String pageTitle, String pageDate) {
        this.pageId = pageId;
        this.pageTitle = pageTitle;
        this.pageDate = pageDate;
    }

    public DiaryPage(int pageId, String pageTitle, String pageDate, String content) {
        this.pageId = pageId;
        this.pageTitle = pageTitle;
        this.pageDate = pageDate;
        this.content = content;
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


    public String getPagetContent() {
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

    public void setContent(String content) {
        this.content = content;
    }

    // ToString method for debugging
    @Override
    public String toString() {
        return "DiaryPage{" +
                "pageId=" + pageId +
                ", pageTitle='" + pageTitle + '\'' +
                ", pageDate='" + pageDate + '\'' +
                ", pageContent='" + content + '\'' +
                '}';
    }
}
