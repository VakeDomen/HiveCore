package upr.famnit;

import upr.famnit.managers.DatabaseManager;
import upr.famnit.network.ClientServer;
import upr.famnit.network.ManagementServer;
import upr.famnit.network.WorkerServer;
import upr.famnit.util.Config;
import upr.famnit.util.Logger;

import java.io.*;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        try {
            Config.init();
            DatabaseManager.createKeysTable();

            WorkerServer workerServer = new WorkerServer();
            ClientServer clientServer = new ClientServer();
            ManagementServer managementServer = new ManagementServer();

            workerServer.start();
            clientServer.start();
            managementServer.start();




            workerServer.join();
            clientServer.join();
            managementServer.join();

        } catch (IOException | SQLException | InterruptedException e) {
            Logger.error("An error occurred in the main thread: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
