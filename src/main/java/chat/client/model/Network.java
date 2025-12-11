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
import java.util.function.Consumer;

public class Network {
    private static final Logger LOGGER = LoggerFactory.getLogger(Network.class);

    private final ClientConfig config;
    private final Gson gson;
    private final String host;
    private final int port;
    private int reconnectAttempts;
    private Socket socket;
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

    public void connect() {
        try {
            socket = new Socket();
            // Подключаемся с таймаутом самого подключения (если сервер выключен)
            socket.connect(new InetSocketAddress(host, port), config.getConnectionTimeout());
            
            // ВАЖНО: УБРАЛИ socket.setSoTimeout(5000);
            // Теперь клиент будет ждать ответа от сервера бесконечно долго и не вылетит.

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
                    String json = in.readUTF(); // Ждем сообщения от сервера (теперь без тайм-аута)
                    Message message = gson.fromJson(json, Message.class);

                    if (!isAuthenticated) {
                        if (message.getType() == CommandType.AUTH_OK) {
                            // Сервер присылает: "login успешно вошел" или просто логин
                            // Берем первое слово как имя
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
                LOGGER.warn("Соединение с сервером потеряно: {}", e.getMessage());
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
            // Если соединения нет, не пытаемся писать в поток, иначе будет ошибка
            LOGGER.error("Попытка отправить сообщение без подключения");
            return; 
        }
        try {
            out.writeUTF(gson.toJson(message));
            out.flush();
        } catch (IOException e) {
            LOGGER.error("Не удалось отправить сообщение", e);
            close();
        }
    }

    public void sendPublicMessage(String messageText) {
        Message message = new Message(CommandType.PUBLIC_MESSAGE, this.username, messageText);
        sendMessage(message);
    }

    public void sendAuthMessage(String login, String password) {
        // "client" - это отправитель, логин и пароль в теле сообщения
        Message message = new Message(CommandType.AUTH, "client", login + " " + password);
        sendMessage(message);
        LOGGER.info("Отправка запроса авторизации для {}", login);
    }

    public void sendRegisterMessage(String login, String password) {
        Message message = new Message(CommandType.REGISTER, "client", login + " " + password);
        sendMessage(message);
        LOGGER.info("Отправка запроса регистрации для {}", login);
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