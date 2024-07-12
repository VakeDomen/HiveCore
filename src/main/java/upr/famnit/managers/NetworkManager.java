package upr.famnit.managers;

import org.java_websocket.WebSocket;
import upr.famnit.network.Message;
import upr.famnit.network.Peer;
import upr.famnit.network.Server;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class NetworkManager {
    private Server server;
    public HashMap<WebSocket, Peer> peers = new HashMap<>();
    public BlockingDeque<Message> messageQueue = new LinkedBlockingDeque<>();



    public NetworkManager(int port) {
        this.server = new Server(
                new InetSocketAddress("localhost", port),
                this);
        this.server.start();
    }
}
