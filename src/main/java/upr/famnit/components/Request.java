package upr.famnit.components;

import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

public class Request {

    private String protocol;
    private String method;
    private String uri;
    private Map<String, String> headers;
    private byte[] body;

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
        Logger.log("Request Headers: " + headers, LogLevel.info);
        int contentLength = Integer.parseInt(headers.get("Content-Length"));

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && contentLength > 0) {
            this.body = StreamUtil.readRequestBody(clientInputStream, contentLength);
            Logger.log("Read request body of length " + contentLength + " bytes.", LogLevel.info);
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

}
