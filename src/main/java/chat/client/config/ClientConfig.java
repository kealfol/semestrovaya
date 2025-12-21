package chat.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConfig.class);
    private static final String CONFIG_FILE = "application.properties";

    private String serverHost;
    private int serverPort;
    private int connectionTimeout;
    private int reconnectAttempts;
    private int reconnectDelay;

    public ClientConfig() {
        loadConfig();
    }

    private void loadConfig() {
        Properties props = new Properties();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                props.load(input);
            } else {
                LOGGER.warn("Конфигурационный файл не найден. Используются значения по умолчанию.");
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка загрузки конфигурации.", e);
        }

        serverHost = props.getProperty("server.host", "localhost");
        serverPort = Integer.parseInt(props.getProperty("server.port", "8189"));
        connectionTimeout = Integer.parseInt(props.getProperty("client.connection.timeout", "5000"));
        reconnectAttempts = Integer.parseInt(props.getProperty("client.reconnect.attempts", "3"));
        reconnectDelay = Integer.parseInt(props.getProperty("client.reconnect.delay", "1000"));
    }

    public String getServerHost() { return serverHost; }
    public int getServerPort() { return serverPort; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getReconnectAttempts() { return reconnectAttempts; }
    public int getReconnectDelay() { return reconnectDelay; }
}