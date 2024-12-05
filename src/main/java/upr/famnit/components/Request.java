package upr.famnit.components;

import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Request {

    private final String protocol;
    private final String method;
    private final String uri;
    private final Map<String, String> headers;
    private final byte[] body;

    public Request(String protocol, String method, String uri, Map<String, String> headers, byte[] body) {
        this.protocol = protocol;
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
    }

    public Request(Socket socket) throws IOException {
        // Read the client's request line
        if (socket.isClosed() || !socket.isConnected()) {
            throw new IOException("Socket closed or not connected");
        }

        InputStream clientInputStream = socket.getInputStream();
        String requestLine = StreamUtil.readLine(clientInputStream);

        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Received empty request from client. ");
        }

        // Parse the request line
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length != 3) {
            throw new IOException("Bad Request");
        }

        this.method = requestParts[0];
        this.uri = requestParts[1];
        this.protocol = requestParts[2];

        if (protocol.equals("HIVE")) {
            this.headers = new LinkedHashMap<>();
            this.body = null;
            return;
        }

        // Read the request headers
        this.headers = StreamUtil.readHeaders(clientInputStream);
        if (headers.containsKey("content-length")) {
            int contentLength = Integer.parseInt(headers.get("content-length"));
            this.body = StreamUtil.readRequestBody(clientInputStream, contentLength);
        } else {
            this.body = null;
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void log() {
        Logger.info(method
                + " "
                + uri
                + " "
                + protocol
                + "\n"
                + headers
                + "\n"
                + new String(body, StandardCharsets.UTF_8)
        );
    }

}
