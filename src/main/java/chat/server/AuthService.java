package chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String DB_URL = "jdbc:sqlite:chat.db";
    

    private static final String SALT = "SAltSALTSALTMANSXDXD"; 

    public AuthService() {
        try {
            try (Connection connection = DriverManager.getConnection(DB_URL);
                 Statement statement = connection.createStatement()) {
                
                String sql = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "login TEXT UNIQUE NOT NULL," +
                        "password TEXT NOT NULL" +
                        ")";
                statement.execute(sql);
                LOGGER.info("Database connected: chat.db");
            }
        } catch (Exception e) {
            LOGGER.error("Database connection error", e);
        }
    }

    public boolean register(String login, String password) {
        String sql = "INSERT INTO users(login, password) VALUES(?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            // 1. Хешируем пароль перед записью!
            String passwordHash = hashPassword(password);

            pstmt.setString(1, login);
            pstmt.setString(2, passwordHash); 
            pstmt.executeUpdate();
            
            LOGGER.info("New user registered: {}", login);
            return true;
        } catch (SQLException e) {
            LOGGER.warn("Registration failed. Login '{}' already exists.", login);
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
                
                // 2. Хешируем введенный пароль и сравниваем с тем, что в базе
                String inputHash = hashPassword(password);
                
                return storedHash.equals(inputHash);
            }
        } catch (SQLException e) {
            LOGGER.error("Authentication error", e);
        }
        return false;
    }

    /**
     * Метод превращает чистый пароль в SHA-256 хеш с солью.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Добавляем соль к паролю
            String textToHash = password + SALT;
            byte[] encodedhash = digest.digest(textToHash.getBytes(StandardCharsets.UTF_8));
            
            // Переводим байты в шестнадцатеричную строку (Hex)
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // Этого никогда не должно случиться, так как SHA-256 есть в любой Java
            throw new RuntimeException("Encryption algorithm not found", e);
        }
    }
}