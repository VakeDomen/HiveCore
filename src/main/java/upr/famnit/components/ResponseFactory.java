package upr.famnit.components;

import java.util.Map;

public class ResponseFactory {

    public static Response MethodNotAllowed() {
        return new Response("HTTP/1.1", 405, "Method Not Allowed", Map.of("Content-Length", "0", "Connection", "close"));
    }

    public static Response Ok() {
        return new Response("HTTP/1.1", 200, "OK", Map.of("Content-Length", "0", "Connection", "close"));
    }

    public static Response Ok(byte[] body) {
        return new Response("HTTP/1.1", 200, "OK", Map.of("Content-Length", "" + body.length, "Connection", "close"), body);
    }

    public static Response NotFound() {
        return new Response("HTTP/1.1", 404, "Not Found", Map.of("Content-Length", "0", "Connection", "close"));
    }

    public static Response Unauthorized() {
        return new Response("HTTP/1.1", 403, "Unauthorized", Map.of("Content-Length", "0", "Connection", "close"));
    }

    public static Response BadRequest() {
        return new Response("HTTP/1.1", 400, "Bad Request", Map.of("Content-Length", "0", "Connection", "close"));
    }
}
