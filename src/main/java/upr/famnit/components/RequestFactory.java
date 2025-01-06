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

    public static Request AuthenticationResponse(String name) {
        return new Request(
                "HIVE",
                "AUTH",
                name,
                null,
                null
        );
    }
}
