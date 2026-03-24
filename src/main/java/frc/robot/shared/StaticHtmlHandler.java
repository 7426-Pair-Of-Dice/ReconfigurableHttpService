package frc.robot.shared;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StaticHtmlHandler implements HttpHandler {
    private final StaticHtmlProvider provider;

    public StaticHtmlHandler(StaticHtmlProvider provider) {
        this.provider = provider;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String contextPath = provider.getContextPath();
        String requestPath = exchange.getRequestURI().getPath();
        String relativePath = requestPath.substring(contextPath.length());
        if (relativePath.isEmpty() || relativePath.equals("/")) {
            relativePath = "/index.html";
        }
        String resourcePath = provider.getClasspathLocation() + relativePath;
        resourcePath = URLDecoder.decode(resourcePath, "UTF-8");
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            exchange.getResponseHeaders().add("Content-Type", getContentType(resourcePath));
            byte[] bytes = is.readAllBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
