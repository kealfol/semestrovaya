package chat.server;

import chat.common.CommandType;
import chat.common.Message;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class TestClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8189);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            Gson gson = new Gson();
            Scanner scanner = new Scanner(System.in);
            System.out.println("Команды: auth login pass | reg login pass | текст сообщения");

            new Thread(() -> {
                try {
                    while (true) {
                        String json = in.readUTF();
                        Message msg = gson.fromJson(json, Message.class);
                        System.out.println(">> [" + msg.getType() + "] " + msg.getMessage());
                    }
                } catch (IOException e) { System.out.println("Связь потеряна"); }
            }).start();

            while (true) {
                String line = scanner.nextLine();
                String[] parts = line.split(" ");
                Message msg = new Message();
                if (parts[0].equals("auth")) {
                    msg.setType(CommandType.AUTH);
                    msg.setMessage(parts[1] + " " + parts[2]);
                } else if (parts[0].equals("reg")) {
                    msg.setType(CommandType.REGISTER);
                    msg.setMessage(parts[1] + " " + parts[2]);
                } else {
                    msg.setType(CommandType.PUBLIC_MESSAGE);
                    msg.setMessage(line);
                }
                out.writeUTF(gson.toJson(msg));
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}