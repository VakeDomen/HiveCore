package upr.famnit;

import upr.famnit.managers.ClientRequestManager;
import upr.famnit.managers.NodeConnectionManager;
import upr.famnit.network.ClientServer;
import upr.famnit.network.NodeServer;
import upr.famnit.util.Logger;
import upr.famnit.util.LogLevel;

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
            // Start the Node connection handler
//            NodeConnectionManager nodeHandler = new NodeConnectionManager(NODE_CONNECTION_PORT);
//            new Thread(() -> {
//                try {
//                    nodeHandler.acceptConnection();
//                } catch (IOException e) {
//                    Logger.log("Error accepting connection from Node: " + e.getMessage(), LogLevel.error);
//                    e.printStackTrace();
//                }
//            }).start();



            // Start the proxy server for clients
//            ServerSocket serverSocket = new ServerSocket(PROXY_PORT);
//            Logger.log("Proxy server is running on port " + PROXY_PORT + "...");

//            // Wait for the Node to connect
//            while (nodeHandler.getNodeSocket() == null) {
//                Thread.sleep(100);
//            }

            // Log when Node is connected
//            Logger.log("Node connected: " + nodeHandler.getNodeSocket().getRemoteSocketAddress(), LogLevel.network);

//            while (true) {
//                // Accept incoming client connection
//                Socket clientSocket = serverSocket.accept();
//                Logger.log("Client connected: " + clientSocket.getRemoteSocketAddress(), LogLevel.network);
//                ClientRequestManager crm = new ClientRequestManager(clientSocket, nodeHandler.getNodeSocket());
//                executorService.submit(crm);
//            }
        } catch (IOException e) {
            Logger.log("An error occurred in the main thread: " + e.getMessage(), LogLevel.error);
            e.printStackTrace();
        }
    }
}
