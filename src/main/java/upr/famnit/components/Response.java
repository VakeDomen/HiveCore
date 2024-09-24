package upr.famnit.components;

import java.util.Map;

public class Response {

    private String protocol;
    private int code;
    private String text;
    private Map<String, String> headers;

    public Response(String protocol, int code, String text, Map<String, String> headers) {
        this.protocol = protocol;
        this.code = code;
        this.text = text;
        this.headers = headers;
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
}
