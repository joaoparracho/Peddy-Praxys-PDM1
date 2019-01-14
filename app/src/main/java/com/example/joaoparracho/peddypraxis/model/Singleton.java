package com.example.joaoparracho.peddypraxis.model;

import android.util.Log;

import com.google.firebase.firestore.auth.User;

public class Singleton {
    private static final Singleton ourInstance = new Singleton();
    private Manager manager;
    private Utilizador currentUser;
    private boolean showFinishBtn;
    private boolean fd;
    private boolean fenceBool;
    private boolean walkingBool;
    private boolean bLibLoc;
    private boolean bInRotA;
    private boolean bInEsslei;
    private boolean bCreateFenceTime;
    private char inEdidicio = ' ';
    private int numTasksComplete;
    private long startTime = -1;
    private String activityKey = "patioKey";
    private boolean bStart = false;
    private boolean[] faltaEdificios = {true, true, true, true, true}; // A, B, C, D, E

    public Utilizador getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(Utilizador currentUser) {
        this.currentUser = currentUser;
    }

    private Singleton() {
        manager = new Manager();
    }

    public static Singleton getInstance() {
        return ourInstance;
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

    public boolean isWalkingBool() {
        return walkingBool;
    }

    public void setWalkingBool(boolean walkingBool) {
        this.walkingBool = walkingBool;
    }

    public boolean getFd() {
        return fd;
    }

    public void setFd(boolean fd) {
        this.fd = fd;
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public void restartVariables() {
        Log.d("xxxfences", "Reset variables");
        this.fd = false;
        this.showFinishBtn = false;
        this.bCreateFenceTime = false;
        this.numTasksComplete = 0;
        this.faltaEdificios = new boolean[]{true, true, true, true, true};
        this.startTime = -1;
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

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public boolean isbInEsslei() {
        return bInEsslei;
    }

    public void setbInEsslei(boolean bInEsslei) {
        this.bInEsslei = bInEsslei;
    }

    public boolean isbInRotA() {
        return bInRotA;
    }

    public void setbInRotA(boolean bInRotA) {
        this.bInRotA = bInRotA;
    }

    public char getInEdidicio() {
        return inEdidicio;
    }

    public void setInEdidicio(char inEdidicio) {
        this.inEdidicio = inEdidicio;
    }

    public boolean isbStart() {
        return bStart;
    }

    public void setbStart(boolean bStart) {
        this.bStart = bStart;
    }
}