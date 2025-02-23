package com.example.clarity;

public class Resource {
    private int resourceId;
    private int pageId;
    private String resourceType;
    private String resourceContent;
    private int resourceOrder;

    // Constructor
    public Resource(int pageId, String resourceType, String resourceContent, int resourceOrder) {
        this.pageId = pageId;
        this.resourceType = resourceType;
        this.resourceContent = resourceContent;
        this.resourceOrder = resourceOrder;
    }

    // Getter methods
    public int getResourceId() { return resourceId; }
    public int getPageId() { return pageId; }
    public String getResourceType() { return resourceType; }
    public String getResourceContent() { return resourceContent; }
    public int getResourceOrder() { return resourceOrder; }

    // Setter methods (if needed)
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }
    public void setPageId(int pageId) { this.pageId = pageId; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public void setResourceContent(String resourceContent) { this.resourceContent = resourceContent; }
    public void setResourceOrder(int resourceOrder) { this.resourceOrder = resourceOrder; }
}
