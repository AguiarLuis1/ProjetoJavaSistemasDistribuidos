package edu.ufp.inf.sd.rmi.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;


public class FactoryImpl extends UnicastRemoteObject implements FactoryRI {

    //String - username | SessionRI - Session respetiva
    private HashMap<String, SessionRI> sessions;

    private DB db;


    public FactoryImpl() throws RemoteException {
        // Invoca UnicastRemoteObject constructor que exporta um objeto remoto
        super();
        //atribui a mesma db para todas as factory's
        this.db = DB.getInstance();
        this.sessions = new HashMap<>();
    }

    /**
     * regista o user na base de dados
     *
     * @param uname - username
     * @param pw    - password
     * @return true se conseguir registar e false se já existir alguem com as credenciais
     */
    @Override
    public boolean register(String uname, String pw) throws RemoteException {
        return db.registerUser(uname, pw);
    }

    /**
     * Atribui uma nova session ao user, caso este já esteja logado devolve a session ativa
     * @param uname - username
     * @param pw - password
     * @return - Session logada com as credenciais do user
     */
    @Override
    public SessionRI login(String uname, String pw) throws RemoteException {
        if(db.existsUser(uname,pw)){
            if(!sessions.containsKey(uname)){
                SessionRI sessionRI = new SessionImpl(db, db.getUserByCredentials(uname,pw));
                this.sessions.put(uname, sessionRI);
                return sessionRI;

            }
            //caso já exista alguém logado na session
            else{
                return sessions.get(uname);
            }
        }
        return null;
    }

}