package upr.famnit.components;

import upr.famnit.util.LogLevel;
import upr.famnit.util.Logger;
import upr.famnit.util.StreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Request {
    private String method;
    private String uri;
    private Map<String, String> headers;
    private byte[] body;

    public Request(String method, String uri, Map<String, String> headers, byte[] body) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
    }

    public Request(InputStream clientInputStream) throws IOException {
        // Read the client's request line
        String requestLine = StreamUtil.readLine(clientInputStream);
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Received empty request from client. ");
        }

        // Parse the request line
        String[] requestParts = requestLine.split(" ");
        this.method = requestParts[0];
        this.uri = requestParts[1];
        Logger.log("Request Line: " + requestLine, LogLevel.info);

        // Read the request headers
        this.headers = StreamUtil.readHeaders(clientInputStream);
        Logger.log("Request Headers: " + headers, LogLevel.info);
        int contentLength = Integer.parseInt(headers.get("Content-Length"));

        // Read the request body if present
        this.body = null;
        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && contentLength > 0) {
            body = StreamUtil.readRequestBody(clientInputStream, contentLength);
            Logger.log("Read request body of length " + contentLength + " bytes.", LogLevel.info);
        }
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
