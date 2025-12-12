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
    
    // Минимальная задержка между сообщениями (в миллисекундах)
    // 800 мс = чуть меньше секунды. Быстрее писать руками сложно, только спамить.
    private static final long MESSAGE_DELAY_MS = 900;

    private final ServerApp server;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Gson gson;

    private String username;
    private long lastMessageTime = 0; // Время последнего сообщения

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
                LOGGER.warn("Client {} disconnected", socket.getInetAddress());
            } finally {
                closeConnection();
            }
        }).start();
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
                        this.username = login;
                        sendMessage(CommandType.AUTH_OK, "Server", login);
                        server.subscribe(this);
                        return true;
                    }
                }
                sendMessage(CommandType.ERROR, "Server", "Invalid login or password");
            } 
            else if (message.getType() == CommandType.REGISTER) {
                String[] parts = message.getMessage().split("\\s+", 2);
                
                if (parts.length == 2) {
                    String login = parts[0];
                    String password = parts[1];

                    if (server.getAuthService().register(login, password)) {
                        sendMessage(CommandType.ERROR, "Server", "Registration successful! Please login.");
                    } else {
                        sendMessage(CommandType.ERROR, "Server", "Login '" + login + "' is already taken.");
                    }
                } else {
                    sendMessage(CommandType.ERROR, "Server", "Registration data error");
                }
            } 
            else {
                sendMessage(CommandType.ERROR, "Server", "Authentication required first");
            }
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            String json = in.readUTF();
            Message message = gson.fromJson(json, Message.class);
            
            if (message.getType() == CommandType.PUBLIC_MESSAGE) {
                // --- ЗАЩИТА ОТ СПАМА ---
                long currentTime = System.currentTimeMillis();
                
                // Если прошло меньше времени, чем задано в задержке
                if (currentTime - lastMessageTime < MESSAGE_DELAY_MS) {
                    // Отправляем предупреждение ЛИЧНО этому пользователю
                    sendMessage(CommandType.ERROR, "Server", "Too fast! Please do not spam.");
                    LOGGER.warn("User {} is spamming. Message blocked.", username);
                    
                    // continue означает "пропустить этот цикл" -> сообщение не уйдет в общий чат
                    continue; 
                }
                
                // Если всё ок, обновляем время последнего сообщения
                lastMessageTime = currentTime;
                
                // И рассылаем всем
                server.broadcastMessage(this.username, message.getMessage());
            }
        }
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
            // ignore
        }
    }
}