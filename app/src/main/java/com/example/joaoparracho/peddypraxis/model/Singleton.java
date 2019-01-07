package com.example.joaoparracho.peddypraxis.model;

public class Singleton {
    private static final Singleton ourInstance = new Singleton();
    private Manager manager;
    private boolean fd;

    public boolean getFd() {
        return fd;
    }
    public void setFd(boolean fd) {
        this.fd = fd;
    }

    private Singleton() {
        manager = new Manager();
    }

    public static Singleton getInstance() {
        return ourInstance;
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public boolean isNumeric(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            // s is not numeric
            return false;
        }
    }
}