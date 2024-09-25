package upr.famnit;

import upr.famnit.components.LogLevel;
import upr.famnit.managers.DatabaseManager;
import upr.famnit.network.ClientServer;
import upr.famnit.network.ManagementServer;
import upr.famnit.network.NodeServer;
import upr.famnit.util.Logger;

import java.io.*;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        try {
            DatabaseManager.createKeysTable();

            NodeServer nodeServer = new NodeServer();
            Thread nodeServerThread = new Thread(nodeServer);

            ClientServer clientServer = new ClientServer();
            Thread clientServerThread = new Thread(clientServer);

            ManagementServer managementServer = new ManagementServer();
            Thread managementServerThread = new Thread(managementServer);

            nodeServerThread.start();
            clientServerThread.start();
            managementServerThread.start();

            nodeServerThread.join();
            clientServerThread.join();
            managementServerThread.join();

        } catch (IOException | SQLException | InterruptedException e) {
            Logger.log("An error occurred in the main thread: " + e.getMessage(), LogLevel.error);
            e.printStackTrace();
        }
    }
}
