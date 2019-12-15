package com.example.playerandrecorder.EventBustModels;

public class EventBusShowToast {
    private String message;

    public EventBusShowToast(String msg) {
        this.message = msg;

    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
