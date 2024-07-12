package upr.famnit.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import upr.famnit.managers.NetworkManager;
import upr.famnit.network.messages.AuthenticationMessage;
import upr.famnit.network.messages.Message;
import upr.famnit.util.Logger;
import upr.famnit.util.LogLevel;


import java.net.InetSocketAddress;


public class Server extends WebSocketServer {
    GsonBuilder gsonBuilder;
    Gson gson;
    NetworkManager networkManager;
    public Server(int port, NetworkManager networkManager) {
        super(new InetSocketAddress(port));
        this.networkManager = networkManager;
        gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Message.class, new MessageDeserializer());
        gson = gsonBuilder.create();

    }
    @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        if(!networkManager.peers.containsKey(webSocket)) {
            //pending authentication likely via handshake
            networkManager.peers.put(webSocket, new Peer());
            Logger.log("Peer connected", LogLevel.network);
        }
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        networkManager.peers.remove(webSocket);
        Logger.log("Client disconnected!",LogLevel.network);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        Message message = gson.fromJson(s, Message.class);
        if(message==null){
            Logger.log("Protocol violation: Unknown message type from client : " + webSocket.getRemoteSocketAddress(),LogLevel.network);
            webSocket.close();
        }

        webSocket.send(s);
        /*
        if (message instanceof AuthenticationMessage authMessage) {
            System.out.println("Type: " + authMessage.getType());
            System.out.println("Token: " + authMessage.getBody().getToken());
            for (AuthenticationMessage.Body.HW hw : authMessage.getBody().getHw()) {
                Logger.log("GPU Model: " + hw.getGpuModel(), LogLevel.info);
                Logger.log("GPU VRAM: " + hw.getGpuVram(), LogLevel.info);
                Logger.log("Driver: " + hw.getDriver(), LogLevel.info);
                Logger.log("CUDA: " + hw.getCuda(), LogLevel.info);
            }
        }*/
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        Logger.log(e.getMessage(),LogLevel.error);
    }

    @Override
    public void onStart() {

    }
}
