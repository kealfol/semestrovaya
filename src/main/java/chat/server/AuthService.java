package chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String DB_URL = "jdbc:sqlite:chat.db"; // Файл базы создастся сам

    public AuthService() {
        try {
            // 1. Подгружаем драйвер (для старых версий Java, но полезно оставить)
            Class.forName("org.sqlite.JDBC");
            
            // 2. Создаем таблицу, если её нет
            try (Connection connection = DriverManager.getConnection(DB_URL);
                 Statement statement = connection.createStatement()) {
                
                String sql = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "login TEXT UNIQUE NOT NULL," +
                        "password TEXT NOT NULL" +
                        ")";
                statement.execute(sql);
                LOGGER.info("База данных пользователей подключена/создана.");
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка подключения к БД", e);
        }
    }

    // Регистрация (INSERT)
    public boolean register(String login, String password) {
        String sql = "INSERT INTO users(login, password) VALUES(?, ?)";

        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, login);
            pstmt.setString(2, password); // В идеале тут надо хешировать пароль!
            pstmt.executeUpdate();
            
            LOGGER.info("Зарегистрирован новый пользователь: {}", login);
            return true;

        } catch (SQLException e) {
            // Код 19 в SQLite означает Constraint Violation (дубликат логина)
            LOGGER.warn("Ошибка регистрации пользователя {}: {}", login, e.getMessage());
            return false;
        }
    }

    // Авторизация (SELECT)
    public boolean authenticate(String login, String password) {
        String sql = "SELECT password FROM users WHERE login = ?";

        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, login);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    // Сравниваем пароль из БД с тем, что прислал клиент
                    return dbPassword.equals(password);
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Ошибка при авторизации", e);
        }
        
        return false; // Пользователь не найден или пароль не совпал
    }
}