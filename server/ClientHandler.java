package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import static shared.SharedConstants.*;

public class ClientHandler implements Runnable {
    int MAX_USERNAME_LENGTH = 10;
    private final Socket socket;
    private final ServerBroadcaster broadcaster;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private boolean connected;

    public ClientHandler(Socket socket, ServerBroadcaster broadcaster) {
        this.socket = socket;
        this.broadcaster = broadcaster;
        this.connected = true;
        this.username = DEFAULT_USERNAME;
    }

    private void initializeStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            initializeStreams();
            String initialMessage = reader.readLine();
            if (initialMessage != null) handleMessage(initialMessage);

            String message;
            while (connected && (message = reader.readLine()) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleMessage(String message) {
        String[] parts = parseMessage(message);
        if (parts.length == 0) return;
        String messageType = parts[0];

        switch (messageType) {
            case MSG_JOIN -> handleJoin(parts);
            case MSG_EDIT -> handleMultiTabEdit(parts); // Updated
            case MSG_DELETE -> handleMultiTabDelete(parts); // Updated
            case MSG_SYNC -> handleMultiTabSync(parts); // Updated
            default -> System.out.println("Unknown type: " + messageType);
        }
    }

    private void handleJoin(String[] parts) {
        if (parts.length >= 2) {
            username = parts[1];
            if (username.length() > MAX_USERNAME_LENGTH) username = username.substring(0, MAX_USERNAME_LENGTH);
            broadcaster.addClient(this);
            broadcastUserList();
        }
    }

    // New logic: parts[1] is now the FileType (TXT or JAVA)
    private void handleMultiTabEdit(String[] parts) {
        if (parts.length >= 5) {
            String fileType = parts[1];
            String position = parts[2];
            String text = parts[3];
            String sender = parts[4];
            broadcaster.broadcast(buildMessage(MSG_EDIT, fileType, position, text, sender), this);
        }
    }

    private void handleMultiTabDelete(String[] parts) {
        if (parts.length >= 5) {
            String fileType = parts[1];
            String position = parts[2];
            String length = parts[3];
            String sender = parts[4];
            broadcaster.broadcast(buildMessage(MSG_DELETE, fileType, position, length, sender), this);
        }
    }

    private void handleMultiTabSync(String[] parts) {
        if (parts.length >= 4) {
            String fileType = parts[1];
            String fullText = parts[2];
            String sender = parts[3];
            broadcaster.broadcast(buildMessage(MSG_SYNC, fileType, fullText, sender), this);
        }
    }

    private void broadcastUserList() {
        String userList = String.join(USER_LIST_DELIMITER, broadcaster.getConnectedUsernames());
        broadcaster.broadcastToAll(buildMessage(MSG_USER_LIST, userList));
    }

    public void sendMessage(String message) throws IOException {
        if (connected && writer != null) writer.println(message);
    }

    public void disconnect() {
        if (connected) {
            connected = false;
            broadcaster.removeClient(this);
            try {
                if (socket != null) socket.close();
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public String getUsername() { return username; }
    public boolean isConnected() { return connected; }
}
