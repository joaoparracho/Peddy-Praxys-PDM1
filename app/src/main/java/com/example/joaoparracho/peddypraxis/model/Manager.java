package com.example.joaoparracho.peddypraxis.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

public class Manager implements Serializable {
    private ArrayList<Game> games;
    private ArrayList<Task> tasks;

    public Manager() {
        this.games = new ArrayList<>();
        this.tasks = new ArrayList<>();
    }

    public ArrayList<Game> getGames() {
        return games;
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    //=============================================================//
    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }

    //=====================GAME====================================//
    public void addGame(Game g) {
        if (containsGame(g.getId())) {
            throw new RuntimeException("Cannot add Game;Game ID already exists.");
        }
        games.add(g);
    }

    public boolean containsGame(String id) {
        for (Game g : games) {
            if (g.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean changeGame(Game g1, Game g2) {
        Calendar data_atual = Calendar.getInstance();
        for (Game g : games) {
            if (g.getId().equals(g1.getId())) {
                g.setDescription(g2.getDescription());
                g.setDataUltimaAlteracao(data_atual);
                g.setAutor(g2.getAutor());
                g.setGame_title(g2.getGametitle());
                g.setDuration(g2.getDuration());
                return true;
            }
        }
        return false;
    }

    public boolean deleteGame(Game g1) {
        for (int i = 0; i < games.size(); i++) {
            if (games.get(i).getId().equals(g1.getId())) {

                games.remove(i);
                return true;
            }
        }
        return false;
    }

    public ArrayList<Game> searchGameById(String id) {
        ArrayList<Game> res = new ArrayList<>();
        for (Game c : games) {
            if (c.getId().contains(id)) {
                res.add(c);
            }
        }
        return res;
    }

    public ArrayList<Game> searchGameByGame_title(String game_title) {
        ArrayList<Game> res = new ArrayList<>();
        for (Game c : games) {
            if (c.getGametitle().contains(game_title)) {
                res.add(c);
            }
        }
        return res;
    }

    //=====================TASK====================================//
    public void addTask(Task t) {
        tasks.add(t);
    }

    public void multiple_addTask(ArrayList<Task> t) {
        for (int i = 0; i < t.size(); i++) {
            addTask(t.get(i));
        }
    }

    public boolean containsTask(String id) {
        for (Task t : tasks) {
            if (t.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean changeTask(Task t1, Task t2) {
        for (Task t : tasks) {
            if (t.getId().equals(t1.getId())) {
                t.setDescription(t2.getDescription());
                t.setOrder(t2.getOrder());
                t.setValue_order(t2.getValue_order());
                t.setTipo(t2.getTipo());
                return true;
            }
        }
        return false;
    }

    public boolean deleteTask(Task t1) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getTipo().equals(t1.getTipo())) {
                tasks.remove(i);
                return true;
            }
        }
        return false;
    }

    public ArrayList<Task> searchTasktById(String id) {
        ArrayList<Task> res = new ArrayList<>();
        for (Task c : tasks) {
            if (c.getId().contains(id)) {
                res.add(c);
            }
        }
        return res;
    }

    public ArrayList<Task> searchContactByTipo(String tipo) {
        ArrayList<Task> res = new ArrayList<>();
        for (Task c : tasks) {
            if (c.getTipo().contains(tipo)) {
                res.add(c);
            }
        }
        return res;
    }
}
