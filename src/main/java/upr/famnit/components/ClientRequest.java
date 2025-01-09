package upr.famnit.components;

import upr.famnit.authentication.KeyUtil;
import upr.famnit.authentication.VerificationType;
import upr.famnit.util.Config;
import upr.famnit.util.Logger;

import javax.security.sasl.AuthenticationException;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import static upr.famnit.util.Config.PROXY_TIMEOUT_MS;

public class ClientRequest {

    private final Socket clientSocket;
    private final Request request;


    private long queEnterTime;
    private long queLeftTime;
    private long responseFinishTime;
    private String responseNodeName;

    /**
     * Handles client requests by forwarding them to the Node and writing the response
     * back through the clientSocket.
     *
     * @param clientSocket   The client's socket connection.
     */
    public ClientRequest(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        clientSocket.setSoTimeout(PROXY_TIMEOUT_MS);
        this.request = new Request(clientSocket);
        this.authorize();
    }

    private void authorize() throws AuthenticationException {
        if (Config.USER_AUTHENTICATION) {
            Map<String, String> headers = this.request.getHeaders();
            if (!headers.containsKey("authorization")) {
                throw new AuthenticationException("Missing authorization header");
            }

            String[] tokens = headers.get("authorization").split(" ");
            if (tokens.length != 2) {
                throw new AuthenticationException("Invalid authorization header");
            }

            String key = tokens[1];
            if (!KeyUtil.verifyKey(key, VerificationType.ClientRequest)) {
                throw new AuthenticationException("Invalid authorization header");
            }
        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public Request getRequest() {
        return request;
    }

    public void stampQueueEnter() {
        this.queEnterTime = System.currentTimeMillis();
    }
    public void stampQueueLeave(String responseNodeName) {
        this.responseNodeName = responseNodeName;
        this.queLeftTime = System.currentTimeMillis();
    }
    public void stampResponseFinish() {
        this.responseFinishTime = System.currentTimeMillis();
    }

    public long queTime() {
        if (queEnterTime == 0) {
            return 0l;
        }

        if (queLeftTime == 0) {
            return System.currentTimeMillis() - queEnterTime;
        }

        return queLeftTime - queEnterTime;
    }

    public long proxyTime() {
        if (queLeftTime == 0) {
            return 0l;
        }

        if (responseFinishTime == 0) {
            return System.currentTimeMillis() - queLeftTime;
        }

        return responseFinishTime - queLeftTime;
    }

    public long totalTime() {
        if (queEnterTime == 0) {
            return 0l;
        }

        if (responseFinishTime == 0) {
            return System.currentTimeMillis() - queEnterTime;
        }

        return responseFinishTime - queEnterTime;
    }
}
