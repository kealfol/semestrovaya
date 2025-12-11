package chat.client.controller;

import chat.client.model.Network;
import chat.common.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

public class ChatController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private Network network;
    private final Gson gson = new Gson();

    @FXML
    private TextArea chatArea;
    @FXML
    private TextField messageField;
    @FXML
    private Label userCountLabel;

    public void init(Network network) {
        this.network = network;
        this.network.setController(this);
    }

    @FXML
    private void handleSend() {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
            network.sendPublicMessage(messageText);
            messageField.clear();
        }
    }

    public void handleMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case PUBLIC_MESSAGE:
                    chatArea.appendText(String.format("%s: %s%n", message.getSender(), message.getMessage()));
                    break;
                case CLIENT_MESSAGE:
                    updateUserList(message.getMessage());
                    break;
                case ERROR:
                    showError(message.getMessage());
                    break;
                default: break;
            }
        });
    }

    private void updateUserList(String jsonUserList) {
        try {
            Type listType = new TypeToken<List<String>>() {
            }.getType();
            List<String> users = gson.fromJson(jsonUserList, listType);
            userCountLabel.setText("В чате: " + users.size());
        } catch (Exception e) {
            LOGGER.error("Ошибка парсинга списка пользователей: {}", jsonUserList, e);
        }
    }

    public void showError(String errorMessage) {
        Platform.runLater(() -> {
            chatArea.appendText(String.format("Ошибка от сервера: %s%n", errorMessage));
        });
    }
}