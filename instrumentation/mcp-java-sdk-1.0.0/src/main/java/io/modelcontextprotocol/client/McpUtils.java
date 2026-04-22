package io.modelcontextprotocol.client;

public class McpUtils {

    /**
     * Extracts the scheme from a given URI string.
     * The scheme is the part of the URI before the first colon (:).
     *
     * @param uri
     * @return the scheme of the URI, or "unknown" if the URI is null, empty, or does not contain a colon.
     *
     * Example:
     * "https://example.com/weather" -> "https"
     * "file:///path/to/file" -> "file"
     * "custom-scheme://some/resource" -> "custom-scheme"
     */

    public static String extractScheme(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "unknown";
        }
        int colonIndex = uri.indexOf(':');
        return colonIndex > 0
                ? uri.substring(0, colonIndex)
                : "unknown";
    }
}
