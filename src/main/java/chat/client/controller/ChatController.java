package chat.client.controller;

import chat.client.ClientApp;
import chat.client.model.Network;
import chat.common.CommandType;
import chat.common.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
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
import java.util.Optional;

public class ChatController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private Network network;
    private final Gson gson = new Gson();
    private ClientApp clientApp;
    private Stage primaryStage;

    private List<String> currentUsers = new ArrayList<>();

    @FXML
    private TextArea chatArea;
    @FXML
    private TextField messageField;
    @FXML
    private Label userCountLabel;

    public void init(Network network, ClientApp clientApp, Stage primaryStage) {
        this.network = network;
        this.clientApp = clientApp;
        this.primaryStage = primaryStage;
        this.network.setController(this);

        userCountLabel.setStyle("-fx-text-fill: #38b50fff; -fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");

        userCountLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                showUsersList();
            }
        });

        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            handleLogout();
        });
    }

    @FXML
    private void handleSend() {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
            network.sendPublicMessage(messageText);
            messageField.clear();
        }
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Выход из чата");
        alert.setHeaderText("Вы уверены, что хотите выйти?");
        alert.setContentText("Вы будете отключены от сервера.");

        ButtonType yesButton = new ButtonType("Да, выйти");
        ButtonType noButton = new ButtonType("Нет, остаться");
        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == yesButton) {
            logout();
        }
    }

    private void logout() {
        try {
            LOGGER.info("User {} is logging out", network.getUsername());

            network.sendLogoutMessage();

            network.close();

            Platform.runLater(() -> {
                try {
                    clientApp.showLoginWindow();
                } catch (IOException e) {
                    LOGGER.error("Ошибка при открытии окна входа", e);
                    System.exit(0);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Ошибка при выходе", e);
            Platform.runLater(() -> {
                try {
                    clientApp.showLoginWindow();
                } catch (IOException ex) {
                    LOGGER.error("Критическая ошибка", ex);
                }
            });
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
                    if (!message.getMessage().contains("logout")) {
                        showError(message.getMessage());
                    }
                    break;
                case USER_LOGOUT:
                    chatArea.appendText(String.format("[СИСТЕМА]: %s покинул(а) чат%n", message.getSender()));
                    break;
                default:
                    break;
            }
        });
    }

    private void updateUserList(String jsonUserList) {
        try {
            Type listType = new TypeToken<List<String>>() {
            }.getType();
            List<String> users = gson.fromJson(jsonUserList, listType);

            currentUsers.clear();
            currentUsers.addAll(users);

            userCountLabel.setText(users.size() + " онлайн");
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