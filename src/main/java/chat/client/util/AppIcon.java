package chat.client.util;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Objects;

public class AppIcon {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppIcon.class);
    private static Image icon = null;

    static {
        try {
            InputStream stream = AppIcon.class.getResourceAsStream("/images/icon.png");
            if (stream != null) {
                icon = new Image(stream);
                LOGGER.info("Иконка приложения загружена");
            } else {
                LOGGER.warn("Иконка не найдена: /images/icon.png");
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка загрузки иконки", e);
        }
    }

    public static void applyTo(Stage stage) {
        if (stage != null && icon != null) {
            stage.getIcons().add(icon);
        }
    }

    public static Image getIcon() {
        return icon;
    }
}