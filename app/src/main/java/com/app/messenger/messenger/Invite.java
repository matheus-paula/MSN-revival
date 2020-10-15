package com.app.messenger.messenger;

public class Invite {
    private String fromUserId;
    private String toUserId;
    private String fromUserEmail;
    private String toUserEmail;

    Invite(){}

    Invite(String fromUserId, String toUserId,
                  String fromUserEmail, String toUserEmail) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.fromUserEmail = fromUserEmail;
        this.toUserEmail = toUserEmail;
    }

    /* getters */

    public String getFromUserEmail() {
        return fromUserEmail;
    }

    public String getToUserEmail() {
        return toUserEmail;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

}
