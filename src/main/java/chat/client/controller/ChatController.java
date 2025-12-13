package chat.client.controller;

import chat.client.model.Network;
import chat.common.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ChatController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private Network network;
    private final Gson gson = new Gson();

    private List<String> currentUsers = new ArrayList<>();

    @FXML
    private TextArea chatArea;
    @FXML
    private TextField messageField;
    @FXML
    private Label userCountLabel;

    public void init(Network network) {
        this.network = network;
        this.network.setController(this);

        userCountLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                showUsersList();
            }
        });

        userCountLabel.setStyle("-fx-cursor: hand;");
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
                default:
                    // Игнорируем технические сообщения (AUTH, REGISTER и т.д.) в окне чата
                    break;
            }
        });
    }

    private void updateUserList(String jsonUserList) {
        try {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> users = gson.fromJson(jsonUserList, listType);

            currentUsers.clear();
            currentUsers.addAll(users);

            userCountLabel.setText("В чате: " + users.size());
        } catch (Exception e) {
            LOGGER.error("Ошибка парсинга списка пользователей", e);
        }
    }

    private void showUsersList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/users_list.fxml"));
            Parent root = loader.load();

            UsersListController controller = loader.getController();
            controller.setUsers(currentUsers);

            Stage stage = new Stage();
            controller.setStage(stage);

            stage.setTitle("Пользователи в чате");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            LOGGER.error("Ошибка при открытии окна списка пользователей", e);
            showError("Не удалось открыть список пользователей");
        }
    }

    public void showError(String errorMessage) {
        Platform.runLater(() -> {
            chatArea.appendText(String.format("[СИСТЕМА]: %s%n", errorMessage));
        });
    }
}