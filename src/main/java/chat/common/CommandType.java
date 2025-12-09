package chat.common;

public enum CommandType {
    AUTH,           // Запрос на вход
    AUTH_OK,        // Вход успешен
    REGISTER,       // Запрос на регистрацию
    ERROR,          // Ошибка
    PUBLIC_MESSAGE, // Сообщение в чат
    CLIENT_MESSAGE  // Служебное
}