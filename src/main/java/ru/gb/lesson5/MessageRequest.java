package ru.gb.lesson5;

public class MessageRequest extends AbstractRequest{
    private String recipient;
    private String author;
    private String message;
    public MessageRequest() {
        setType("MessageRequest");
    }

    public MessageRequest(String recipient, String author, String message) {
        this();
        this.recipient = recipient;
        this.author = author;
        this.message = message;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
