package edu.ufp.inf.sd.rmi.server;

import edu.ufp.inf.sd.rmi.client.ObserverRI;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SessionRI extends Remote{

    public void logout() throws RemoteException;

    public String listTaskGroups() throws RemoteException;

    public void deleteTaskGroup(int idTask) throws RemoteException;

    public SubjectRI createTaskGroup(String hashAlgorithm, boolean type, File charSequence, int subCharSize, File hashCodesSet) throws RemoteException;

    public ObserverRI createWorker() throws RemoteException;

    public void assocWorker(ObserverRI worker, int taskId) throws RemoteException;

    public void deassocWorker(ObserverRI worker, int taskId) throws RemoteException;

    public User getUser() throws RemoteException;

    public void pauseTaskGroup(int idTask) throws RemoteException;

    public void resumeTaskGroup(int idTask) throws RemoteException;
}