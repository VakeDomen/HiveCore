//package upr.famnit.managers;
//
//import org.java_websocket.WebSocket;
//import upr.famnit.network.messages.Message;
//
//import java.util.HashMap;
//import java.util.concurrent.BlockingDeque;
//import java.util.concurrent.LinkedBlockingDeque;
//
//public class NetworkManager {
//    private Server server;
//    public HashMap<WebSocket, Peer> peers = new HashMap<>();
//    public BlockingDeque<Message> messageQueue = new LinkedBlockingDeque<>();
//
//
//
//    public NetworkManager(int port) {
//        this.server = new Server(
//             port, this);
//        this.server.start();
//    }
//}
