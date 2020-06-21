package edu.ufp.inf.sd.rmi.client;


import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import edu.ufp.inf.sd.rmi.server.SubjectRI;
import edu.ufp.inf.sd.rmi.server.TaskGroup;
import edu.ufp.inf.sd.rmi.server.User;
import edu.ufp.inf.sd.rmi.util.RabbitUtils;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//Runnable permite implementar threads
public class Worker extends UnicastRemoteObject implements ObserverRI, Runnable {


    private SubjectRI subject;

    private User owner;

    //true -  em andamento | false - parado
    private boolean hashingState;

    //thread do worker (iniciada quando se adiciona o worker à task)
    private Thread thread;

    //arraylist que armazena as strings da task (melhora performance)
    private ArrayList<String> taskStrings;

    //arraylist que armazena os hashes da task (melhora performance)
    private ArrayList<String> taskHashes;


    public Worker(User owner) throws RemoteException {

        super();

        this.owner = owner;
        this.hashingState = true;

        //instancio a thread com o target do run desta
        this.thread = new Thread(this);


    }

    @Override
    public SubjectRI getSubject() {
        return subject;
    }

    @Override
    public void setSubject(SubjectRI subject) {
        this.subject = subject;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public void setHashingState(boolean hashingState) {
        this.hashingState = hashingState;
    }


    public boolean getHashingState() {
        return hashingState;
    }

    public Thread getThread() {
        return thread;
    }

    public void setTaskHashes(ArrayList<String> taskHashes) {
        this.taskHashes = taskHashes;
    }

    /**
     * quando um worker encontra uma resposta adiciona-a ao map de respostas,
     * notifica todos os outros workers através do notify do task e atribui creditos ao criador do worker
     * e escreve no ficheiro de resultados a string seguida do respetivo código
     */
    public void foundAnswer(String string, String code) {
        int creditsForAnswer = 10;

        try {

            this.subject.updateResults(string, code);
            this.owner.setCredits(this.owner.getCredits() + creditsForAnswer);
            this.subject.removeHash(code);
            this.subject.notifyAllObserversRemoveHash();

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //se encontrou todas as respostas, ou seja o array de hashes está vazio
        if (this.taskHashes.size() == 0)
            this.endMining();
    }

    /**
     * responsável por acabar o processo de mining
     * notifica todos os workers que o trabalho chegou ao fim, escreve os resultados para ficheiro e interrompe as threads
     */
    public void endMining() {
        try {
            this.subject.setState(false);
            this.subject.notifyAllObservers();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        this.writeResultsToFile();

        System.out.println("All answers found or end of file reached!");

    }


    /**
     * cria um novo ficheiro e escreve os resultados da mineração
     */
    public void writeResultsToFile() {
        try {
            File resultsFile = new File("C:\\Users\\Aguia\\IdeaProjects\\ProjetoSD\\src\\edu\\ufp\\inf\\sd\\rmi\\server\\txt\\results_" + this.subject.getTASK_QUEUE_NAME());
            resultsFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(resultsFile);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            //itera para todos os resultados do map
            for (Map.Entry<String, String> entry : this.subject.getResults().entrySet()) {
                bw.write(entry.getKey() + " => " + entry.getValue());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * dá update ao hashing state para o mesmo que o task
     */
    @Override
    public void update(boolean taskState) {
        try {
            this.setHashingState(taskState);
            this.stopThread();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * dá update nos hashed do worker
     */
    @Override
    public void updateRemoveHash(ArrayList<String> taskHashes) {
        this.setTaskHashes(taskHashes);
    }

    /**
     * inicia o funcionamento da thread
     *
     * @throws RemoteException
     */
    @Override
    public void startThread() throws RemoteException {
        this.getThread().start();
    }

    /**
     * interrompe o funcionamento da thread
     *
     * @throws RemoteException
     */
    @Override
    public void stopThread() throws RemoteException {
        this.getThread().interrupt();
    }


    /**
     * função que é corrida pelo thread
     * lê e faz o trabalho da queue, inserido nesta pelo taskgroup
     */
    @Override
    public void run() {

        try {
            this.taskStrings = this.subject.getStrings();
            this.taskHashes = this.subject.getHashes();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        try (Channel channel = RabbitUtils.createChannel2Server(RabbitUtils.newConnection2Server("localhost", "guest", "guest"))) {
            boolean durable = true;
            channel.queueDeclare(this.subject.getTASK_QUEUE_NAME(), durable, false, false, null);


            System.out.println("Thread " + this.thread.getId() + " is waiting for work.");

            //com esta definição o task não envia mais work até o worker terminar este
            int prefetchCount = 1;
            channel.basicQos(prefetchCount);

            //corre até não ter mais work na queue
            while (true) {

                //se a task estiver em pausa
                while (this.subject.isPaused()) {
                    System.out.println("Thread " + this.thread.getId() +" your task is paused!");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println("Thread " + this.thread.getId() + " received message: " + message);

                    //divide a string que recebe num array, 0 = firstLine e 1 = lastLine
                    String[] arrayMsg = message.split(",", 2);

                    int firstLine = Integer.parseInt(arrayMsg[0]);
                    int lastLine = Integer.parseInt(arrayMsg[1]);

                    //se a taskgroup estiver ativa, caso contrario não faz o trabalho
                    if (this.getHashingState())
                        subCharSetCompare(firstLine, lastLine);
                    else
                        System.out.println("Skipped (only consuming not doing work)");

                    //se atingiu o fim do ficheiro sem encontrar todas as respostas
                    if (this.taskStrings.size() == lastLine)
                        this.endMining();

                    System.out.println("Thread " + this.thread.getId() + " done processing task");

                    //Envia um ack manualmente sempre que termina uma task, desta forma, mesmo que o worker seja interrompido enquando processa uma
                    //mensagem, não se perde informação nenhuma. Pouco depois do worker ser interrompido, todas as mensagens sem ack vão ser recolocadas na queue.
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                };
                //autoAck a false para enviar manualmente um ack quando acabar as task
                boolean autoAck = false;
                channel.basicConsume(this.subject.getTASK_QUEUE_NAME(), autoAck, deliverCallback, consumerTag -> {
                });


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Ignora as linhas até á primeira, depois analisa o intervalo enviado pelos argumentos, comparando cada uma das strings
     * com todos os hashs
     *
     * @param firstLine - primeira linha a analisar
     * @param lastLine  - ultima linha analisar
     */
    public void subCharSetCompare(int firstLine, int lastLine) {

        List<String> subStrings = this.taskStrings.subList(firstLine, lastLine);
        BufferedReader reader;
        try {

            reader = new BufferedReader(new FileReader(this.getSubject().getCharSequence()));

            //corre todas as strings enquanto o taskgroup estiver ativo
            for (String s : subStrings) {


                String answer = compareStringWithHashes(encryptToSHA512(s));
                if (answer != null) {
                    this.foundAnswer(s, answer);
                    System.out.println(s + " = " + answer);
                }
                //adiciona um credito por cada string comparada
                this.owner.setCredits(this.owner.getCredits() + 1);

            }

            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Compara a string encriptada com todos os hashes do arraylist
     *
     * @param string string a comparar
     * @return se existir relaçao retorna a string encriptada, caso contrário retorna null
     */
    public String compareStringWithHashes(String string) {

        for (String hash : this.taskHashes) {
            if (hash.equals(string))
                return hash;
        }
        return null;

    }

    /**
     * Converte uma string para SHA512
     *
     * @param input String de input
     * @return String encriptada
     */
    public String encryptToSHA512(String input) {
        try {
            // o metodo getInstance é chamado com o algoritmo SHA-512
            MessageDigest md = MessageDigest.getInstance("SHA-512");


            // para calcular o message digest da string de input
            // returnado como um array de bytes
            byte[] messageDigest = md.digest(input.getBytes());

            // converter o array de bytes em representação de sinal
            BigInteger no = new BigInteger(1, messageDigest);

            // convert o message digest para hexadecimal
            String hashtext = no.toString(16);

            // add 0's precedentes para a tornar 32 bits
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            // return HashText
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}