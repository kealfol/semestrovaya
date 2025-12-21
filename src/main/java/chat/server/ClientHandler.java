package chat.server;

import chat.common.CommandType;
import chat.common.Message;
import chat.common.validation.RegistrationValidator;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
    private static final long MESSAGE_DELAY_MS = 800;

    private final ServerApp server;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Gson gson;

    private String username;
    private long lastMessageTime = 0;
    private boolean running = true;

    public ClientHandler(ServerApp server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            if (authenticate()) {
                readMessages();
            }
        } catch (IOException e) {
            LOGGER.warn("Client {} disconnected", socket.getInetAddress());
        } finally {
            closeConnection();
        }
    }

    private boolean authenticate() throws IOException {
        while (true) {
            String json = in.readUTF();
            Message message = gson.fromJson(json, Message.class);

            if (message.getType() == CommandType.AUTH) {
                String[] parts = message.getMessage().split("\\s+", 2);

                if (parts.length == 2) {
                    String login = parts[0];
                    String password = parts[1];

                    if (server.getAuthService().authenticate(login, password)) {
                        if (server.isUserOnline(login)) {
                            sendMessage(CommandType.ERROR, "Server", "Пользователь уже в сети.");
                            LOGGER.warn("User {} tried to login, but already online.", login);
                            continue;
                        }
                        this.username = login;
                        sendMessage(CommandType.AUTH_OK, "Server", login);
                        server.subscribe(this);
                        return true;
                    }
                }
                sendMessage(CommandType.ERROR, "Server", "Неверный логин или пароль.");
            }
            else if (message.getType() == CommandType.REGISTER) {
                String[] parts = message.getMessage().split("\\s+", 2);

                if (parts.length == 2) {
                    String login = parts[0];
                    String password = parts[1];

                    String validationError = RegistrationValidator.validateRegistration(login, password);
                    if (validationError != null) {
                        sendMessage(CommandType.ERROR, "Server", validationError);
                        continue;
                    }

                    if (server.getAuthService().register(login, password)) {
                        sendMessage(CommandType.REG_OK, "Server", "Регистрация успешна! Пожалуйста, войдите в чат.");
                    } else {
                        sendMessage(CommandType.ERROR, "Server", "Логин '" + login + "' уже занят.");
                    }
                } else {
                    sendMessage(CommandType.ERROR, "Server", "Ошибка регистрации.");
                }
            }
            else {
                sendMessage(CommandType.ERROR, "Server", "Сначала нужно войти в чат.");
            }
        }
    }

    private void readMessages() throws IOException {
        while (running) {
            String json = in.readUTF();
            Message message = gson.fromJson(json, Message.class);

            switch (message.getType()) {
                case PUBLIC_MESSAGE:
                    handlePublicMessage(message);
                    break;
                case LOGOUT:
                    LOGGER.info("User {} requested logout", username);
                    running = false;
                    sendMessage(CommandType.ERROR, "Server", "logout_ack");
                    return;
                default:
                    LOGGER.warn("Unknown message type from user {}: {}", username, message.getType());
            }
        }
    }

    private void handlePublicMessage(Message message) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime < MESSAGE_DELAY_MS) {
            sendMessage(CommandType.ERROR, "Server", "Вы слишком быстры! Пожалуйста, подождите.");
            LOGGER.warn("User {} is spamming. Message blocked.", username);
            return;
        }
        lastMessageTime = currentTime;
        server.broadcastMessage(this.username, message.getMessage());
    }

    public void sendMessage(CommandType type, String sender, String text) {
        try {
            Message msg = new Message(type, sender, text);
            out.writeUTF(gson.toJson(msg));
        } catch (IOException e) {
            LOGGER.error("Failed to send message", e);
        }
    }

    public String getUsername() {
        return username;
    }

    private void closeConnection() {
        server.unsubscribe(this);
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
        }
    }
}