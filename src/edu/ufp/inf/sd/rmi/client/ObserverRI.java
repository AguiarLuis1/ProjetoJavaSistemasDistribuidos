package edu.ufp.inf.sd.rmi.client;

import edu.ufp.inf.sd.rmi.server.SubjectRI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ObserverRI extends Remote {

    public void update(boolean taskState) throws RemoteException;


    public void updateRemoveHash(ArrayList<String> taskHashes) throws RemoteException;

    public SubjectRI getSubject() throws RemoteException;

    public void setSubject(SubjectRI subject) throws RemoteException;

    public void startThread() throws RemoteException;

    public void stopThread() throws RemoteException;



}