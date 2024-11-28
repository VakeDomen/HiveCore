package upr.famnit.components;

import upr.famnit.util.Logger;

import java.io.*;
import java.net.Socket;

import static upr.famnit.util.Config.PROXY_TIMEOUT_MS;

public class ClientRequest {

    private final Socket clientSocket;
    private final Request request;
    private long queEnterTime;

    /**
     * Handles client requests by forwarding them to the Node and writing the response
     * back through the clientSocket.
     *
     * @param clientSocket   The client's socket connection.
     */
    public ClientRequest(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        Logger.log("Parsing client request...");
        clientSocket.setSoTimeout(PROXY_TIMEOUT_MS);
        this.request = new Request(clientSocket);
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public Request getRequest() {
        return request;
    }

    public void stamp() {
        this.queEnterTime = System.currentTimeMillis();
    }

    public long getQueEnterTime() {
        return queEnterTime;
    }
}
