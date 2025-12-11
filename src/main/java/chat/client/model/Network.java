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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.function.Consumer;

public class Network {
    private static final Logger LOGGER = LoggerFactory.getLogger(Network.class);

    private final ClientConfig config;
    private final Gson gson;
    private final String host;
    private final int port;
    private int reconnectAttempts;
    private Socket socket;
    private boolean isConnected = false;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private ChatController controller;


    public Network() {
        this.gson = new Gson();
        this.config = new ClientConfig();
        this.host = config.getServerHost();
        this.port = config.getServerPort();
        this.reconnectAttempts = 0;
    }

    // Опционально
    public boolean connectWithRetry() {
        int maxAttempts = config.getReconnectAttempts();
        while (reconnectAttempts < maxAttempts) {
            try {
                connect();
                isConnected = true;
                reconnectAttempts = 0;
                LOGGER.info("Успешное подключение к серверу");
                return true;
            } catch (ConnectionException e) {
                reconnectAttempts++;
                LOGGER.warn("Попытка подключения {} из {} неудачна", reconnectAttempts, maxAttempts);

                if (reconnectAttempts < maxAttempts) {
                    try {
                        Thread.sleep(config.getReconnectDelay());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    // Используется
    public void connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), config.getConnectionTimeout());
            socket.setSoTimeout(5000);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            LOGGER.info("Установлено соединение с сервером: {}:{}", host, port);
        } catch (IOException e) {
            LOGGER.error("Ошибка подключения", e);
            throw new ConnectionException("Не удалось подключиться к серверу: " + host + ":" + port, e);
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void setController(ChatController controller) {
        this.controller = controller;
    }

    public void startReading(Runnable onAuthOk, Consumer<String> onAuthError) {
        new Thread(() -> {
            try {
                boolean isAuthenticated = false;
                while (true) {
                    String json = in.readUTF();
                    Message message = gson.fromJson(json, Message.class);

                    if (!isAuthenticated) {
                        if (message.getType() == CommandType.AUTH_OK) {
                            this.username = message.getMessage().split("\\s+")[0];
                            LOGGER.info("Успешная аутентификация пользователя: {}", this.username);
                            isAuthenticated = true;
                            onAuthOk.run();
                        } else if (message.getType() == CommandType.ERROR) {
                            onAuthError.accept(message.getMessage());
                        }
                    } else {
                        if (controller != null) {
                            Platform.runLater(() -> controller.handleMessage(message));
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Соединение потеряно", e);
                if (controller != null) {
                    Platform.runLater(() -> controller.showError("Соединение с сервером потеряно"));
                }
            } finally {
                close();
            }
        }).start();
    }

    public void sendMessage(Message message) {
        if (!isConnected()) {
            throw new ConnectionException("Нет соединения с сервером");
        }
        try {
            out.writeUTF(gson.toJson(message));
            out.flush();
        } catch (IOException e) {
            LOGGER.error("Не удалось отправить сообщение", e);
            isConnected = false;
            throw new ConnectionException("Не удалось отправить сообщение", e);
        }
    }

    public void sendPublicMessage(String messageText) {
        Message message = new Message(CommandType.PUBLIC_MESSAGE, this.username, messageText);
        sendMessage(message);
        LOGGER.info("Пользователь '{}' отправил публичное сообщение", this.username);
    }

    public void sendAuthMessage(String login, String password) {
        Message message = new Message(CommandType.AUTH, "client", login + " " + password);
        sendMessage(message);
        LOGGER.info("Пользователь '{}' пытается войти", login);
    }

    public void sendRegisterMessage(String login, String password) {
        Message message = new Message(CommandType.REGISTER, "client", login + " " + password);
        sendMessage(message);
        LOGGER.info("Пользователь '{}' пытается зарегистрироваться", login);
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                LOGGER.info("Соединение закрыто.");
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка при закрытии соединения", e);
        }
    }

    public String getUsername() {
        return username;
    }
}