package edu.ufp.inf.sd.rmi.server;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import edu.ufp.inf.sd.rmi.client.ObserverRI;
import edu.ufp.inf.sd.rmi.util.RabbitUtils;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskGroup extends UnicastRemoteObject implements SubjectRI {

    static AtomicInteger nextId = new AtomicInteger();
    private int id;

    //e.g. SHA-512, PBKDF2, BCrypt, SCrypt
    private String hashAlgorithm;

    //false - parcial | true - total
    private boolean type;

    private File charSequence;

    private File hashCodesSet;

    private int subCharSize;

    //true -  em andamento | false - parado
    private boolean state;

    //workers que vão trabalhar no task
    private Set<ObserverRI> observers = new HashSet<>();

    private User creator;

    //map que armazena as respostas <string,code>
    private Map<String, String> results = new HashMap<>();

    //arraylist que armazena as string
    private ArrayList<String> strings;

    //arraylist que armazena os hashes
    private ArrayList<String> hashes;

    //nome único para rabbitmq
    private String TASK_QUEUE_NAME;

    //true= paused | false=running
    private boolean paused;


    public TaskGroup(String hashAlgorithm, boolean type, File charSequence, int subCharSize, File hashCodesSet, User creator) throws RemoteException {
        super();
        //id unico
        this.id = nextId.incrementAndGet();
        this.hashAlgorithm = hashAlgorithm;
        this.type = type;
        this.charSequence = charSequence;
        this.subCharSize = subCharSize;
        this.hashCodesSet = hashCodesSet;

        //true=em andamento, false=terminada
        this.state = true;
        this.creator=creator;


        this.hashes = this.convertFileToArray(hashCodesSet);

        System.out.println(this.hashes);

        this.strings = this.convertFileToArray(charSequence);

        //task queue name = contacatenação de task_queue com o id da task
        this.TASK_QUEUE_NAME = "task_queue@" + this.id;

        //false = em andamento, true= parada
        this.paused=false;

        System.out.println(TASK_QUEUE_NAME);

        try {
            this.mine();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }


    }


    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public boolean getType() {
        return type;
    }

    public void setType(boolean type) {
        this.type = type;
    }

    @Override
    public File getCharSequence() {
        return charSequence;
    }

    public void setCharSequence(File charSequence) {
        this.charSequence = charSequence;
    }

    public File getHashCodesSet() {
        return hashCodesSet;
    }

    public void setHashCodesSet(File hashCodesSet) {
        this.hashCodesSet = hashCodesSet;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }


    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Set<ObserverRI> getObservers() {
        return observers;
    }

    @Override
    public Map<String, String> getResults() {
        return results;
    }

    @Override
    public void removeHash(String code) throws RemoteException {
        this.getHashes().remove(code);
    }

    @Override
    public void updateResults(String string, String code) throws RemoteException {
        this.getResults().put(string, code);
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }

    @Override
    public ArrayList<String> getStrings() {
        return strings;
    }

    @Override
    public ArrayList<String> getHashes() {
        return hashes;
    }

    @Override
    public String getTASK_QUEUE_NAME() {
        return TASK_QUEUE_NAME;
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public boolean isPaused() throws RemoteException
    {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setId(int id) {
        this.id = id;
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
     * Converte um ficheiro de txt para um ArrayList
     *
     * @param f ficheiro a converter
     * @return ArrayList convertido
     */
    public ArrayList<String> convertFileToArray(File f) {
        ArrayList<String> list = new ArrayList<>();
        try {
            Scanner s = new Scanner(f);
            while (s.hasNextLine()) {
                list.add(s.nextLine());
            }
            s.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return list;

    }

    /**
     * adicionar um observer (worker) a este task
     *
     * @param observerRI - observer a adicionar
     */
    @Override
    public void attach(ObserverRI observerRI) {
        this.getObservers().add(observerRI);
    }


    /**
     * remover um observer (worker) deste task
     *
     * @param observerRI - observer a remover
     */
    @Override
    public void detach(ObserverRI observerRI) {
        this.getObservers().remove(observerRI);
    }

    /**
     * quando algum dos worker encontra resposta, dá update em todos os workers do task e
     * interrompe todas as threads em andamento, uma vez que a task terminou
     */
    @Override
    public void notifyAllObservers() {
        for (ObserverRI obs : this.observers) {
            try {
                obs.update(this.state);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * quando um dos hashs é encontrado todos os workers são notificados para o remover da
     * sua lista, desta forma evitando trabalho desnecessário
     */
    @Override
    public void notifyAllObserversRemoveHash() {
        for (ObserverRI obs : this.observers) {
            try {
                obs.updateRemoveHash(this.getHashes());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Coloca na queue do rabbit todoo o trabalho
     * @throws IOException
     * @throws TimeoutException
     */
    public void mine() throws IOException, TimeoutException {
        //criar o canal se não existir
        try (Channel channel = RabbitUtils.createChannel2Server(RabbitUtils.newConnection2Server("localhost", "guest", "guest"))) {
            boolean durable = true;
            //durable para não se perder se o rabbit der restart
            channel.queueDeclare(TASK_QUEUE_NAME, durable, false, false, null);


            int line = 1;
            int fileLines = this.countFileLines(charSequence);

            //envia todas as linhas, em sub-intervalos definidos pelo utilizador, para a queue
            while (line <= fileLines) {
                int firstLine = line;
                int lastLine = line + this.subCharSize;
                //se ultrapassar a ultima linha
                if (lastLine > fileLines)
                    lastLine = fileLines;


                String message = firstLine + "," + lastLine; //ex: 0,200

                //para evitar perder queues quando rabbit crashar, marcar as mensagens como persistentes
                channel.basicPublish("", TASK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());

                //System.out.println(" Task sent to queue(" + this.TASK_QUEUE_NAME + "): " + message);

                line += subCharSize + 1; //+1 para começar na linha seguinte
            }
        }

    }

    @Override
    public String toString() {
        return "TaskGroup{" +
                "id=" + id +
                ", hashAlgorithm='" + hashAlgorithm + '\'' +
                ", type=" + type +
                ", subCharSize=" + subCharSize +
                ", state=" + state +
                ", TASK_QUEUE_NAME='" + TASK_QUEUE_NAME + '\'' +
                '}';
    }
}