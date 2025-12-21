package chat.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UsersListController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsersListController.class);

    @FXML
    private ListView<String> usersListView;

    @FXML
    private Label titleLabel;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setUsers(List<String> users) {
        LOGGER.info("Setting users to list view. Count: {}", users.size());

        usersListView.getItems().clear();

        if (users.isEmpty()) {
            usersListView.getItems().add("Нет подключенных пользователей.");
            titleLabel.setText("Пользователи в чате (0)");
        } else {
            usersListView.getItems().addAll(users);
            titleLabel.setText("Пользователи в чате (" + users.size() + ")");
        }
    }

    @FXML
    private void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }
}