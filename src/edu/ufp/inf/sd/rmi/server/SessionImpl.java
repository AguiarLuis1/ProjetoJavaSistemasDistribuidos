package edu.ufp.inf.sd.rmi.server;

import edu.ufp.inf.sd.rmi.client.ObserverRI;
import edu.ufp.inf.sd.rmi.client.Worker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class SessionImpl extends UnicastRemoteObject implements SessionRI {

    private DB db;

    private User user;

    public SessionImpl(DB db, User user) throws RemoteException {
        super();
        this.db = db;
        this.user = user;
    }

    @Override
    public void logout() {

    }

    /**
     * Procura na base de dados todos os taskgroups e forma um string com estas
     *
     * @return - String com todos os tasks
     */
    @Override
    public String listTaskGroups() throws RemoteException {
        StringBuilder stringBuilder = new StringBuilder();
        if (!db.getTaskGroups().isEmpty()) {
            for (TaskGroup tg : db.getTaskGroups()) {
                stringBuilder.append(tg.toString()).append("\n");
            }

            return stringBuilder.toString();
        }
        return "There are none task groups";
    }

    /**
     * Apaga o taskgroup,  remove a sua ligação com os intervenientes, para as threads dos workers e remove-a da db
     *
     * @param idTask - id task para se apagar
     */
    @Override
    public void deleteTaskGroup(int idTask) throws RemoteException {
        for (TaskGroup taskGroup : this.db.getTaskGroups()) {
            if (taskGroup.getId() == idTask) {

                taskGroup.setCreator(null);

                for (ObserverRI obs : taskGroup.getObservers()) {
                    obs.setSubject(null);
                    obs.stopThread();
                }
                this.db.getTaskGroups().remove(taskGroup);
                break;
            }
        }
    }

    /**
     * Cria um novo task group se o user tiver creditos suficientes
     *
     * @param hashAlgorithm - algoritmo a utilizar
     * @param type          - tipo (parcial ou total)
     * @param charSequence  - Ficheiro com a sequencia de chars
     * @param hashCodesSet  - Ficheiro com os hash
     * @return o task group criado, se não tiver creditos null
     */
    @Override
    public SubjectRI createTaskGroup(String hashAlgorithm, boolean type, File charSequence, int subCharSize, File hashCodesSet) throws RemoteException {
        int numberOfChars = this.countFileLines(charSequence);
        int numberOfHashes = this.countFileLines(hashCodesSet);

        //preço de criação depende do nº de strings e de hashes
        int creditsToCreateTask = (numberOfChars / 500) * (numberOfHashes * 5);

        if (this.user.getCredits() >= creditsToCreateTask) {
            TaskGroup newTaskGroup = new TaskGroup(hashAlgorithm, type, charSequence, subCharSize, hashCodesSet, this.user);
            this.db.addTaskGroup(newTaskGroup);
            this.user.setCredits(this.user.getCredits() - creditsToCreateTask);

            System.out.println("Spent " + creditsToCreateTask + " credits to create new task");
            return newTaskGroup;
        }
        System.out.println("Not enough credits to create task, you need " + creditsToCreateTask + " credits");
        return null;
    }

    /**
     * Cria um novo worker
     *
     * @return novo worker
     */
    @Override
    public ObserverRI createWorker() throws RemoteException {
        Worker worker = new Worker(this.user);
        this.db.addWorker(worker);
        return worker;
    }

    /**
     * Associa um worker a um taskGroup, e inicia a thread do worker
     *
     * @param worker worker a associar
     * @param taskId task group id do task
     */
    @Override
    public void assocWorker(ObserverRI worker, int taskId) throws RemoteException {

        if (worker.getSubject() == null) {
            for (SubjectRI tg : db.getTaskGroups()) {
                if (tg.getId() == taskId) {
                    tg.attach(worker);
                    worker.setSubject(tg);
                    worker.startThread();
                    break;
                }
            }
        }
    }

    /**
     * Desassocia um worker de um task e interrompe a sua thread
     *
     * @param worker worker a desassociar
     * @param taskId - id task group do qual se desassocia o worker
     */
    @Override
    public void deassocWorker(ObserverRI worker, int taskId) throws RemoteException {

        for (SubjectRI taskGroup : db.getTaskGroups()) {
            if (taskGroup.getId() == taskId) {
                if (worker.getSubject().equals(taskGroup)) {
                    taskGroup.detach(worker);
                    worker.setSubject(null);
                    worker.stopThread();
                }
            }
        }

    }

    @Override
    public User getUser() throws RemoteException {
        return this.user;
    }

    /**
     * Abre o ficheiro e conta o nº de linhas
     *
     * @param f - ficheiro a ler
     * @return nº de linhas
     * @throws IOException
     */
    public int countFileLines(File f) {
        BufferedReader reader;
        int lines = 0;
        try {
            reader = new BufferedReader(new FileReader(f));
            while (reader.readLine() != null) lines++;
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    /**
     * pausa o taskgroup
     *
     * @param idTask - id do task a pausar
     * @throws RemoteException
     */
    @Override
    public void pauseTaskGroup(int idTask) throws RemoteException {

        for (TaskGroup tg : this.db.getTaskGroups()) {
            if (tg.getId() == idTask && tg.getCreator().equals(this.user)) {
                tg.setPaused(true);
                System.out.println("Task paused!");
                break;
            }
        }
    }

    /**
     * retomar o trabalho da task
     *
     * @param idTask - id da task a retomar
     * @throws RemoteException
     */
    @Override
    public void resumeTaskGroup(int idTask) throws RemoteException {

        for (TaskGroup tg : this.db.getTaskGroups()) {
            if (tg.getId() == idTask && tg.getCreator().equals(this.user)) {
                tg.setPaused(false);
                System.out.println("Task resumed!");
                break;
            }
        }
    }


}