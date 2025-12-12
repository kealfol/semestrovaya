package chat.server;

import chat.common.CommandType;
import chat.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String DB_URL = "jdbc:sqlite:chat.db";
    private static final String SALT = "MySuperSecretSalt_#&@!2024";

    public AuthService() {
        try {
            try (Connection connection = DriverManager.getConnection(DB_URL);
                 Statement statement = connection.createStatement()) {
                
                String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "login TEXT UNIQUE NOT NULL," +
                        "password TEXT NOT NULL" +
                        ")";
                statement.execute(sqlUsers);

                String sqlMessages = "CREATE TABLE IF NOT EXISTS messages (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "sender TEXT NOT NULL," +
                        "message TEXT NOT NULL," +
                        "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")";
                statement.execute(sqlMessages);
                
                LOGGER.info("Database connected & tables checked.");
            }
        } catch (Exception e) {
            LOGGER.error("Database connection error", e);
        }
    }

    public void saveMessage(String sender, String message) {
        String sql = "INSERT INTO messages(sender, message) VALUES(?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setString(1, sender);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.error("Failed to save message history", e);
        }
    }

    public List<Message> getLastMessages(int limit) {
        List<Message> history = new ArrayList<>();
        // ИСПРАВЛЕНИЕ ТУТ: Добавили 'id' во внутренний SELECT
        String sql = "SELECT * FROM (SELECT id, sender, message FROM messages ORDER BY id DESC LIMIT ?) ORDER BY id ASC";
        
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String sender = rs.getString("sender");
                String text = rs.getString("message");
                history.add(new Message(CommandType.PUBLIC_MESSAGE, sender, text));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to load message history", e);
        }
        return history;
    }

    // --- Остальные методы без изменений ---

    public boolean register(String login, String password) {
        String sql = "INSERT INTO users(login, password) VALUES(?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String passwordHash = hashPassword(password);
            pstmt.setString(1, login);
            pstmt.setString(2, passwordHash); 
            pstmt.executeUpdate();
            LOGGER.info("New user registered: {}", login);
            return true;
        } catch (SQLException e) {
            LOGGER.warn("Registration failed for '{}'", login);
            return false;
        }
    }

    public boolean authenticate(String login, String password) {
        String sql = "SELECT password FROM users WHERE login = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                String inputHash = hashPassword(password);
                return storedHash.equals(inputHash);
            }
        } catch (SQLException e) {
            LOGGER.error("Authentication error", e);
        }
        return false;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String textToHash = password + SALT;
            byte[] encodedhash = digest.digest(textToHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}