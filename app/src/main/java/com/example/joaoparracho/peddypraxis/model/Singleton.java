package com.example.joaoparracho.peddypraxis.model;

import android.util.Log;

public class Singleton {
    private static final Singleton ourInstance = new Singleton();
    private Manager manager;
    private boolean showFinishBtn;
    private boolean fd;
    private boolean fenceBool;
    private boolean notWalkinBool;
    private boolean bLibLoc;
    private boolean bInPatio;
    private boolean bCreateFenceTime;
    private int numTasksComplete;
    private String activityKey="patioKey";



    private boolean[] faltaEdificios = {true, true, true, true, true}; // A, B, C, D, E

    public boolean isbInPatio() {
        return bInPatio;
    }
    public void setbInPatio(boolean bInPatio) {
        this.bInPatio = bInPatio;
    }

    public boolean isbLibLoc() {
        return bLibLoc;
    }
    public void setbLibLoc(boolean bLibLoc) {
        this.bLibLoc = bLibLoc;
    }

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

    public void restartVariables(){
        Log.d("xxxfences","Reset variables");
        Singleton.getInstance().setFd(false);
        Singleton.getInstance().setShowFinishBtn(false);
        Singleton.getInstance().setbCreateFenceTime(false);
        Singleton.getInstance().setNumTasksComplete(0);
        this.faltaEdificios = new boolean[] {true, true, true, true, true};
    }
    public String getActivityKey() {
        return activityKey;
    }
    public void setActivityKey(String activityKey) {
        this.activityKey = activityKey;
    }

    public boolean isbCreateFenceTime() {
        return bCreateFenceTime;
    }

    public void setbCreateFenceTime(boolean bCreateFenceTime) {
        this.bCreateFenceTime = bCreateFenceTime;
    }


    public boolean getFaltaEdificios(int i) {
        return faltaEdificios[i];
    }

    public void setFaltaEdificios(int i, boolean bool) {
        this.faltaEdificios[i] = bool;
    }

    public int getNumTasksComplete() {
        return numTasksComplete;
    }
    public void setNumTasksComplete(int numTasksComplete) {
        this.numTasksComplete = numTasksComplete;
    }
}