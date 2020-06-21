package edu.ufp.inf.sd.rmi.server;

import edu.ufp.inf.sd.rmi.client.Worker;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


public class User implements Serializable {

    private String name;

    private String password;

    private int credits;


    public User(String name, String password) {
        this.name = name;
        this.password = password;
        this.credits = 100000; //começa com 50000 creditos
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }


}