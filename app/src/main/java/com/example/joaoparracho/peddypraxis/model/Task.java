package com.example.joaoparracho.peddypraxis.model;

import java.io.Serializable;

public class Task implements Serializable {
    private String id;
    private int order; //em que ordem e que deve ser executada
    private String tipo; //genero da task
    private String description;
    private int value_order; //pontos que vale a task
    private byte[] answer;

    public Task(String tipo, String description, int value_order, byte[] answer) {
        this.tipo = tipo;
        this.description = description;
        this.value_order = value_order;
        this.answer = answer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getValue_order() {
        return value_order;
    }

    public void setValue_order(int value_order) {
        this.value_order = value_order;
    }


    @Override
    public String toString() {
        return "ID: " + id + "; Description: " + description;
    }

    public byte[] getAnswer() {
        return answer;
    }

    public void setAnswer(byte[] answer) {
        this.answer = answer;
    }
}


