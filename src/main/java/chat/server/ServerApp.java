package chat.server;

import chat.common.CommandType;
import chat.common.Message;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ServerApp {
    static {
        System.setProperty("logback.configurationFile", "src/main/resources/logbackserver.xml");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);
    private static final int MAX_CLIENTS = 40;

    private int port;
    private final List<ClientHandler> clients;
    private final AuthService authService;
    private final Gson gson;
    private final ExecutorService executorService;

    public ServerApp() {
        this.clients = new ArrayList<>();
        this.authService = new AuthService();
        this.gson = new Gson();
        this.executorService = Executors.newFixedThreadPool(MAX_CLIENTS);
        loadConfig();
    }

    public static void main(String[] args) {
        new ServerApp().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Server started on port: {}", port);
            LOGGER.info("Max connections limit: {}", MAX_CLIENTS);
            
            while (true) {
                Socket socket = serverSocket.accept();
                LOGGER.info("Client connected: {}", socket.getInetAddress());
                ClientHandler handler = new ClientHandler(this, socket);
                executorService.execute(handler);
            }
        } catch (IOException e) {
            LOGGER.error("Server error", e);
        } finally {
            executorService.shutdown();
        }
    }

    public synchronized void subscribe(ClientHandler client) {
        clients.add(client);
        broadcastClientsList();
        
        List<Message> history = authService.getLastMessages(20); 
        for (Message msg : history) {
            String json = gson.toJson(msg);
            client.sendMessage(CommandType.PUBLIC_MESSAGE, msg.getSender(), msg.getMessage());
        }
    }

    public synchronized void unsubscribe(ClientHandler client) {
        clients.remove(client);
        broadcastClientsList();
    }

    public synchronized void broadcastMessage(String sender, String message) {
        authService.saveMessage(sender, message);
        for (ClientHandler client : clients) {
            client.sendMessage(CommandType.PUBLIC_MESSAGE, sender, message);
        }
    }

    public synchronized void broadcastClientsList() {
        List<String> usernames = clients.stream()
                .map(ClientHandler::getUsername)
                .collect(Collectors.toList());
        
        String jsonUserList = gson.toJson(usernames);
        
        for (ClientHandler client : clients) {
            client.sendMessage(CommandType.CLIENT_MESSAGE, "Server", jsonUserList);
        }
    }

    public AuthService getAuthService() { return authService; }

    // --- НОВЫЙ МЕТОД: Проверка, что юзер онлайн ---
    public synchronized boolean isUserOnline(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername() != null && client.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                this.port = 8189;
                return;
            }
            prop.load(input);
            this.port = Integer.parseInt(prop.getProperty("server.port"));
        } catch (IOException e) {
            this.port = 8189;
        }
    }
}