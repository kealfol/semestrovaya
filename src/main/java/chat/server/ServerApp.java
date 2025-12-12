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
import java.util.stream.Collectors;

public class ServerApp {
    // Статическая инициализация конфига логера
    static {
        System.setProperty("logback.configurationFile", "src/main/resources/logbackserver.xml");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);

    private int port;
    private final List<ClientHandler> clients;
    private final AuthService authService;
    private final Gson gson;

    public ServerApp() {
        this.clients = new ArrayList<>();
        this.authService = new AuthService();
        this.gson = new Gson();
        loadConfig();
    }

    public static void main(String[] args) {
        new ServerApp().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Сервер запущен на порту: {}", port);
            while (true) {
                Socket socket = serverSocket.accept();

                LOGGER.info("Клиент подключился: {}", socket.getInetAddress());
                new ClientHandler(this, socket).start();
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка сервера", e);
        }
    }

    // --- Управление клиентами ---

    public synchronized void subscribe(ClientHandler client) {
        clients.add(client);
        broadcastClientsList(); // Обновляем список у всех при входе
    }

    public synchronized void unsubscribe(ClientHandler client) {
        clients.remove(client);
        broadcastClientsList(); // Обновляем список у всех при выходе
    }

    public synchronized void broadcastMessage(String sender, String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(CommandType.PUBLIC_MESSAGE, sender, message);
        }
    }

    // Метод для отправки списка пользователей всем подключенным
    public synchronized void broadcastClientsList() {
        // Собираем список имен: ["user1", "user2"]
        List<String> usernames = clients.stream()
                .map(ClientHandler::getUsername)
                .collect(Collectors.toList());
        
        String jsonUserList = gson.toJson(usernames);
        
        // Отправляем специальный тип сообщения CLIENT_MESSAGE (как ждет клиент)
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