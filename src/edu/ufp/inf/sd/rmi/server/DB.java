package edu.ufp.inf.sd.rmi.server;

import edu.ufp.inf.sd.rmi.client.Worker;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

//DB singleton para ser partilhada para ser igual para todos os users
public class DB {

    private static DB db = null;

    private final Set<User> users;

    private final Set<TaskGroup> taskGroups;

    private final Set<Worker> workers;


    private DB() {
        this.users = new HashSet<>();
        this.taskGroups = new HashSet<>();
        this.workers=new HashSet<>();
    }

    //caso ainda não exista uma db cria-se, caso contrário vai-se buscá-la
    public static synchronized DB getInstance() {
        if (db == null)
            db = new DB();
        return db;
    }


    public Set<User> getUsers() {
        return users;
    }

    public Set<TaskGroup> getTaskGroups() {
        return taskGroups;
    }

    public Set<Worker> getWorkers() {
        return workers;
    }

    /**
     * Verifica as credenciais de um utilizador
     *
     * @param uname - username
     * @param pw    - password
     * @return true se existir e false se não existir
     */
    public boolean existsUser(String uname, String pw) {
        for (User u : this.users) {
            if (u.getName().compareTo(uname) == 0 && u.getPassword().compareTo(pw) == 0)
                return true;
        }
        return false;
    }

    /**
     * Retorna um user através das suas credenciais
     *
     * @param uname - username
     * @param pw    - password
     * @return retorna o objeto user
     */
    public User getUserByCredentials(String uname, String pw) {
        for (User u : this.users) {
            if (u.getName().compareTo(uname) == 0 && u.getPassword().compareTo(pw) == 0)
                return u;
        }
        return null;
    }

    /**
     * Registar um novo utilizador na bd
     *
     * @param uname - username
     * @param pw    - pw
     * @return - true se conseguir registar e false caso contrário
     */
    public boolean registerUser(String uname, String pw) {
        if (!existsUser(uname, pw)) {
            users.add(new User(uname, pw));
            return true;
        }
        return false;
    }


    /**
     * Adicionar um novo Task Group à base de dados
     *
     * @param taskGroup - task group a adicionar
     */
    public void addTaskGroup(TaskGroup taskGroup) {
        this.taskGroups.add(taskGroup);
    }

    /**
     * Adicionar um novo Worker à base de dados
     *
     * @param worker - task group a adicionar
     */
    public void addWorker(Worker worker) {
        this.workers.add(worker);
    }

}