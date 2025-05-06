package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ConnectException;

public class ClientView {
    @FXML
    private Button button_send;

    @FXML
    private TextField tf_message;

    @FXML
    private VBox vbox_messages;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private volatile boolean isConnected = false;
    private Thread receiveThread;

    @FXML
    public void initialize() {
        button_send.setDisable(true);
        tf_message.setDisable(true);

        button_send.setOnAction(e -> sendMessage());
        tf_message.setOnAction(e -> sendMessage());

        new Thread(this::startClient).start();
    }

    private void startClient() {
        try {
            appendMessage("Connecting to server...");
            socket = new Socket("localhost", 6666);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            isConnected = true;

            Platform.runLater(() -> {
                button_send.setDisable(false);
                tf_message.setDisable(false);
                tf_message.requestFocus();
            });

            // Listen for messages in a separate thread
            receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();

            appendMessage("Connected to server.");
        } catch (ConnectException e) {
            appendMessage("Connection failed. Is the server running?");
            scheduleReconnect();
        } catch (Exception e) {
            appendMessage("Client error: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        appendMessage("Will try to reconnect in 5 seconds...");
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                if (!isConnected) {
                    startClient();
                }
            } catch (InterruptedException e) {
                // Thread was interrupted
            }
        }).start();
    }

    private void sendMessage() {
        if (!isConnected || dos == null) {
            appendMessage("Not connected to server.");
            return;
        }

        String msg = tf_message.getText();
        if (msg.isEmpty()) return;

        try {
            dos.writeUTF(msg);
            dos.flush();
            appendMessage("Steven: " + msg);
            tf_message.clear();
        } catch (IOException e) {
            appendMessage("Send error: " + e.getMessage());
            handleDisconnect();
        }
    }

    private void receiveMessages() {
        try {
            while (isConnected) {
                String msg = dis.readUTF();
                appendMessage("Server: " + msg);
            }
        } catch (IOException e) {
            if (isConnected) {
                appendMessage("Connection closed: " + e.getMessage());
                handleDisconnect();
            }
        }
    }

    private void handleDisconnect() {
        isConnected = false;
        Platform.runLater(() -> {
            button_send.setDisable(true);
            tf_message.setDisable(true);
        });

        closeResources();
        scheduleReconnect();
    }

    private void closeResources() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            appendMessage("Error closing connection: " + e.getMessage());
        }
    }

    private void appendMessage(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            vbox_messages.getChildren().add(label);
            // Auto-scroll to bottom
            vbox_messages.heightProperty().addListener((obs, oldVal, newVal) -> {
                vbox_messages.layout();
                vbox_messages.setTranslateY(vbox_messages.getHeight());
            });
        });
    }
}