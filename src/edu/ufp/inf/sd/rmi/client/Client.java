package edu.ufp.inf.sd.rmi.client;

import edu.ufp.inf.sd.rmi.server.FactoryRI;
import edu.ufp.inf.sd.rmi.server.SessionRI;
import edu.ufp.inf.sd.rmi.server.SubjectRI;
import edu.ufp.inf.sd.rmi.server.TaskGroup;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;

import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    private SetupContextRMI contextRMI;
    private FactoryRI factoryRI;

    public Client(String[] args) {
        try {
            String registryIP = args[0];
            String registryPort = args[1];
            String serviceName = args[2];
            contextRMI = new SetupContextRMI(this.getClass(), registryIP, registryPort, new String[]{serviceName});
        } catch (RemoteException e) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private Remote lookupService() {
        try {
            //Get proxy to rmiregistry
            Registry registry = contextRMI.getRegistry();
            //Lookup service on rmiregistry and wait for calls
            if (registry != null) {
                //Get service url (including servicename)
                String serviceUrl = contextRMI.getServicesUrl(0);
                //Logger.getLogger(this.getClass().getName()).log(Level.INFO, "going to lookup service @ {0}", serviceUrl);

                //============ Get proxy to HelloWorld service ============
                factoryRI = (FactoryRI) registry.lookup(serviceUrl);
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "registry not bound (check IPs). :(");
                //registry = LocateRegistry.createRegistry(1099);
            }
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return factoryRI;
    }

    private void playService() {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("TEST -> \n1 - Create task and pause it\n2 - Join Task with 2 workers\n3 - Join Task with 1 workers\n4 - Resume task");
            int option = scanner.nextInt();
            scanner.nextLine();
            switch (option) {

                // criar task
                case 1:
                    factoryRI.register("luis", "luis");
                    SessionRI sessionRI = factoryRI.login("luis", "luis");
                    File f1 = new File("C:\\Users\\Aguia\\IdeaProjects\\ProjetoSD\\src\\edu\\ufp\\inf\\sd\\rmi\\server\\txt\\text.txt");
                    File f2 = new File("C:\\Users\\Aguia\\IdeaProjects\\ProjetoSD\\src\\edu\\ufp\\inf\\sd\\rmi\\server\\txt\\hash.txt");
                    sessionRI.createTaskGroup("SHA-512", false, f1, 800, f2);
                    sessionRI.pauseTaskGroup(1);

                    break;

                // add 2 workers
                case 2:
                    factoryRI.register("luis2", "luis2");
                    SessionRI sessionRI2 = factoryRI.login("luis2", "luis2");
                    System.out.println(sessionRI2.listTaskGroups());
                    createWorker(sessionRI2, 1);
                    createWorker(sessionRI2, 1);
                    break;

                //add outro worker no mesmo task
                case 3:
                    factoryRI.register("luis3", "luis3");
                    SessionRI sessionRI3 = factoryRI.login("luis3", "luis3");
                    System.out.println(sessionRI3.listTaskGroups());
                    createWorker(sessionRI3, 1);
                    break;

                //resume da task
                case 4:
                    SessionRI sessionRI4 = factoryRI.login("luis", "luis");
                    sessionRI4.resumeTaskGroup(1);
                    break;


                default:
                    System.out.println("Wrong number!");


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException, TimeoutException {

        if (args != null && args.length < 3) {
            System.exit(-1);
        } else {
            assert args != null;
            //1. ============ Setup client RMI context ============
            Client client = new Client(args);
            //2. ============ Lookup service ============
            client.lookupService();
            //3. ============ Play with service ============
            client.playService();
        }
    }

    /**
     * Creates a new worker who has 1 thread
     *
     * @param sessionRI with all the methods from the session
     */
    private void createWorker(SessionRI sessionRI, int idTask) throws IOException, TimeoutException {

        ObserverRI worker = new Worker(sessionRI.getUser());
        sessionRI.assocWorker(worker, idTask);
    }


}