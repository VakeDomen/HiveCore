package upr.famnit.components;

public class RequestFactory {
    public static Request EmptyQueResponse() {
        return new Request(
                "HIVE",
                "PONG",
                "/",
                null,
                null
        );
    }
}
