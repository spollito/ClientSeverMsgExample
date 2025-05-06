package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.BindException;

public class ServerView {
    @FXML
    private Button button_send;

    @FXML
    private TextField tf_message;

    @FXML
    private VBox vbox_messages;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private volatile boolean isRunning = true;
    private volatile boolean isClientConnected = false;
    private Thread listenThread;
    private Thread receiveThread;

    // Start listening when initialized
    @FXML
    public void initialize() {
        button_send.setDisable(true);
        tf_message.setDisable(true);

        button_send.setOnAction(e -> sendMessage());
        tf_message.setOnAction(e -> sendMessage());

        listenThread = new Thread(this::startServer);
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(6666);
            appendMessage("Server started. Waiting for client...");

            while (isRunning) {
                try {
                    clientSocket = serverSocket.accept();
                    isClientConnected = true;

                    Platform.runLater(() -> {
                        button_send.setDisable(false);
                        tf_message.setDisable(false);
                        tf_message.requestFocus();
                    });

                    dis = new DataInputStream(clientSocket.getInputStream());
                    dos = new DataOutputStream(clientSocket.getOutputStream());

                    appendMessage("Client connected.");

                    // Listen for messages in a separate thread
                    if (receiveThread != null && receiveThread.isAlive()) {
                        receiveThread.interrupt();
                    }

                    receiveThread = new Thread(this::receiveMessages);
                    receiveThread.setDaemon(true);
                    receiveThread.start();

                    // Wait until this client disconnects before accepting a new one
                    receiveThread.join();

                    closeClientConnection();
                } catch (InterruptedException e) {
                    // Thread was interrupted
                    break;
                } catch (IOException e) {
                    appendMessage("Client error: " + e.getMessage());
                }
            }
        } catch (BindException e) {
            appendMessage("Server error: Port 6666 is already in use. Close other server instances first.");
        } catch (Exception e) {
            appendMessage("Server error: " + e.getMessage());
        } finally {
            closeServerSocket();
        }
    }

    private void sendMessage() {
        if (!isClientConnected || dos == null) {
            appendMessage("No client connected yet.");
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
            handleClientDisconnect();
        }
    }

    private void receiveMessages() {
        try {
            while (isClientConnected && isRunning) {
                String msg = dis.readUTF();
                appendMessage("Client: " + msg);
            }
        } catch (IOException e) {
            if (isClientConnected && isRunning) {
                appendMessage("Client disconnected: " + e.getMessage());
                handleClientDisconnect();
            }
        }
    }

    private void handleClientDisconnect() {
        isClientConnected = false;
        Platform.runLater(() -> {
            button_send.setDisable(true);
            tf_message.setDisable(true);
        });

        closeClientConnection();
        appendMessage("Waiting for new client connection...");
    }

    private void closeClientConnection() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            appendMessage("Error closing client connection: " + e.getMessage());
        }

        dis = null;
        dos = null;
        clientSocket = null;
        isClientConnected = false;
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            appendMessage("Error closing server socket: " + e.getMessage());
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