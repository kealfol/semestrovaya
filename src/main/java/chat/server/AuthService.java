package chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String DB_URL = "jdbc:sqlite:chat.db"; 

    public AuthService() {
        try {
            Class.forName("org.sqlite.JDBC");
            // Создаем таблицу users, если её нет
            try (Connection connection = DriverManager.getConnection(DB_URL);
                 Statement statement = connection.createStatement()) {
                
                String sql = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "login TEXT UNIQUE NOT NULL," +
                        "password TEXT NOT NULL" +
                        ")";
                statement.execute(sql);
                LOGGER.info("База данных подключена: chat.db");
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка подключения к БД", e);
        }
    }

    // Регистрация
    public boolean register(String login, String password) {
        String sql = "INSERT INTO users(login, password) VALUES(?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, login);
            pstmt.setString(2, password); 
            pstmt.executeUpdate();
            
            LOGGER.info("Зарегистрирован новый пользователь: {}", login);
            return true;
        } catch (SQLException e) {
            // Ошибка возникает, если логин уже есть (UNIQUE constraint)
            LOGGER.warn("Попытка регистрации существующего логина: {}", login);
            return false;
        }
    }

    // Вход
    public boolean authenticate(String login, String password) {
        String sql = "SELECT password FROM users WHERE login = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                // Сравниваем пароль из базы с присланным
                return dbPassword.equals(password);
            }
        } catch (SQLException e) {
            LOGGER.error("Ошибка авторизации", e);
        }
        return false;
    }
}