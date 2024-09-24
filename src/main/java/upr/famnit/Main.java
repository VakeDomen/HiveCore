package upr.famnit;

import upr.famnit.components.LogLevel;
import upr.famnit.managers.NodeConnectionMonitor;
import upr.famnit.network.ClientServer;
import upr.famnit.network.NodeServer;
import upr.famnit.util.Logger;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(8);

            NodeServer nodeServer = new NodeServer();
            executorService.submit(nodeServer);

            ClientServer clientServer = new ClientServer(nodeServer);
            executorService.submit(clientServer);



        } catch (IOException e) {
            Logger.log("An error occurred in the main thread: " + e.getMessage(), LogLevel.error);
            e.printStackTrace();
        }
    }
}
