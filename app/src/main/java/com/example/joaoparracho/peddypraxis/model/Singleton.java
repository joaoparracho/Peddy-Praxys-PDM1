package com.example.joaoparracho.peddypraxis.model;

public class Singleton {
    private static final Singleton ourInstance = new Singleton();
    private Manager manager;
    private boolean showFinishBtn;
    private boolean fd;
    private boolean fenceBool;
    private boolean notWalkinBool;

    public boolean isShowFinishBtn() {
        return showFinishBtn;
    }
    public void setShowFinishBtn(boolean showFinishBtn) {
        this.showFinishBtn = showFinishBtn;
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
}