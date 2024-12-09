package com.example.wmpprojectfireaicamera;

public class FlameRecord {
    private String message;
    private String timestamp;

    public FlameRecord() {}

    public FlameRecord(String message, String timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
