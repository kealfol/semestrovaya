package chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ServerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);
    private int port;
    private final List<ClientHandler> clients;
    private final AuthService authService;

    public ServerApp() {
        this.clients = new ArrayList<>();
        this.authService = new AuthService();
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

    public synchronized void subscribe(ClientHandler client) {
        clients.add(client);
    }

    public synchronized void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }

    public synchronized void broadcastMessage(String sender, String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(sender, message);
        }
    }

    public AuthService getAuthService() { return authService; }
}