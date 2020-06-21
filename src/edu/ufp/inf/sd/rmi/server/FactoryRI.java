package edu.ufp.inf.sd.rmi.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FactoryRI extends Remote {

    public SessionRI login(String uname, String pw) throws RemoteException;

    public boolean register(String uname, String pw) throws RemoteException;

}