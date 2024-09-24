package upr.famnit.util;

import upr.famnit.components.LogLevel;
import upr.famnit.components.Request;
import upr.famnit.components.Response;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static upr.famnit.util.Config.MESSAGE_CHUNK_BUFFER_SIZE;

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
    public static int getTotalLength(String protocol, String method, String uri, Map<String, String> headers, byte[] requestBody) {
        int totalLength = (method + " " + uri + " " + protocol + "\r\n").getBytes(StandardCharsets.UTF_8).length;

        if (protocol.equals("HIVE")) {
            return totalLength;
        }

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
     * Calculates the byte size of the request that would be created with the given data
     *
     * @param protocol      The HTTP protocol
     * @param code          The response code
     * @param text          The response text
     * @param headers       The response headers
     * @return              The number of content-length bytes
     */
    public static int getTotalLength(String protocol, int code, String text, Map<String, String> headers) {
        int totalLength = (protocol + " " + code + " " + text + "\r\n").getBytes(StandardCharsets.UTF_8).length;
        if (protocol.equals("HIVE")) {
            return totalLength;
        }

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                totalLength += (entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8).length;
            }
        }

        totalLength += "\r\n".getBytes(StandardCharsets.UTF_8).length;  // End of headers
        return totalLength;
    }


    /**
     * Sends an HTTP response through the given output stream.
     *
     * @param out     The output stream to send the request to.
     * @param response The request object containing the HTTP request details.
     * @throws IOException If an I/O error occurs.
     */
    public static void sendResponse(OutputStream out, Response response) throws IOException {
        DataOutputStream outStream = new DataOutputStream(out);

        // Write the request to node
        outStream.write((response.getProtocol() + " " + response.getCode() + " " + response.getText() + "\r\n").getBytes(StandardCharsets.UTF_8));

        if (response.getProtocol().equals("HIVE")) {
            outStream.flush();
            return;
        }

        if (response.getHeaders() != null && response.getHeaders().isEmpty()) {
            for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
                outStream.write((entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }

        outStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
        outStream.flush();
    }


    /**
     * Sends an HTTP request through the given output stream.
     *
     * @param out     The output stream to send the request to.
     * @param request The request object containing the HTTP request details.
     * @throws IOException If an I/O error occurs.
     */
    public static void sendRequest(OutputStream out, Request request) throws IOException {
        DataOutputStream outStream = new DataOutputStream(out);
        // Write content length for the node to read
        outStream.writeInt(StreamUtil.getTotalLength(
                request.getProtocol(),
                request.getMethod(),
                request.getUri(),
                request.getHeaders(),
                request.getBody()
        ));

        // Write the request to node
        outStream.write((request.getMethod() + " " + request.getUri() + " " + request.getProtocol() + "\r\n").getBytes(StandardCharsets.UTF_8));

        if (request.getProtocol().equals("HIVE")) {
            outStream.flush();
            return;
        }

        for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
            outStream.write((entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }

        outStream.write("\r\n".getBytes(StandardCharsets.UTF_8));  // End of headers
        if (request.getBody() != null && request.getBody().length > 0) {
            outStream.write(request.getBody());
        }
        outStream.flush();
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
        byte[] buffer = new byte[MESSAGE_CHUNK_BUFFER_SIZE];
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
        byte[] buffer = new byte[MESSAGE_CHUNK_BUFFER_SIZE];
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
        try {
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
        } catch (SocketTimeoutException e) {
            // Handle timeout case, maybe log or throw a custom exception
            throw new IOException("Read timed out", e);
        }
        if (b == -1 && buffer.size() == 0) {
            return null;
        }
        return buffer.toString(StandardCharsets.US_ASCII);
    }

    /**
     * Extracts the value associated with the specified key from a JSON-formatted byte array.
     *
     * @param key  The key whose associated value is to be returned.
     * @param body The JSON-formatted byte array.
     * @return The value associated with the key, or null if not found or parsing fails.
     */
    public static String getValueFromJSONBody(String key, byte[] body) {
        if (body == null || body.length == 0 || key == null || key.isEmpty()) {
            return null;
        }

        String jsonString = new String(body, StandardCharsets.UTF_8).trim();

        // Prepare possible key representations with single and double quotes
        String[] keyVariants = {
                "\"" + key + "\"",
                "'" + key + "'"
        };

        int keyIndex = -1;
        String matchedKey = null;

        // Find the key in the JSON string
        for (String keyVariant : keyVariants) {
            keyIndex = jsonString.indexOf(keyVariant);
            if (keyIndex != -1) {
                matchedKey = keyVariant;
                break;
            }
        }

        if (keyIndex == -1) {
            return null; // Key not found
        }

        // Find the colon after the key
        int colonIndex = jsonString.indexOf(':', keyIndex + matchedKey.length());
        if (colonIndex == -1) {
            return null; // Colon not found after key
        }

        // Skip whitespace after the colon
        int valueStart = colonIndex + 1;
        while (valueStart < jsonString.length() && Character.isWhitespace(jsonString.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= jsonString.length()) {
            return null; // No value found
        }

        // Check if the value starts with a quote
        char quoteChar = jsonString.charAt(valueStart);
        boolean isQuoted = quoteChar == '"' || quoteChar == '\'';

        if (isQuoted) {
            valueStart++; // Move past the opening quote
            int valueEnd = jsonString.indexOf(quoteChar, valueStart);
            if (valueEnd == -1) {
                return null; // Closing quote not found
            }
            return jsonString.substring(valueStart, valueEnd);
        } else {
            // Value is unquoted; read until next comma or closing brace
            int valueEnd = valueStart;
            while (valueEnd < jsonString.length()) {
                char c = jsonString.charAt(valueEnd);
                if (c == ',' || c == '}') {
                    break;
                }
                valueEnd++;
            }
            return jsonString.substring(valueStart, valueEnd).trim();
        }
    }


}
