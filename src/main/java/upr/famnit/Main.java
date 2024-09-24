package upr.famnit;

import upr.famnit.components.LogLevel;
import upr.famnit.network.ClientServer;
import upr.famnit.network.NodeServer;
import upr.famnit.util.Logger;

import java.io.*;

public class Main {

    public static void main(String[] args) {
        try {
            NodeServer nodeServer = new NodeServer();
            Thread nodeServerThread = new Thread(nodeServer);

            ClientServer clientServer = new ClientServer();
            Thread clientServerThread = new Thread(clientServer);

            nodeServerThread.start();
            clientServerThread.start();

            nodeServerThread.join();
            clientServerThread.join();

        } catch (IOException | InterruptedException e) {
            Logger.log("An error occurred in the main thread: " + e.getMessage(), LogLevel.error);
            e.printStackTrace();
        }
    }
}
