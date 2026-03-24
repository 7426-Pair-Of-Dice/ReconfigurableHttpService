package frc.robot.shared;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class PropertiesHttpService implements ReconfigurableConfig {

    // Add math for us
    public static int PORT = 7426;
    public static String TEST = "Hello World" ;
    private static HttpServer server = null ;
    static List<String> javaStaticFields = new ArrayList<String>() ;

    public PropertiesHttpService() {
    }  
    
    public PropertiesHttpService(int port) {
        PORT = port ;
    }
    public static void main(String[] args)  {
        try {
            startService();
        } catch (IOException ex) {
            System.getLogger(PropertiesHttpService.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    public static void startService() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        // Serve all StaticHtmlProvider implementations
        for (Class<?> providerClass : ClassScanner.findImplementations(StaticHtmlProvider.class)) {
            try {
                StaticHtmlProvider provider = (StaticHtmlProvider) providerClass.getDeclaredConstructor().newInstance();
                server.createContext(provider.getContextPath() + "/", new StaticHtmlHandler(provider));
            } catch (Exception e) {
                Logger.error(e);
            }
        }
        // Serve properties handler at /properties
        PropertiesHandler propertiesHandler = new PropertiesHandler();
        server.createContext(propertiesHandler.getContextPath(), propertiesHandler);

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
           Logger.error(e) ;
        }
    }

    @Override
    public void reconfigure() {
        if(server != null) {
            server.stop(0);
            try {
                startService();
            } catch (IOException e) {
                Logger.error(e) ;
            }
        }

        
    }
}