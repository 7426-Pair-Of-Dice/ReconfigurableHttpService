package frc.robot.shared;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;

class PropertiesHandlerTest {

    private PropertiesHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PropertiesHandler();
        PropertiesHttpService.javaStaticFields.clear();
        ReconfigurableConfig.addReconfigs(Arrays.asList(TestConfig.class));
        TestConfig.setTimes(0);
    }

    // -------------------------
    // Simple getters/setters
    // -------------------------

    @Test
    void testDefaultClassNameGetterSetter() {
        handler.setDefaultClassName("foo.Bar");
        assertEquals("foo.Bar", handler.getDefaultClassName());
    }

    // -------------------------
    // decode
    // -------------------------

    @Test
    void testDecode() {
        assertEquals("hello world", handler.decode("hello%20world"));
    }

    // -------------------------
    // isEditableType
    // -------------------------

    @Test
    void testIsEditableType() {
        assertTrue(handler.isEditableType(int.class));
        assertTrue(handler.isEditableType(double.class));
        assertTrue(handler.isEditableType(boolean.class));
        assertTrue(handler.isEditableType(String.class));
        assertTrue(handler.isEditableType(float.class));

        assertFalse(handler.isEditableType(long.class));
        assertFalse(handler.isEditableType(Object.class));
    }

    // -------------------------
    // logField
    // -------------------------

    @Test
    void testLogField() {
        String log = handler.logField("X", "5", int.class).toString();
        assertTrue(log.contains("public static int X = 5;"));
    }

    // -------------------------
    // getClassNameFromQuery
    // -------------------------

    @Test
    void testGetClassNameFromQuery_valid() {
        String query = "className=" + TestConfig.class.getName();
        String result = handler.getClassNameFromQuery(query, "default");
        assertEquals(TestConfig.class.getName(), result);
    }

    @Test
    void testGetClassNameFromQuery_invalidFallsBack() {
        String query = "className=not.allowed.Class";
        String result = handler.getClassNameFromQuery(query, "default");
        assertEquals("default", result);
    }

    @Test
    void testGetClassNameFromQuery_nullQuery() {
        String result = handler.getClassNameFromQuery(null, "default");
        assertEquals("default", result);
    }

    // -------------------------
    // buildHtmlForm + print*
    // -------------------------

    @Test
    void testBuildHtmlForm() {
        String html = handler.buildHtmlForm(TestConfig.class);

        assertTrue(html.contains("<form"));
        assertTrue(html.contains("INT_VAL"));
        assertTrue(html.contains("DOUBLE_VAL"));
        assertTrue(html.contains("STRING_VAL"));
        assertTrue(html.contains("Save"));

        // non-editable field excluded
        assertFalse(html.contains("NON_EDITABLE"));

        // static block populated
        assertFalse(PropertiesHttpService.javaStaticFields.isEmpty());
    }

    @Test
    void testPrintField() {
        StringBuilder sb = new StringBuilder();
        handler.printField(sb, "X", "10");
        assertTrue(sb.toString().contains("name='X'"));
        assertTrue(sb.toString().contains("value='10'"));
    }

    @Test
    void testPrintHeaderAndFooter() {
        StringBuilder sb = new StringBuilder();
        handler.printHeader(TestConfig.class, sb);
        handler.printFooter(sb);

        String html = sb.toString();
        assertTrue(html.contains("<html>"));
        assertTrue(html.contains("Reconfigurable Configurations"));
        assertTrue(html.contains("END STATIC BLOCK"));
    }

    // -------------------------
    // updateConfigFromFormData
    // -------------------------

    @Test
    void testUpdateConfigFromFormData() throws Exception {
        String body =
            "className=" + TestConfig.class.getName() +
            "&INT_VAL=10" +
            "&DOUBLE_VAL=3.14" +
            "&BOOL_VAL=false" +
            "&STRING_VAL=test" +
            "&FLOAT_VAL=2.5";

        String className = handler.updateConfigFromFormData(body);

        assertEquals(TestConfig.class.getName(), className);
        assertEquals(10, TestConfig.INT_VAL);
        assertEquals(3.14, TestConfig.DOUBLE_VAL);
        assertFalse(TestConfig.BOOL_VAL);
        assertEquals("test", TestConfig.STRING_VAL);
        assertEquals(2.5f, TestConfig.FLOAT_VAL);
    }

    // -------------------------
    // handleGetRequest
    // -------------------------

    @Test
    void testHandleGetRequest() throws Exception {
        HttpExchange exchange = mock(HttpExchange.class);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        when(exchange.getRequestURI())
            .thenReturn(new URI("/?className=" + TestConfig.class.getName()));
        when(exchange.getResponseBody()).thenReturn(os);
        when(exchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());

        handler.handleGetRequest(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertTrue(os.toString(StandardCharsets.UTF_8).contains("INT_VAL"), 
        os.toString() + " should containt INT_VAL");
    }

    // -------------------------
    // handlePostRequest
    // -------------------------

    @Test
    void testHandlePostRequest() throws Exception {
        HttpExchange exchange = mock(HttpExchange.class);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        String body = "className=" + TestConfig.class.getName() + "&INT_VAL=99";

        when(exchange.getRequestBody())
            .thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        when(exchange.getResponseBody()).thenReturn(os);
        when(exchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());

        handler.handlePostRequest(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        assertEquals(99, TestConfig.INT_VAL);
        assertTrue(os.toString(StandardCharsets.UTF_8).contains("Configuration updated successfully"));
    }

    // -------------------------
    // handle (dispatch)
    // -------------------------

    @Test
    void testHandleGetDispatch() throws Exception {
        HttpExchange exchange = mock(HttpExchange.class);
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI())
            .thenReturn(new URI("/?className=" + TestConfig.class.getName()));
        when(exchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        when(exchange.getResponseHeaders()).thenReturn(new com.sun.net.httpserver.Headers());

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }

    @Test
    void testHandleUnsupportedMethod() throws Exception {
        HttpExchange exchange = mock(HttpExchange.class);
        when(exchange.getRequestMethod()).thenReturn("PUT");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(405, -1);
    }
}
