package chat.client;

import chat.client.controller.ChatController;
import chat.client.controller.LoginController;
import chat.client.model.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {
    private Stage primaryStage;
    private Network network;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.network = new Network();

        try {
            network.connect();
        } catch (Exception e) {
            System.err.println("Не удалось подключиться к серверу.");
        }

        showLoginWindow();
    }

    private void showLoginWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.init(network, this);

        network.startReading(
                controller::handleAuthOk,
                controller::showAlert,
                controller::showInfo
        );

        primaryStage.setTitle("Вход в чат");
        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest(event -> network.close());
        primaryStage.show();
    }

    public void showChatWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
        Parent root = loader.load();

        ChatController controller = loader.getController();
        controller.init(network);

        // Обновление заголовка окна с именем пользователя
        primaryStage.setTitle("Чат - " + network.getUsername());
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}