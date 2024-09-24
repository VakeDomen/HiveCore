package upr.famnit.components;

public class RequestFactory {
    public static Request EmptyQueResponse() {
        return new Request(
                "HIVE",
                "POLL",
                "/",
                null,
                null
        );
    }
}
