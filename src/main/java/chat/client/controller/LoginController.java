package chat.client.controller;

import chat.client.ClientApp;
import chat.client.model.Network;
import chat.common.validation.RegistrationValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);
    private Network network;
    private ClientApp clientApp;

    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;

    public void init(Network network, ClientApp clientApp) {
        this.network = network;
        this.clientApp = clientApp;
    }

    @FXML
    private void handleLogin() {
        String login = loginField.getText();
        String password = passwordField.getText();

        if (login.isEmpty() || password.isEmpty()) {
            showAlert("Логин и пароль не могут быть пустыми!");
            return;
        }

        network.sendAuthMessage(login, password);
    }

    @FXML
    private void handleRegister() {
        String login = loginField.getText();
        String password = passwordField.getText();

        // Проверка заполненности
        if (login.isEmpty() || password.isEmpty()) {
            showAlert("Логин и пароль не могут быть пустыми!");
            return;
        }

        // Валидация
        String validationError = RegistrationValidator.validateRegistration(login, password);
        if (validationError != null) {
            showAlert(validationError);
            return;
        }

        network.sendRegisterMessage(login, password);
    }

    public void handleAuthOk() {
        Platform.runLater(() -> {
            try {
                clientApp.showChatWindow();
            } catch (Exception e) {
                LOGGER.error("Не удалось открыть окно чата", e);
            }
        });
    }

    public void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(message);

            alert.setOnHidden(event -> {
                // Очищаем поле пароля после закрытия алёёрта
                passwordField.clear();
            });

            alert.showAndWait();
        });
    }

    public void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Успешно");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}