package org.example.clientsevermsgexample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML
    private ComboBox dropdownPort;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dropdownPort.getItems().addAll("7",     // ping
                "13",     // daytime
                "21",     // ftp
                "23",     // telnet
                "71",     // finger
                "80",     // http
                "119",    // nntp (news)
                "161"     // snmp
        );

        if (dropdownPort.getItems().size() > 0) {
            dropdownPort.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private Button clearBtn;

    @FXML
    private TextArea resultArea;

    @FXML
    private Label server_lbl;

    @FXML
    private Button testBtn;

    @FXML
    private Label test_lbl;

    @FXML
    private TextField urlName;

    // Old implementation variables
    private Socket socket1;
    private Label lb122, lb12;
    private TextField msgText;

    // Chat server variables
    private ServerSocket oldServerSocket;
    private boolean oldServerRunning = false;

    @FXML
    private void user1_client(ActionEvent event) {
        try {
            // Check if old server implementation is using the same port
            if (oldServerRunning) {
                alert("Warning", "Please stop the old server implementation first!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("client-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("User 1 (Client)");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            alert("Error", "Could not open client: " + e.getMessage());
        }
    }

    @FXML
    private void user2_server(ActionEvent event) {
        try {
            // Check if old server implementation is using the same port
            if (oldServerRunning) {
                alert("Warning", "Please stop the old server implementation first!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("server-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("User 2 (Server)");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            alert("Error", "Could not open server: " + e.getMessage());
        }
    }

    @FXML
    void checkConnection(ActionEvent event) {
        String host = urlName.getText();
        if (host.isEmpty()) {
            alert("Input Error", "Please enter a hostname.");
            return;
        }

        if (dropdownPort.getValue() == null) {
            alert("Input Error", "Please select a port.");
            return;
        }

        int port = Integer.parseInt(dropdownPort.getValue().toString());

        try (Socket sock = new Socket(host, port)) {
            resultArea.appendText(host + " listening on port " + port + "\n");
        } catch (UnknownHostException e) {
            resultArea.setText("Error: " + e.getMessage() + "\n");
        } catch (Exception e) {
            resultArea.appendText(host + " not listening on port " + port + "\n");
        }
    }

    @FXML
    void clearBtn(ActionEvent event) {
        resultArea.setText("");
        urlName.setText("");
    }

    // Old server implementation - using port 7777 to avoid conflict with the chat
    @FXML
    void startServer(ActionEvent event) {
        if (oldServerRunning) {
            alert("Warning", "Server is already running!");
            return;
        }

        Stage stage = new Stage();
        Group root = new Group();

        Label lb11 = new Label("Server");
        lb11.setLayoutX(100);
        lb11.setLayoutY(100);

        lb12 = new Label("info");
        lb12.setLayoutX(100);
        lb12.setLayoutY(200);

        Button stopButton = new Button("Stop Server");
        stopButton.setLayoutX(100);
        stopButton.setLayoutY(250);
        stopButton.setOnAction(e -> stopOldServer());

        root.getChildren().addAll(lb11, lb12, stopButton);
        Scene scene = new Scene(root, 600, 350);
        stage.setScene(scene);
        stage.setTitle("Old Server Implementation");
        stage.show();

        // Start on port 7777 to avoid conflict with the chat
        new Thread(() -> runServer(7777)).start();
    }

    private void stopOldServer() {
        oldServerRunning = false;
        try {
            if (oldServerSocket != null && !oldServerSocket.isClosed()) {
                oldServerSocket.close();
            }
            updateServer("Server stopped.");
        } catch (IOException e) {
            updateServer("Error stopping server: " + e.getMessage());
        }
    }

    private String message;

    private void runServer(int port) {
        try {
            oldServerRunning = true;
            oldServerSocket = new ServerSocket(port);
            updateServer("Server is running on port " + port + " and waiting for a client...");

            while (oldServerRunning) {
                try (Socket clientSocket = oldServerSocket.accept();
                     DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                     DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                    updateServer("Client connected!");

                    message = dis.readUTF();
                    updateServer("Message from client: " + message);

                    // Sending a response back to the client
                    dos.writeUTF("Received: " + message);

                    if (message.equalsIgnoreCase("exit")) break;

                } catch (IOException e) {
                    if (oldServerRunning) {
                        updateServer("Error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            updateServer("Server error: " + e.getMessage());
        } finally {
            oldServerRunning = false;
        }
    }

    private void updateServer(String message) {
        javafx.application.Platform.runLater(() -> {
            if (lb12 != null) {
                lb12.setText(message);
            }
        });
    }

    @FXML
    void startClient(ActionEvent event) {
        Stage stage = new Stage();
        Group root = new Group();

        Button connectButton = new Button("Connect to server");
        connectButton.setLayoutX(100);
        connectButton.setLayoutY(300);
        connectButton.setOnAction(this::connectToServer);

        Label lb11 = new Label("Client");
        lb11.setLayoutX(100);
        lb11.setLayoutY(100);

        msgText = new TextField("msg");
        msgText.setLayoutX(100);
        msgText.setLayoutY(150);

        lb122 = new Label("info");
        lb122.setLayoutX(100);
        lb122.setLayoutY(200);

        root.getChildren().addAll(lb11, lb122, connectButton, msgText);

        Scene scene = new Scene(root, 600, 350);
        stage.setScene(scene);
        stage.setTitle("Old Client Implementation");
        stage.show();
    }

    private void connectToServer(ActionEvent event) {
        try (Socket socket = new Socket("localhost", 7777);  // Connect to the old server port
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            updateTextClient("Connected to server");
            dos.writeUTF(msgText.getText());

            String response = dis.readUTF();
            updateTextClient("Server response: " + response);

        } catch (Exception e) {
            updateTextClient("Error: " + e.getMessage());
        }
    }

    private void updateTextClient(String message) {
        javafx.application.Platform.runLater(() -> {
            if (lb122 != null) {
                lb122.setText(message);
            }
        });
    }

    private void alert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}