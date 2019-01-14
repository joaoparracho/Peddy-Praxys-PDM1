package com.example.joaoparracho.peddypraxis.model;

import java.io.Serializable;

public class Utilizador implements Serializable,Comparable<Utilizador> {

    private String name;
    private String idade;
    private String email;
    private int numJogosInic;
    private int numJogosTerm;
    private long melhorTempo;

    public Utilizador(String name, String idade) {
        this.name = name;
        this.idade = idade;
    }
    public Utilizador() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdade() {
        return idade;
    }

    public void setIdade(String idade) {
        this.idade = idade;
    }

    public int getNumJogosInic() {
        return numJogosInic;
    }

    public void setNumJogosInic(int numJogosInic) {
        this.numJogosInic = numJogosInic;
    }

    public int getNumJogosTerm() {
        return numJogosTerm;
    }

    public void setNumJogosTerm(int numJogosTerm) {
        this.numJogosTerm = numJogosTerm;
    }

    public long getMelhorTempo() {
        return melhorTempo;
    }

    public void setMelhorTempo(long melhorTempo) {
        this.melhorTempo = melhorTempo;
    }

    @Override
    public int compareTo(Utilizador o) {
        if(melhorTempo==0||o.getMelhorTempo()==0){
            return -1;
        }
        if(melhorTempo>o.getMelhorTempo()){
            return 1;
        }
        else if(melhorTempo<o.getMelhorTempo()){
            return -1;
        }
        else {
            return 0;
        }
    }
}

