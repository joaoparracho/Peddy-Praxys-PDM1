package com.example.joaoparracho.peddypraxis.model;

import java.io.Serializable;
import java.util.UUID;

public class Game implements Serializable {
    private String gameTitle;
    private String description;
    private String id;
    private int duration;
    private String autor;

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

    public String getAutor() {
        return autor;
    }

    public String getGametitle() {
        return gameTitle;
    }
}
