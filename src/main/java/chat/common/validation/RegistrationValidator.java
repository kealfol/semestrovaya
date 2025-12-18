package chat.common.validation;

import java.util.regex.Pattern;

public class RegistrationValidator {
    private static final int MIN_LOGIN_LENGTH = 3;
    private static final int MAX_LOGIN_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 30;

    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$");

    // Валидация логина и пароля при регистрации
    public static String validateRegistration(String login, String password) {
        if (login == null || login.isEmpty()) {
            return "Логин не может быть пустым";
        }

        if (password == null || password.isEmpty()) {
            return "Пароль не может быть пустым";
        }

        if (login.trim().isEmpty()) {
            return "Логин не может состоять только из пробелов или невидимых символов";
        }

        if (password.trim().isEmpty()) {
            return "Пароль не может состоять только из пробелов или невидимых символов";
        }

        if (containsWhitespace(login)) {
            return "Логин не должен содержать пробелов, табуляций или других невидимых символов";
        }

        if (containsWhitespace(password)) {
            return "Пароль не должен содержать пробелов, табуляций или других невидимых символов";
        }

        if (login.length() < MIN_LOGIN_LENGTH) {
            return String.format("Логин слишком короткий. Минимум %d символов", MIN_LOGIN_LENGTH);
        }

        if (login.length() > MAX_LOGIN_LENGTH) {
            return String.format("Логин слишком длинный. Максимум %d символов", MAX_LOGIN_LENGTH);
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return String.format("Пароль слишком короткий. Минимум %d символов", MIN_PASSWORD_LENGTH);
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            return String.format("Пароль слишком длинный. Максимум %d символов", MAX_PASSWORD_LENGTH);
        }

        if (!LOGIN_PATTERN.matcher(login).matches()) {
            return "Логин может содержать только латинские буквы, цифры и символ подчеркивания (_)";
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return "Пароль может содержать только латинские буквы, цифры и специальные символы";
        }

        return null; // Валидация пройдена
    }

    private static boolean containsWhitespace(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}