package upr.famnit.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class StreamUtil {

    /**
     * Calculates the byte size of the request that would be created with the given data
     *
     * @param method        The HTTP method
     * @param uri           The request URI
     * @param headers       The request headers
     * @param requestBody   The request body in bytes
     * @return              The number of content-length bytes
     */
    public static int getTotalLength(String method, String uri, Map<String, String> headers, byte[] requestBody) {
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
     * Reads the request headers from the input stream.
     * Does not check if the structure is correct, but reads lines
     * from the input stream and parses, assuming the content is
     * HTTP headers.
     *
     * @param inputStream The client's input stream.
     * @return A map of header names and values.
     * @throws IOException If an I/O error occurs.
     */
    public static Map<String, String> readHeaders(InputStream inputStream) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        String headerLine;
        while ((headerLine = StreamUtil.readLine(inputStream)) != null && !headerLine.isEmpty()) {
            int separatorIndex = headerLine.indexOf(":");
            if (separatorIndex != -1) {
                String headerName = headerLine.substring(0, separatorIndex).trim();
                String headerValue = headerLine.substring(separatorIndex + 1).trim();
                headers.put(headerName, headerValue);
            }
        }
        return headers;
    }



    /**
     * Reads the request body from the input stream.
     * Does not check if the structure is correct, but reads lines
     * from the input stream and parses, assuming the content is
     * HTTP body.
     *
     * @param inputStream   The client's input stream.
     * @param contentLength The length of the content to read.
     * @return A byte array containing the request body.
     * @throws IOException If an I/O error occurs.
     */
    public static byte[] readRequestBody(InputStream inputStream, int contentLength) throws IOException {
        byte[] requestBody = new byte[contentLength];
        int totalBytesRead = 0;
        while (totalBytesRead < contentLength) {
            int bytesRead = inputStream.read(requestBody, totalBytesRead, contentLength - totalBytesRead);
            if (bytesRead == -1) {
                // Client closed the connection
                throw new IOException("Client closed the connection while reading the request body.");
            }
            totalBytesRead += bytesRead;
        }
        return requestBody;
    }

    /**
     * Reads a chunked response body from the input stream
     * and forwards it to the output stream.
     *
     * @param in  The input stream from the node.
     * @param out The output stream to the client.
     * @throws IOException If an I/O error occurs.
     */
    public static void readAndForwardChunkedBody(InputStream in, OutputStream out) throws IOException {
        Logger.log("Receiving chunked response...", LogLevel.network);
        while (true) {
            // Read the chunk size line
            String chunkSizeLine = StreamUtil.readLine(in);
            if (chunkSizeLine == null) {
                throw new IOException("Unexpected end of stream while reading chunk size");
            }
//            Logger.log("Chunk size line: " + chunkSizeLine, LogLevel.network);
            out.write((chunkSizeLine + "\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(chunkSizeLine.trim(), 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size: " + chunkSizeLine);
            }

            if (chunkSize == 0) {
                // End of chunks
                Logger.log("End of chunked response.", LogLevel.network);
                // Read and forward any trailing headers
                String line;
                while ((line = StreamUtil.readLine(in)) != null && !line.isEmpty()) {
                    Logger.log("Trailing header: " + line, LogLevel.network);
                    out.write((line + "\r\n").getBytes(StandardCharsets.US_ASCII));
                }
                out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
                break;
            }

            // Read the chunk data
            byte[] chunkData = readBytes(in, chunkSize);
            // Write the chunk data to the output
            out.write(chunkData);
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
            out.flush();

            // Read the trailing CRLF after the chunk data
            String crlf = StreamUtil.readLine(in);
            if (crlf == null || !crlf.isEmpty()) {
                throw new IOException("Expected CRLF after chunk data");
            }
        }
    }

    /**
     * Reads a fixed-length response body from the input stream and
     * forwards it to the output stream.
     *
     * @param in            The input stream from the node.
     * @param out           The output stream to the client.
     * @param contentLength The length of the content to read.
     * @throws IOException If an I/O error occurs.
     */
    public static void readAndForwardFixedLengthBody(InputStream in, OutputStream out, int contentLength) throws IOException {
        Logger.log("Receiving fixed length response...", LogLevel.network);
        byte[] buffer = new byte[8192];
        int totalBytesRead = 0;
        int bytesRead;
        while (totalBytesRead < contentLength) {
            bytesRead = in.read(buffer, 0, Math.min(buffer.length, contentLength - totalBytesRead));
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of stream while reading response body");
            }
            out.write(buffer, 0, bytesRead);
            out.flush();
            totalBytesRead += bytesRead;
        }
    }

    /**
     * Reads a response body of unknown length from the input stream and
     * forwards it to the output stream.
     *
     * @param in  The input stream from the node.
     * @param out The output stream to the client.
     * @throws IOException If an I/O error occurs.
     */
    public static void readAndForwardUntilEOF(InputStream in, OutputStream out) throws IOException {
        Logger.log("Receiving unknown length response...", LogLevel.network);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
        }
    }

    /**
     * Reads a specific number of bytes from the input stream.
     *
     * @param in     The input stream.
     * @param length The number of bytes to read.
     * @return A byte array containing the data read.
     * @throws IOException If an I/O error occurs.
     */
    public static byte[] readBytes(InputStream in, int length) throws IOException {
        byte[] data = new byte[length];
        int totalBytesRead = 0;
        while (totalBytesRead < length) {
            int bytesRead = in.read(data, totalBytesRead, length - totalBytesRead);
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of stream while reading data");
            }
            totalBytesRead += bytesRead;
        }
        return data;
    }

    /**
     * Reads a line from the input stream.
     *
     * @param in The input stream.
     * @return The line read, or null if the end of the stream is reached.
     * @throws IOException If an I/O error occurs.
     */
    public static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                int next = in.read();
                if (next == '\n') {
                    break;
                } else {
                    buffer.write(b);
                    if (next != -1) {
                        buffer.write(next);
                    }
                }
            } else if (b == '\n') {
                break;
            } else {
                buffer.write(b);
            }
        }
        if (b == -1 && buffer.size() == 0) {
            return null;
        }
        return buffer.toString(StandardCharsets.US_ASCII);
    }
}
