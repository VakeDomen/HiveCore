package upr.famnit.components;

import java.util.Map;

public class Response {

    private final String protocol;
    private final int code;
    private final String text;
    private final Map<String, String> headers;
    private final byte[] body;

    public Response(String protocol, int code, String text, Map<String, String> headers) {
        this.protocol = protocol;
        this.code = code;
        this.text = text;
        this.headers = headers;
        this.body = null;
    }

    public Response(String protocol, int code, String text, Map<String, String> headers, byte[] body) {
        this.protocol = protocol;
        this.code = code;
        this.text = text;
        this.headers = headers;
        this.body = body;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }
}
