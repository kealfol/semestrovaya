package chat.client.model;

import chat.client.config.ClientConfig;
import chat.client.controller.ChatController;
import chat.client.exception.ConnectionException;
import chat.common.CommandType;
import chat.common.Message;
import com.google.gson.Gson;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Network {
    private static final Logger LOGGER = LoggerFactory.getLogger(Network.class);

    private final ClientConfig config;
    private final Gson gson;
    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private ChatController controller;
    
    // --- НОВОЕ: Буфер для сообщений, которые пришли до открытия окна ---
    private final List<Message> delayedMessages = new ArrayList<>();

    public Network() {
        this.gson = new Gson();
        this.config = new ClientConfig();
        this.host = config.getServerHost();
        this.port = config.getServerPort();
    }

    public void connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), config.getConnectionTimeout());
            
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            LOGGER.info("Connected to server: {}:{}", host, port);
        } catch (IOException e) {
            LOGGER.error("Connection error", e);
            throw new ConnectionException("Failed to connect to server: " + host + ":" + port, e);
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // --- ОБНОВЛЕННЫЙ МЕТОД ---
    public void setController(ChatController controller) {
        this.controller = controller;
        
        // Как только окно открылось и контроллер пришел,
        // отдаем ему все сообщения, которые накопились в буфере
        synchronized (delayedMessages) {
            for (Message msg : delayedMessages) {
                Platform.runLater(() -> controller.handleMessage(msg));
            }
            delayedMessages.clear();
        }
    }

    public void startReading(Runnable onAuthOk, Consumer<String> onAuthError, Consumer<String> onRegOk) {
        new Thread(() -> {
            try {
                boolean isAuthenticated = false;
                while (true) {
                    String json = in.readUTF();
                    Message message = gson.fromJson(json, Message.class);

                    if (!isAuthenticated) {
                        if (message.getType() == CommandType.AUTH_OK) {
                            this.username = message.getMessage().split("\\s+")[0];
                            LOGGER.info("User authenticated: {}", this.username);
                            isAuthenticated = true;
                            onAuthOk.run();
                        } 
                        else if (message.getType() == CommandType.ERROR) {
                            onAuthError.accept(message.getMessage());
                        }
                        else if (message.getType() == CommandType.REG_OK) {
                            onRegOk.accept(message.getMessage());
                        }
                    } else {
                        if (controller != null) {
                            Platform.runLater(() -> controller.handleMessage(message));
                        } else {
                            synchronized (delayedMessages) {
                                delayedMessages.add(message);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Connection lost: {}", e.getMessage());
                if (controller != null) {
                    Platform.runLater(() -> controller.showError("Connection lost"));
                }
            } finally {
                close();
            }
        }).start();
    }

    public void sendMessage(Message message) {
        if (!isConnected()) {
            LOGGER.error("Attempt to send message without connection");
            return; 
        }
        try {
            out.writeUTF(gson.toJson(message));
            out.flush();
        } catch (IOException e) {
            LOGGER.error("Failed to send message", e);
            close();
        }
    }

    public void sendPublicMessage(String messageText) {
        Message message = new Message(CommandType.PUBLIC_MESSAGE, this.username, messageText);
        sendMessage(message);
    }

    public void sendAuthMessage(String login, String password) {
        Message message = new Message(CommandType.AUTH, "client", login + " " + password);
        sendMessage(message);
        LOGGER.info("Auth request sent for {}", login);
    }

    public void sendRegisterMessage(String login, String password) {
        Message message = new Message(CommandType.REGISTER, "client", login + " " + password);
        sendMessage(message);
        LOGGER.info("Registration request sent for {}", login);
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                LOGGER.info("Connection closed.");
            }
        } catch (IOException e) {
            LOGGER.error("Error closing connection", e);
        }
    }

    public String getUsername() {
        return username;
    }
}