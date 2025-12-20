package chat.common.validation;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.List;

public class RegistrationValidator {
    private static final int MIN_LOGIN_LENGTH = 3;
    private static final int MAX_LOGIN_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 30;

    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$");

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]");

    // Список самых простых и распространенных паролей
    private static final List<String> WEAK_PASSWORDS = Arrays.asList(
            "password", "123456", "12345678", "123456789", "qwerty",
            "admin", "letmein", "welcome", "monkey", "password1",
            "123123", "111111", "sunshine", "iloveyou", "trustno1",
            "123qwe", "qwerty123", "admin123", "football", "baseball",
            "superman", "dragon", "master", "hello", "login",
            "princess", "qwertyuiop", "password123", "1q2w3e4r",
            "654321", "1234", "test", "guest", "user"
    );

    // Валидация логина и пароля при регистрации
    public static String validateRegistration(String login, String password) {
        // Проверка логина
        String loginValidation = validateLogin(login);
        if (loginValidation != null) {
            return loginValidation;
        }

        // Проверка пароля
        String passwordValidation = validatePassword(password, login);
        if (passwordValidation != null) {
            return passwordValidation;
        }

        return null; // Валидация пройдена
    }

    public static String validatePassword(String password, String login) {
        if (password == null || password.isEmpty()) {
            return "Пароль не может быть пустым.";
        }

        if (password.trim().isEmpty()) {
            return "Пароль не может состоять только из пробелов или невидимых символов.";
        }

        if (containsWhitespace(password)) {
            return "Пароль не должен содержать пробелов, табуляций или других невидимых символов.";
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return String.format("Пароль слишком короткий. Минимум %d символов.", MIN_PASSWORD_LENGTH);
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            return String.format("Пароль слишком длинный. Максимум %d символов.", MAX_PASSWORD_LENGTH);
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return "Пароль может содержать только латинские буквы, цифры и специальные символы.";
        }

        // Проверка на простоту пароля
        String complexityCheck = checkPasswordComplexity(password, login);
        if (complexityCheck != null) {
            return complexityCheck;
        }

        return null;
    }

    // Проверка сложности пароля
    private static String checkPasswordComplexity(String password, String login) {
        String lowerPassword = password.toLowerCase();
        if (WEAK_PASSWORDS.contains(lowerPassword)) {
            return "Пароль слишком простой и распространенный. Выберите более сложный.";
        }

        if (login != null && !login.isEmpty() && lowerPassword.contains(login.toLowerCase())) {
            return "Пароль не должен содержать ваш логин.";
        }

        if (isSequential(password)) {
            return "Пароль содержит простые последовательности символов. Выберите более сложный.";
        }

        if (hasRepeatingCharacters(password, 3)) { // 3 повторяющихся символа подряд
            return "Пароль содержит слишком много повторяющихся символов.";
        }
        int complexityScore = calculateComplexityScore(password);

        if (complexityScore < 3) { // Минимум 3 из 4 требований
            return "Пароль слишком простой. Пароль должен содержать хотя бы 3 из следующих условий: " +
                    "заглавные буквы, строчные буквы, цифры, специальные символы.";
        }

        return null;
    }

    // Проверка на последовательности
    private static boolean isSequential(String password) {
        if (password.length() < 3) return false;

        String lower = password.toLowerCase();

        for (int i = 0; i < lower.length() - 2; i++) {
            char c1 = lower.charAt(i);
            char c2 = lower.charAt(i + 1);
            char c3 = lower.charAt(i + 2);

            if (Character.isDigit(c1) && Character.isDigit(c2) && Character.isDigit(c3)) {
                if (c2 == c1 + 1 && c3 == c2 + 1) return true;
                if (c2 == c1 - 1 && c3 == c2 - 1) return true;
            }

            if (Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3)) {
                if (c2 == c1 + 1 && c3 == c2 + 1) return true;
                if (c2 == c1 - 1 && c3 == c2 - 1) return true;
            }
        }

        String[] keyboardSequences = {"qwe", "asd", "zxc", "qaz", "wsx", "edc", "rfv", "tgb", "yhn", "ujm", "ik", "ol"};
        for (String seq : keyboardSequences) {
            if (lower.contains(seq)) return true;
        }

        return false;
    }

    private static boolean hasRepeatingCharacters(String password, int maxRepeats) {
        if (password.length() < maxRepeats) return false;

        for (int i = 0; i <= password.length() - maxRepeats; i++) {
            char firstChar = password.charAt(i);
            boolean allSame = true;

            for (int j = 1; j < maxRepeats; j++) {
                if (password.charAt(i + j) != firstChar) {
                    allSame = false;
                    break;
                }
            }

            if (allSame) return true;
        }

        return false;
    }

    // Расчёёт сложности пароля
    private static int calculateComplexityScore(String password) {
        int score = 0;

        Matcher upperMatcher = UPPERCASE_PATTERN.matcher(password);
        if (upperMatcher.find()) score++;

        Matcher lowerMatcher = LOWERCASE_PATTERN.matcher(password);
        if (lowerMatcher.find()) score++;

        Matcher digitMatcher = DIGIT_PATTERN.matcher(password);
        if (digitMatcher.find()) score++;

        Matcher specialMatcher = SPECIAL_CHAR_PATTERN.matcher(password);
        if (specialMatcher.find()) score++;

        return score;
    }

    // Валидация логина
    private static String validateLogin(String login) {
        if (login == null || login.isEmpty()) {
            return "Логин не может быть пустым.";
        }

        if (login.trim().isEmpty()) {
            return "Логин не может состоять только из пробелов или невидимых символов.";
        }

        if (containsWhitespace(login)) {
            return "Логин не должен содержать пробелов, табуляций или других невидимых символов.";
        }

        if (login.length() < MIN_LOGIN_LENGTH) {
            return String.format("Логин слишком короткий. Минимум %d символов.", MIN_LOGIN_LENGTH);
        }

        if (login.length() > MAX_LOGIN_LENGTH) {
            return String.format("Логин слишком длинный. Максимум %d символов.", MAX_LOGIN_LENGTH);
        }

        if (!LOGIN_PATTERN.matcher(login).matches()) {
            return "Логин может содержать только латинские буквы, цифры и символ подчеркивания (_).";
        }

        return null;
    }

    // Метод для проверки наличия пробелов
    private static boolean containsWhitespace(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}