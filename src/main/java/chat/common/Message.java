package chat.common;

import java.time.LocalDateTime;

public class Message {
    private CommandType type;
    private String sender;
    private String message;
    private String timestamp; // Используем String для простоты JSON

    public Message() {
    }

    public Message(CommandType type, String sender, String message) {
        this.type = type;
        this.sender = sender;
        this.message = message;
        this.timestamp = LocalDateTime.now().toString();
    }

    public CommandType getType() { return type; }
    public void setType(CommandType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}