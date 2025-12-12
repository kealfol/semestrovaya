package chat.server;

import chat.common.CommandType;
import chat.common.Message;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

    private final ServerApp server;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Gson gson;

    private String username;

    public ClientHandler(ServerApp server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.gson = new Gson();
    }

    public void start() {
        new Thread(() -> {
            try {
                if (authenticate()) {
                    readMessages();
                }
            } catch (IOException e) {
                LOGGER.warn("Клиент {} отключился", socket.getInetAddress());
            } finally {
                closeConnection();
            }
        }).start();
    }

    private boolean authenticate() throws IOException {
        while (true) {
            String json = in.readUTF();
            Message message = gson.fromJson(json, Message.class);

            // === ЛОГИКА АВТОРИЗАЦИИ ===
            if (message.getType() == CommandType.AUTH) {
                // Клиент шлет строку: "login password"
                String[] parts = message.getMessage().split("\\s+", 2);
                
                if (parts.length == 2) {
                    String login = parts[0];
                    String password = parts[1];

                    // Проверяем через БД
                    if (server.getAuthService().authenticate(login, password)) {
                        this.username = login;
                        // Клиенту нужно отправить AUTH_OK, чтобы он открыл окно чата
                        // В сообщении отправляем логин, чтобы клиент знал своё имя
                        sendMessage(CommandType.AUTH_OK, "Server", login);
                        server.subscribe(this);
                        return true;
                    }
                }
                sendMessage(CommandType.ERROR, "Server", "Неверный логин или пароль");
            } 
            
            // === ЛОГИКА РЕГИСТРАЦИИ ===
            else if (message.getType() == CommandType.REGISTER) {
                String[] parts = message.getMessage().split("\\s+", 2);
                
                if (parts.length == 2) {
                    String login = parts[0];
                    String password = parts[1];

                    // Пробуем записать в БД
                    if (server.getAuthService().register(login, password)) {
                        // Хак для клиента: отправляем тип ERROR, чтобы у него всплыло окно Alert
                        // Но текст пишем позитивный
                        sendMessage(CommandType.ERROR, "Server", "Регистрация успешна! Теперь войдите.");
                    } else {
                        sendMessage(CommandType.ERROR, "Server", "Логин '" + login + "' уже занят.");
                    }
                } else {
                    sendMessage(CommandType.ERROR, "Server", "Ошибка данных регистрации");
                }
            } 
            else {
                sendMessage(CommandType.ERROR, "Server", "Сначала нужно войти!");
            }
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            String json = in.readUTF();
            Message message = gson.fromJson(json, Message.class);

            if (message.getType() == CommandType.PUBLIC_MESSAGE) {
                server.broadcastMessage(this.username, message.getMessage());
            }
            else if (message.getType() == CommandType.LIST_REQUEST) {
                server.broadcastClientsList();
            }
        }
    }

    public void sendMessage(CommandType type, String sender, String text) {
        try {
            Message msg = new Message(type, sender, text);
            out.writeUTF(gson.toJson(msg));
        } catch (IOException e) {
            LOGGER.error("Ошибка отправки сообщения", e);
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
            // ignore
        }
    }
}