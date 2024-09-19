package upr.famnit.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Util {
    /**
     * Calculates the byte size of the request that would be created with the civen data
     *
     * @param method        The HTTP method
     * @param uri           The request URI
     * @param headers       The request headers
     * @param requestBody   The request body in bytes
     * @return              The number of content-length bytes
     */
    public static int getTotalLength(String method, String uri, Map<String, String> headers, byte[] requestBody) {
        // Calculate the total length of the request (headers + body)
        int totalLength = (method + " " + uri + " HTTP/1.1\r\n").getBytes(StandardCharsets.UTF_8).length;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            totalLength += (entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8).length;
        }
        totalLength += "\r\n".getBytes(StandardCharsets.UTF_8).length;  // End of headers

        if (requestBody != null && requestBody.length > 0) {
            totalLength += requestBody.length;  // Add the length of the request body
        }
        return totalLength;
    }

    /**
     * Reads a line from the input stream.
     *
     * @param in The input stream.
     * @return The line read, or null if end of stream.
     * @throws IOException If an I/O error occurs.
     */
    public static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c;

        while ((c = in.read()) != -1) {
            if (c == '\r') {
                in.mark(1);
                int nextChar = in.read();
                if (nextChar != '\n' && nextChar != -1) {
                    in.reset();
                }
                break;
            } else if (c == '\n') {
                break;
            } else {
                buffer.write(c);
            }
        }

        if (c == -1 && buffer.size() == 0) {
            return null;
        }

        return buffer.toString(StandardCharsets.UTF_8.name());
    }
}
