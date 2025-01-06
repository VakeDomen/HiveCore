package upr.famnit;

import upr.famnit.components.LogLevel;
import upr.famnit.managers.DatabaseManager;
import upr.famnit.network.ClientServer;
import upr.famnit.network.ManagementServer;
import upr.famnit.network.NodeServer;
import upr.famnit.util.Config;
import upr.famnit.util.Logger;

import java.io.*;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        try {
            Config.init();
            DatabaseManager.createKeysTable();

            NodeServer nodeServer = new NodeServer();
            ClientServer clientServer = new ClientServer();
            ManagementServer managementServer = new ManagementServer();

            nodeServer.start();
            clientServer.start();
            managementServer.start();




            nodeServer.join();
            clientServer.join();
            managementServer.join();

        } catch (IOException | SQLException | InterruptedException e) {
            Logger.error("An error occurred in the main thread: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
