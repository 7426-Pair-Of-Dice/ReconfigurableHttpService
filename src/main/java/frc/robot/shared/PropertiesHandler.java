package frc.robot.shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PropertiesHandler implements HttpHandler {
    
    private String defaultClassName = "frc.robot.shared.PropertiesHttpService" ;


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            PropertiesHttpService.javaStaticFields.clear();
            handleGetRequest(exchange);
        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            handlePostRequest(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    private String getClassNameFromQuery(String query, String defaultClassName) {
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

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String className = getClassNameFromQuery(query, defaultClassName);

        try {
            Class<?> clazz = Class.forName(className);
            String response = buildHtmlForm(clazz);

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } catch (ClassNotFoundException e) {
            Logger.error(e);
            exchange.sendResponseHeaders(500, -1); // Internal Server Error
        }
    }

    @SuppressWarnings("rawtypes")
    private String buildHtmlForm(Class<?> clazz) {
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

    private void printHeader(Class<?> clazz, StringBuilder html) {
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
        List<Class<?>> reconfigurableConfigs = ReconfigurableConfig.findReconfigurableConfigs();
        html.append("<h2>Reconfigurable Configurations</h2>");
        html.append("<ul>");
        for (Class<?> configClass : reconfigurableConfigs) {
            html.append("<li><a href='/?className=").append(configClass.getName()).append("'>"); 
            html.append(configClass.getName()).append("</a></li>");
        }
        html.append("</ul>");

        html.append("<TABLE BORDER=1>");
    }


    private void printFooter(StringBuilder html) {
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

    private void printField(StringBuilder html, String name, String value) {
        html.append("<TR><TD><label>").append(name).append(": </label></TD>");
        html.append("<TD><DIV><input type='text' class=\"math-input\"  name='").append(name);
        html.append("' value='").append(value).append("'/>") ;
        html.append("  <span class=\"math-icon\" onclick=\"solveMath(this)\">🔢</span>\n") ;
        html.append("</DIV></TD></TR>");
    }

    @SuppressWarnings("rawtypes")      
    private StringBuilder logField(String name, String value, Class fieldType){
        StringBuilder sb = new StringBuilder("public static ");
        sb.append(fieldType.getName()).append(" ");
        sb.append(name).append(" = ");
        sb.append(value).append("; \n");
        return sb;
    }

    @SuppressWarnings("rawtypes")
    private boolean isEditableType(Class fieldType) {
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

    @SuppressWarnings("resource")
    private void handlePostRequest(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        String body = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        String className = "" ;
        try {
            className = updateConfigFromFormData(body);
        } catch (Exception e) {
            Logger.error(e);
        }

        String response = "<html><head><meta http-equiv='refresh' content='3;url=/?className="
                        + className + "'></head><body><h1>Configuration updated successfully!</h1><a href='/?className="
                        + className + "'>Go back</a>" 
                        + "<div id='countdown'>3</div>"
                        + "<script>"
                        + "var countdown = 3;"
                        + "var countdownElement = document.getElementById('countdown');"
                        + "var interval = setInterval(function() {"
                        + "    countdown--;"
                        + "    countdownElement.textContent = countdown;"
                        + "    if (countdown <= 0) {"
                        + "        clearInterval(interval);"
                        + "    }"
                        + "}, 1000);"
                        + "</script></body></html>";
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String updateConfigFromFormData(String body) throws Exception, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
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

    private String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public String getDefaultClassName() {
        return defaultClassName;
    }

    public void setDefaultClassName(String defaultClassName) {
        this.defaultClassName = defaultClassName;
    }
}