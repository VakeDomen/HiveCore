package upr.famnit.components;

public class ResponseFactory {

    public static Response MethodNotAllowed() {
        return new Response("HTTP/1.1", 405, "Method Not Allowed", null);
    }

}
