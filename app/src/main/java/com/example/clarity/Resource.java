package com.example.clarity;

public class Resource {
    private int resourceId;  // Auto-incremented, initially set to -1
    private int pageId;
    private String type;
    private String content;
    private int order;

    // Constructor for new resources (before inserting into the database)
    public Resource(int pageId, String type, String content, int order) {
        this.resourceId = -1;  // Indicating that this is not yet stored
        this.pageId = pageId;
        this.type = type;
        this.content = content;
        this.order = order;
    }

    // Constructor for existing resources (retrieved from the database)
    public Resource(int resourceId, int pageId, String type, String content, int order) {
        this.resourceId = resourceId;
        this.pageId = pageId;
        this.type = type;
        this.content = content;
        this.order = order;
    }




    public int getResourceId() {
        return resourceId;
    }

    public int getPageId() {
        return pageId;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getOrder() {
        return order;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "resourceId=" + resourceId +
                ", pageId=" + pageId +
                ", type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", order=" + order +
                '}';
    }
}
