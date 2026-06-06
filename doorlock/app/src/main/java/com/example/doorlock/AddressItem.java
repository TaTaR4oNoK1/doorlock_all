package com.example.doorlock;

public class AddressItem {
    private int id;
    private String title;

    public AddressItem() {}

    public AddressItem(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}