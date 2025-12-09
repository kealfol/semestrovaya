package chat.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AuthService {
    private static final String DB_FILE = "users_db.json";
    private final Gson gson = new Gson();
    private List<UserEntity> users;

    public AuthService() {
        users = new ArrayList<>();
        loadUsers();
    }

    private void loadUsers() {
        File file = new File(DB_FILE);
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<UserEntity>>(){}.getType();
            users = gson.fromJson(reader, listType);
            if (users == null) users = new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        try (Writer writer = new FileWriter(DB_FILE)) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean register(String login, String password) {
        for (UserEntity user : users) {
            if (user.getUsername().equals(login)) return false;
        }
        users.add(new UserEntity(login, password));
        saveUsers();
        return true;
    }

    public boolean authenticate(String login, String password) {
        for (UserEntity user : users) {
            if (user.getUsername().equals(login) && user.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }
}