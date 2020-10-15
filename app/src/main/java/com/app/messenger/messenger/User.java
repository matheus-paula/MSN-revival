package com.app.messenger.messenger;

import java.util.List;

public class User {
    private String id;
    private String displayName;
    private String bio;
    private String photo;
    private String status;
    private String email;
    private List<String> contacts;

    public User() {
    }

    User(String id, String displayName, String bio, String photo, String status, String email, List<String> contacts) {
        this.id = id;
        this.displayName = displayName;
        this.bio = bio;
        this.photo = photo;
        this.status = status;
        this.email = email;
        this.contacts = contacts;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
