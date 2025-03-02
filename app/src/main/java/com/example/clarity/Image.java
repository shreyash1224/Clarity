package com.example.clarity;

public class Image {
    private String imageUri; // Store URI as a string
    private String imageTitle = ""; // New attribute for image title

    // Existing constructor
    public Image(String imageUri) {
        this.imageUri = imageUri;
    }

    // New constructor with image title
    public Image(String imageUri, String imageTitle) {
        this.imageUri = imageUri;
        this.imageTitle = imageTitle;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getImageTitle() {
        return imageTitle;
    }

    public void setImageTitle(String imageTitle) {
        this.imageTitle = imageTitle;
    }
}
