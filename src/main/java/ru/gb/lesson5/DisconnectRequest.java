package ru.gb.lesson5;

public class DisconnectRequest extends AbstractRequest{
    public static final String TYPE = "DisconnectRequest";


    public DisconnectRequest() {
        setType(TYPE);
    }
}
