package com.example.joaoparracho.peddypraxis.model;

import java.io.Serializable;
import java.util.ArrayList;

public class Manager implements Serializable {
    private ArrayList<Game> games;

    public Manager() {
        this.games = new ArrayList<>();
    }

    public ArrayList<Game> getGames() {
        return games;
    }

    public void addGame(Game g) {
        if (containsGame(g.getId())) throw new RuntimeException("Cannot add Game;Game ID already exists.");
        games.add(g);
    }

    public boolean containsGame(String id) {
        for (Game g : games) if (g.getId().equals(id)) return true;
        return false;
    }

}
