package com.example.joaoparracho.peddypraxis.model;

public class Singleton {
    private static final Singleton ourInstance = new Singleton();
    private Manager manager;
    private boolean fd=false;
    private int delayTimer;
    private boolean fenceBool;
    private boolean notWalkinBool;
    private boolean[] faltaEdificios = {true, true, true, true, true}; // A, B, C, D, E

    public int getDelayTimer() {
        return delayTimer;
    }
    public void setDelayTimer(int delayTimer) {
        this.delayTimer = delayTimer;
    }

    public boolean isFenceBool() {
        return fenceBool;
    }
    public void setFenceBool(boolean fenceBool) {
        this.fenceBool = fenceBool;
    }
    public boolean isNotWalkinBool() {
        return notWalkinBool;
    }
    public void setNotWalkinBool(boolean notWalkinBool) {
        this.notWalkinBool = notWalkinBool;
    }

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


    public boolean getFaltaEdificios(int i) {
        return faltaEdificios[i];
    }

    public void setFaltaEdificios(int i, boolean bool) {
        this.faltaEdificios[i] = bool;
    }
}