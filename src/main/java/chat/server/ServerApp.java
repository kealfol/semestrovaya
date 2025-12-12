package chat.server;

import chat.common.CommandType;
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
    
    // Лимит одновременных клиентов
    private static final int MAX_CLIENTS = 40;

    private int port;
    private final List<ClientHandler> clients;
    private final AuthService authService;
    private final Gson gson;
    
    // Пул потоков (Менеджер)
    private final ExecutorService executorService;

    public ServerApp() {
        this.clients = new ArrayList<>();
        this.authService = new AuthService();
        this.gson = new Gson();
        // Создаем пул на 40 потоков
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
                // Ждем подключения
                Socket socket = serverSocket.accept();
                LOGGER.info("Client connected: {}", socket.getInetAddress());
                
                // Создаем обработчик
                ClientHandler handler = new ClientHandler(this, socket);
                
                // ВМЕСТО handler.start() ОТДАЕМ ЕГО В ПУЛ
                // Если мест нет, он будет ждать в очереди
                executorService.execute(handler);
            }
        } catch (IOException e) {
            LOGGER.error("Server error", e);
        } finally {
            // При остановке сервера закрываем пул
            executorService.shutdown();
        }
    }

    public synchronized void subscribe(ClientHandler client) {
        clients.add(client);
        broadcastClientsList();
    }

    public synchronized void unsubscribe(ClientHandler client) {
        clients.remove(client);
        broadcastClientsList();
    }

    public synchronized void broadcastMessage(String sender, String message) {
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