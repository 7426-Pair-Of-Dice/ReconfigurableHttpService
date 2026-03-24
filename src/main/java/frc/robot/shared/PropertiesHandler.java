package frc.robot.shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class PropertiesHandler implements HttpHandler {
    private String defaultClassName = "frc.robot.shared.PropertiesHttpService" ;

    public String getContextPath() {
        return "/properties";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/configs")) {
            handleConfigsRequest(exchange);
            return;
        }
        if (path.endsWith("/staticFields")) {
            handleStaticFieldsRequest(exchange);
            return;
        }
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            PropertiesHttpService.javaStaticFields.clear();
            handleGetRequest(exchange);
        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            handlePostRequest(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    // New endpoint: /properties/staticFields returns PropertiesHttpService.javaStaticFields as JSON array
    void handleStaticFieldsRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        StringBuilder json = new StringBuilder();
        json.append("[");
        for (int i = 0; i < PropertiesHttpService.javaStaticFields.size(); i++) {
            if (i > 0) json.append(",");
            String line = PropertiesHttpService.javaStaticFields.get(i);
            // Escape tab and control characters for JSON
            String safeLine = line
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
            json.append("\"").append(safeLine).append("\"");
        }
        json.append("]");
        exchange.sendResponseHeaders(200, json.toString().getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    // New endpoint: /properties/configs returns JSON array of reconfigurable config class names
    void handleConfigsRequest(HttpExchange exchange) throws IOException {
        Set<Class<?>> reconfigurableConfigs = ReconfigurableConfig.findReconfigurableConfigs();
        StringBuilder json = new StringBuilder();
        json.append("[");
        boolean first = true;
        for (Class<?> configClass : reconfigurableConfigs) {
            if (!first) json.append(",");
            first = false;
            json.append("{\"name\":\"").append(escapeJson(configClass.getName())).append("\"}");
        }
        json.append("]");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, json.toString().getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    String getClassNameFromQuery(String query, String defaultClassName) {
        String className = defaultClassName ;

        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "className".equals(keyValue[0])) {
                className = decode(keyValue[1]);
                break;
                }
            }
        }
        if(ReconfigurableConfig.isContainReconfig(className)){
            return className ;
        }
        return defaultClassName;
    }

    void handleGetRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String className = getClassNameFromQuery(query, defaultClassName);
        try {
            Class<?> clazz = Class.forName(className);
            Field[] fields = clazz.getFields();
            StringBuilder json = new StringBuilder();
            json.append("[");
            boolean first = true;
            PropertiesHttpService.javaStaticFields.clear();
            for (Field field : fields) {
                if (isEditableType(field.getType())) {
                    if (!first) json.append(",");
                    first = false;
                    String name = field.getName();
                    String value = String.valueOf(field.get(null));
                    json.append("{\"name\":\"").append(escapeJson(name)).append("\",\"value\":\"").append(escapeJson(value)).append("\"}");
                    // Add to staticFields block, matching old printFooter
                    String typeName = field.getType().getName();
                    PropertiesHttpService.javaStaticFields.add("\tpublic static " + typeName + " " + name + " = " + value + ";");
                }
            }
            json.append("]");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, json.toString().getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (ClassNotFoundException | IllegalAccessException e) {
            Logger.error(e);
            exchange.sendResponseHeaders(500, -1);
        }
    }

    @SuppressWarnings("rawtypes")
    String buildHtmlForm(Class<?> clazz) {
        StringBuilder html = new StringBuilder();
        printHeader(clazz, html);

        Field[] fields = clazz.getFields();
        StringBuilder logChanges = new StringBuilder("\n// Saved from robot PropertiesHttpService for "+clazz.getName() + "\n") ;
        for (Field field : fields) {
            try {
                String name = field.getName();
                String value = String.valueOf(field.get(null));
                Class fieldType = field.getType() ;
                if(isEditableType(fieldType)){
                    printField(html, name, value);
                    logChanges.append(logField(name, value, fieldType));
                    PropertiesHttpService.javaStaticFields.add("\tpublic static " + fieldType + " " + name + " = " + value + ";");

                }
            } catch (IllegalAccessException e) {
                Logger.error(e);
            }
        }
        Logger.printf(logChanges.toString());

        printFooter(html);
        return html.toString();
    }

    void printHeader(Class<?> clazz, StringBuilder html) {
        html.append("<html><script>\n") 
            .append("  function solveMath(iconElement) {\n").append(
                    "    const input = iconElement.parentNode.firstElementChild;\n").append(
                    "    evaluateInput(input);\n").append(
                    "  }\n").append(
                    "\n").append(
                    "  function evaluateInput(input) {\n").append(
                    "    const expression = input.value;\n").append(
                    "    try {\n").append(
                    "      const result = Function('\"use strict\"; return (' + expression + ')')();\n").append(
                    "      input.value = result;\n").append(
                    "    } catch (err) {\n").append(
                    "      // Invalid expressions are ignored (or could optionally be cleared)\n" ).append(
                    "    }\n" ).append(
                    "  }\n" ).append(
                    "\n" )
            .append("</script><body>");
        html.append("<form method='POST' id=\"mathForm\">");
        html.append("<h1><INPUT NAME='className' VALUE='").append(clazz.getName());
        html.append("' size='").append(clazz.getName().length()).append("'></h1>");
        Set<Class<?>> reconfigurableConfigs = ReconfigurableConfig.findReconfigurableConfigs();
        html.append("<h2>Reconfigurable Configurations</h2>");
        html.append("<ul>");
        for (Class<?> configClass : reconfigurableConfigs) {
            html.append("<li><a href='/?className=").append(configClass.getName()).append("'>"); 
            html.append(configClass.getName()).append("</a></li>");
        }
        html.append("</ul>");

        html.append("<TABLE BORDER=1>");
    }


    void printFooter(StringBuilder html) {
        html.append("</TABLE><button type='submit'>Save</button>");
        html.append("</form>");
        html.append("<pre>\n") ;
        html.append("\t//----------------- BEGIN STATIC BLOCK -------- \n");
        for(String fString : PropertiesHttpService.javaStaticFields){
            html.append(fString).append("\n");
        }
        html.append("\t//----------------- END STATIC BLOCK -------- \n\n");
        html.append("</body><script> document.getElementById('mathForm').addEventListener('submit', function(e) {\n" ).append(
                    "    const inputs = document.querySelectorAll('.math-input');\n" ).append(
                    "    inputs.forEach(input => evaluateInput(input));\n" ).append(
                    "    // Allow the form to submit naturally afterward\n" ).append(
                    "  });")
        .append("</script></html>");
    }

    void printField(StringBuilder html, String name, String value) {
        html.append("<TR><TD><label>").append(name).append(": </label></TD>");
        html.append("<TD><DIV><input type='text' class=\"math-input\"  name='").append(name);
        html.append("' value='").append(value).append("'/>") ;
        html.append("  <span class=\"math-icon\" onclick=\"solveMath(this)\">🔢</span>\n") ;
        html.append("</DIV></TD></TR>");
    }

    @SuppressWarnings("rawtypes")      
    StringBuilder logField(String name, String value, Class fieldType){
        StringBuilder sb = new StringBuilder("public static ");
        sb.append(fieldType.getName()).append(" ");
        sb.append(name).append(" = ");
        sb.append(value).append("; \n");
        return sb;
    }

    @SuppressWarnings("rawtypes")
    boolean isEditableType(Class fieldType) {
       if(fieldType == int.class){
        return true ;
       }
       if(fieldType == double.class){
        return true ;
       }
       if(fieldType == boolean.class){
        return true ;
       }
       if(fieldType == String.class){
        return true ;
       }
       if(fieldType == float.class){
        return true ;
       }
       return false ;
    }

    void handlePostRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String className = getClassNameFromQuery(query, defaultClassName);
        String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        try {
            Class<?> clazz = Class.forName(className);

            StringBuilder logChanges = new StringBuilder("\n// Saved from robot PropertiesHttpService for "+clazz.getName() + "\n") ;
            // Expecting JSON array: [{"name":"fieldName","value":"fieldValue"}, ...]
            String json = body.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
                String[] items = json.split("(?<=\\}),\\s*(?=\\{)");
                for (String item : items) {
                    String name = extractJsonField(item, "name");
                    String value = extractJsonField(item, "value");
                    if (name != null) {
                        try {
                            Field field = clazz.getField(name);
                            if (field.getType() == int.class) {
                                field.setInt(null, Integer.parseInt(value));
                            } else if (field.getType() == double.class) {
                                field.setDouble(null, Double.parseDouble(value));
                            } else if (field.getType() == boolean.class) {
                                field.setBoolean(null, Boolean.parseBoolean(value));
                            } else if (field.getType() == String.class) {
                                field.set(null, value);
                            } else if (field.getType() == float.class) {
                                field.setFloat(null, Float.parseFloat(value));
                            }
                            logChanges.append(logField(name, value, field.getType()));

                        } catch (Exception e) {
                            Logger.error(e);
                        }
                    }
                }
                if (ReconfigurableConfig.class.isAssignableFrom(clazz)) {
                    ReconfigurableConfig instance = (ReconfigurableConfig) clazz.getDeclaredConstructor().newInstance();
                    instance.reconfigure();
                }
                // Repopulate javaStaticFields after update
                PropertiesHttpService.javaStaticFields.clear();
                Field[] fields = clazz.getFields();
                for (Field field : fields) {
                    if (isEditableType(field.getType())) {
                        String name = field.getName();
                        String value = String.valueOf(field.get(null));
                        String typeName = field.getType().getName();
                        PropertiesHttpService.javaStaticFields.add("\tpublic static " + typeName + " " + name + " = " + value + ";");
                    }
                }
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            String resp = "{\"status\":\"ok\"}";
            exchange.sendResponseHeaders(200, resp.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes(StandardCharsets.UTF_8));
            }
            Logger.printf(logChanges.toString());

        } catch (Exception e) {
            Logger.error(e);
            exchange.sendResponseHeaders(500, -1);
        }
    }
    // Utility for escaping JSON strings
    String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Utility for extracting a field from a JSON object string (very simple, not robust)
    String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        int start = idx + pattern.length();
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    String updateConfigFromFormData(String body) throws Exception, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        String className = "";
        Map<String, String> formData = Arrays.stream(body.split("&"))
            .map(pair -> pair.split("=", 2))
            .collect(Collectors.toMap(
                keyValue -> decode(keyValue[0]),
                keyValue -> keyValue.length > 1 ? decode(keyValue[1]) : ""
            ));
        Class reconfigClass = Class.forName(formData.get("className")) ;
        for (String key : formData.keySet()) {
            String value = formData.get(key);
            try {
                if(key.equals("className")){
                    className = value ;
                } else {
                    Field field = reconfigClass.getField(key);
                    if (field.getType() == int.class) {
                        field.setInt(null, Integer.parseInt(value));
                    } else if (field.getType() == double.class) {
                        field.setDouble(null, Double.parseDouble(value));
                    } else if (field.getType() == boolean.class) {
                        field.setBoolean(null, Boolean.parseBoolean(value));
                    } else if (field.getType() == String.class) {
                        field.set(null, value);
                    } else if (field.getType() == float.class) {
                        field.setFloat(null, Float.parseFloat(value));
                    }
                }
            } catch (Exception e) {
                Logger.error(e);
            }
            if (ReconfigurableConfig.class.isAssignableFrom(reconfigClass)) {
                ReconfigurableConfig instance = (ReconfigurableConfig) reconfigClass.getDeclaredConstructor().newInstance();
                instance.reconfigure();
            }
        }
        return className ;
    }

    String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    String getDefaultClassName() {
        return defaultClassName;
    }

    void setDefaultClassName(String defaultClassName) {
        this.defaultClassName = defaultClassName;
    }
}