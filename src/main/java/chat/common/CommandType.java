package chat.common;

public enum CommandType {
    AUTH,           // Запрос на вход
    AUTH_OK,        // Вход успешен
    REGISTER,       // Запрос на регистрацию
    ERROR,          // Ошибка
    PUBLIC_MESSAGE, // Сообщение в чат
    LIST_REQUEST,   // Запрос списка пользователей
    CLIENT_MESSAGE  // Служебное
}