package ru.gb.lesson5;

public class ListClientsRequest extends AbstractRequest{
    public static final String TYPE = "BroadcastMessage";

    public ListClientsRequest() {
        setType(TYPE);
    }
}
