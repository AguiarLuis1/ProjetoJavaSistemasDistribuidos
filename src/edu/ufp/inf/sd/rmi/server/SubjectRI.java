package edu.ufp.inf.sd.rmi.server;

import edu.ufp.inf.sd.rmi.client.ObserverRI;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

public interface SubjectRI extends Remote {

    public int getId() throws RemoteException;

    public boolean isPaused() throws RemoteException;

    public void attach(ObserverRI observerRI) throws RemoteException;

    public void detach(ObserverRI observerRI) throws RemoteException;

    public void notifyAllObservers() throws RemoteException;

    public void notifyAllObserversRemoveHash() throws RemoteException;

    public void setState(boolean state) throws RemoteException;

    public boolean getState() throws RemoteException;

    public Map<String, String> getResults() throws RemoteException;

    public void removeHash(String code) throws RemoteException;

    public void updateResults(String string, String code) throws RemoteException;

    public String getTASK_QUEUE_NAME() throws RemoteException;

    public ArrayList<String> getStrings() throws RemoteException;

    public ArrayList<String> getHashes() throws RemoteException;

    public File getCharSequence() throws RemoteException;




}