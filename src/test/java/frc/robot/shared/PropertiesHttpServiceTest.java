package frc.robot.shared;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;

class PropertiesHttpServiceTest implements ReconfigurableConfig{

    private PropertiesHandler propertiesHandler;
    private HttpExchange exchange;
    public static int TEST = 0 ;

    @BeforeEach
    void setUp() {
        propertiesHandler = new PropertiesHandler();
        exchange = mock(HttpExchange.class);
    }

    @Test
    void testHandlePostRequest() throws Exception {
        // Mocking the HttpExchange
        when(exchange.getRequestMethod()).thenReturn("POST");
        assertNotEquals(999, TestConfig.INT_VAL);

        String requestBody = "[{\"name\":\"INT_VAL\",\"value\":\"999\"}]";
        when(exchange.getRequestBody()).thenReturn(new java.io.ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);
        when(exchange.getResponseHeaders()).thenReturn(mock(com.sun.net.httpserver.Headers.class));
        URI uri = mock(URI.class);
        when(uri.getQuery()).thenReturn("className=" + TestConfig.class.getName());
        when(uri.getPath()).thenReturn("/properties"); // mock getPath()
        when(exchange.getRequestURI()).thenReturn(uri);

        // Calling the method
        propertiesHandler.handle(exchange);

        // Verifying the response headers and body
        verify(exchange.getResponseHeaders()).set("Content-Type", "application/json; charset=UTF-8");
        verify(exchange).sendResponseHeaders(200, os.toString().getBytes(StandardCharsets.UTF_8).length);
        String response = os.toString();
        assertEquals("{\"status\":\"ok\"}", response, "Response should indicate success, but was: " + response);
        assertEquals(999, TestConfig.INT_VAL);
        assertEquals("1 times", TestConfig.getReconfigureTimes());
    }

    @Test
    void testHandleUnsupportedMethod() throws Exception {
        // Mocking the HttpExchange
        when(exchange.getRequestMethod()).thenReturn("PUT");
        URI uri = mock(URI.class);
        when(uri.getQuery()).thenReturn("className=" + TestConfig.class.getName());
        when(uri.getPath()).thenReturn("/properties"); // mock getPath()
        when(exchange.getRequestURI()).thenReturn(uri);

        // Calling the method
        propertiesHandler.handle(exchange);

        // Verifying the response headers
        verify(exchange).sendResponseHeaders(405, -1); // Method Not Allowed
    }

    @Override
    public void reconfigure() {
        // TODO Auto-generated method stub
    }
}