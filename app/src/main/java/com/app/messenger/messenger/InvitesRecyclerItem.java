package com.app.messenger.messenger;

public class InvitesRecyclerItem {
    private String contactName;
    private String photo;
    private String status;
    private String bio;
    private String contactId;
    private String inviteId;


    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    InvitesRecyclerItem(String contactId, String cName,
                               String photo, String bio, String status, String inviteId) {
        this.contactId = contactId;
        this.contactName = cName;
        this.photo = photo;
        this.status = status;
        this.bio = bio;
        this.inviteId = inviteId;
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

    public String getInviteId() {
        return inviteId;
    }

}
