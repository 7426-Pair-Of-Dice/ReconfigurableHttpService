package frc.robot.shared;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;

class PropertiesHttpServiceTest {

    private PropertiesHandler propertiesHandler;
    private HttpExchange exchange;

    @BeforeEach
    void setUp() {
        propertiesHandler = new PropertiesHandler();
        exchange = mock(HttpExchange.class);
    }

    @Test
    void testHandlePostRequest() throws Exception {
        // Mocking the HttpExchange
        when(exchange.getRequestMethod()).thenReturn("POST");
        String requestBody = "className=frc.robot.shared.TestConfig&key2=value2";
        when(exchange.getRequestBody()).thenReturn(new java.io.ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);
        when(exchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));

        // Calling the method
        propertiesHandler.handle(exchange);

        // Verifying the response headers and body
        verify(exchange.getResponseHeaders()).set("Content-Type", "text/html; charset=UTF-8");
        verify(exchange).sendResponseHeaders(200, os.toString().getBytes(StandardCharsets.UTF_8).length);
        String response = os.toString();
        assertEquals(true, response.contains("Configuration updated successfully!"));
    }

    @Test
    void testHandleUnsupportedMethod() throws Exception {
        // Mocking the HttpExchange
        when(exchange.getRequestMethod()).thenReturn("PUT");

        // Calling the method
        propertiesHandler.handle(exchange);

        // Verifying the response headers
        verify(exchange).sendResponseHeaders(405, -1); // Method Not Allowed
    }
}