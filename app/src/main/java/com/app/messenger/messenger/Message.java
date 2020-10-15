package com.app.messenger.messenger;

public class Message {
    private String type;
    private String msg;
    private String date;
    private String msgToUserId;
    private String myUserId;
    private String myUserName;
    private boolean isMine;
    private boolean read;

    public Message() {
    }

    Message(String type, String msg, String date, String msgToUserId, String myUserId, boolean isMine, boolean read, String myUserName) {
        this.msg = msg;
        this.date = date;
        this.msgToUserId = msgToUserId;
        this.type = type;
        this.myUserId = myUserId;
        this.isMine = isMine;
        this.read = read;
        this.myUserName = myUserName;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setMine(boolean mine) {
        isMine = mine;
    }

    public String getMsgToUserId() {
        return msgToUserId;
    }

    public String getMyUserId() {
        return myUserId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMyUserName() {
        return myUserName;
    }

}
