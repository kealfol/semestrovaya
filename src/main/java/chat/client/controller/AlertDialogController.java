package chat.client.controller;

import chat.client.util.AppIcon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AlertDialogController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertDialogController.class);

    @FXML
    private VBox root;

    @FXML
    private Label messageLabel;

    @FXML
    private HBox buttonsContainer;

    @FXML
    private Button yesButton;

    @FXML
    private Button noButton;

    private Stage stage;
    private boolean result = false;

    public static void showError(String title, String message) {
        Platform.runLater(() -> showDialog(title, message, false));
    }

    public static void showInfo(String title, String message) {
        Platform.runLater(() -> showDialog(title, message, false));
    }

    public static boolean showConfirm(String title, String message) {
        return showDialog(title, message, true);
    }

    private static boolean showDialog(String title, String message, boolean isConfirm) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AlertDialogController.class.getResource("/fxml/alert_dialog.fxml")
            );

            Scene scene = new Scene(loader.load());
            AlertDialogController controller = loader.getController();

            Stage stage = new Stage();
            controller.stage = stage;
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);

            AppIcon.applyTo(stage);

            controller.setup(message, isConfirm);

            stage.setScene(scene);
            stage.showAndWait();

            return controller.result;

        } catch (IOException e) {
            LOGGER.error("Ошибка при отображении диалогового окна: {}", title, e);
            return false;
        }
    }

    private void setup(String message, boolean isConfirm) {
        messageLabel.setText(message);
        
        if (isConfirm) {
            
            noButton.setVisible(true);
            noButton.setManaged(true);
            yesButton.setText("Да");
            
            
            buttonsContainer.setAlignment(javafx.geometry.Pos.CENTER);
        } else {
            
            noButton.setVisible(false);
            noButton.setManaged(false); 
            yesButton.setText("ОК");
            
            
            buttonsContainer.setAlignment(javafx.geometry.Pos.CENTER);
        }
    }

    @FXML
    private void handleYes() {
        result = true;
        if (stage != null) stage.close();
    }

    @FXML
    private void handleNo() {
        result = false;
        if (stage != null) stage.close();
    }
}