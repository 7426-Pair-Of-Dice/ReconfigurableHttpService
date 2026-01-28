package frc.robot.shared;

import com.sun.net.httpserver.HttpServer;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PropertiesHttpService {

    // Add math for us
    private static final int PORT = 8081;
    static List<String> javaStaticFields = new ArrayList<String>() ;

    private PropertiesHttpService() {
    }   
    public static void main(String[] args) throws IOException {
        startService();
    }

    public static void startService() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new PropertiesHandler());
        server.setExecutor(null);
        printIps();
        server.start();
    }

     public static void printIps() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        Logger.println("http://" + inetAddress.getHostAddress() + ":" + PORT);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class PropertiesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                javaStaticFields.clear();
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
            String className = getClassNameFromQuery(query, "frc.robot.subsystems.climber.Climber");

            try {
                Class<?> clazz = Class.forName(className);
                String response = buildHtmlForm(clazz);

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
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
                        javaStaticFields.add("\tpublic static " + fieldType + " " + name + " = " + value + ";");

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
            for(String fString : javaStaticFields){
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
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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

        private String buildHtmlForm2(Class clazz) {
            StringBuilder html = new StringBuilder();
            html.append("<html><body>");
            html.append("<h1>Edit Configuration</h1>");
            html.append("<form method='POST'>");

            Field[] fields = clazz.getFields();
            for (Field field : fields) {
                try {
                    String name = field.getName();
                    String value = String.valueOf(field.get(null));
                    html.append("<label>").append(name).append(": </label>");
                    html.append("<input type='text' name='").append(name).append("' value='").append(value).append("'/><br/><br/>");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            html.append("<button type='submit'>Save</button>");
            html.append("</form>");
            html.append("</body></html>");
            return html.toString();
        }
    }


}