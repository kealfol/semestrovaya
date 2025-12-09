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
                LOGGER.warn("Клиент отключился");
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
                String[] parts = message.getMessage().split("\\s+");
                if (parts.length == 2) {
                    if (server.getAuthService().authenticate(parts[0], parts[1])) {
                        this.username = parts[0];
                        sendMessage(CommandType.AUTH_OK, "Вход выполнен");
                        server.subscribe(this);
                        return true;
                    }
                }
                sendMessage(CommandType.ERROR, "Неверный логин/пароль");
            } 
            else if (message.getType() == CommandType.REGISTER) {
                String[] parts = message.getMessage().split("\\s+");
                if (parts.length == 2 && server.getAuthService().register(parts[0], parts[1])) {
                    sendMessage(CommandType.AUTH_OK, "Регистрация успешна");
                } else {
                    sendMessage(CommandType.ERROR, "Логин занят");
                }
            } else {
                sendMessage(CommandType.ERROR, "Нужна авторизация");
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
        }
    }

    public void sendMessage(String sender, String text) {
        sendMessage(new Message(CommandType.PUBLIC_MESSAGE, sender, text));
    }

    public void sendMessage(CommandType type, String text) {
        sendMessage(new Message(type, "Server", text));
    }

    private void sendMessage(Message msg) {
        try {
            out.writeUTF(gson.toJson(msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        server.unsubscribe(this);
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}