package com.example.joaoparracho.peddypraxis.model;

import com.google.android.gms.tasks.Task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

public class Game implements Serializable {
    private String gameTitle;
    private String description;
    private String id;
    private int duration;
    private Calendar dataUltimaAlteracao;
    private String autor;
    private ArrayList<Task> tasks = new ArrayList<>();

    public Game(String title, String description, String authors, int duration) {
        this.gameTitle = title;
        this.description = description;
        this.duration = duration;
        this.autor = authors;
        this.id = UUID.randomUUID().toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        autor = autor;
    }

    public Calendar getDataUltimaAlteracao() {
        return dataUltimaAlteracao;
    }

    public void setDataUltimaAlteracao(Calendar dataUltimaAlteracao) {
        this.dataUltimaAlteracao = dataUltimaAlteracao;
    }

    public String getGametitle() {
        return gameTitle;
    }

    public void setGame_title(String title) {
        gameTitle = title;
    }
}
