package ru.gb.lesson5;

public class BroadcastRequest extends AbstractRequest {
    public static final String TYPE = "BroadcastRequest";


    private String message;


    public BroadcastRequest() {
        setType(TYPE);
    }

    public BroadcastRequest(String message) {
        this();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
