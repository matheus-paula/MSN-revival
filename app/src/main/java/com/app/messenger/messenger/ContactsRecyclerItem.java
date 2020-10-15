package com.app.messenger.messenger;

public class ContactsRecyclerItem {
    private String contactName;
    private String photo;
    private String status;
    private String bio;
    private String email;
    private int notifications;
    private String contactId;

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    ContactsRecyclerItem(String contactId, String cName, String photo,String bio, String status, String email, int notifications) {
        this.contactId = contactId;
        this.contactName = cName;
        this.photo = photo;
        this.status = status;
        this.bio = bio;
        this.notifications = notifications;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getNotifications() {
        return notifications;
    }

    public void setNotifications(int notifications) {
        this.notifications = notifications;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
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

    public String getContactId() {
        return contactId;
    }

}
