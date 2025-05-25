package org.example;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class RegisterController {

    @FXML private TextField usernameFieldReg;
    @FXML private PasswordField passwordFieldReg;
    @FXML private PasswordField confirmPasswordFieldReg;
    @FXML private Button registerButton;
    @FXML private Label messageLabelReg;
    @FXML private Hyperlink loginLink;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    // We use same SERVER_BASE_URL in LoginController
    private final String SERVER_BASE_URL = "http://5.231.26.86:8081"; // VDS IP

    @FXML
    public void initialize() {
        messageLabelReg.setText("");
    }

    @FXML
    void handleRegisterAction(ActionEvent event) {
        String username = usernameFieldReg.getText();
        String password = passwordFieldReg.getText();
        String confirmPassword = confirmPasswordFieldReg.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabelReg.setText("All fields are required.");
            messageLabelReg.setStyle("-fx-text-fill: red;");
            return;
        }
        if (!password.equals(confirmPassword)) {
            messageLabelReg.setText("Passwords do not match.");
            messageLabelReg.setStyle("-fx-text-fill: red;");
            return;
        }

        messageLabelReg.setText("Registering...");
        messageLabelReg.setStyle("-fx-text-fill: black;");

        Thread registerThread = new Thread(() -> {
            try {
                String formData = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                        "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_BASE_URL + "/register"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formData))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200 && response.body().contains("Registration successful")) {
                        messageLabelReg.setText("Registration Successful! You can now login.");
                        messageLabelReg.setStyle("-fx-text-fill: green;");


                    } else {
                        messageLabelReg.setText("Registration Failed: " + response.body());
                        messageLabelReg.setStyle("-fx-text-fill: red;");
                    }
                });
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    messageLabelReg.setText("Error connecting to server: " + e.getMessage());
                    messageLabelReg.setStyle("-fx-text-fill: red;");
                });
            }
        });
        registerThread.setDaemon(true);
        registerThread.start();
    }

    @FXML
    void handleLoginLinkAction(ActionEvent event) {
        try {
            Stage stage = (Stage) loginLink.getScene().getWindow();
            Parent loginRoot = FXMLLoader.load(getClass().getResource("login_view.fxml"));
            Scene scene = new Scene(loginRoot);
            // Load main Css file
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());
            stage.setScene(scene);
            stage.setTitle("SkyPeek User Login");
        } catch (IOException e) { /* ... */ }
    }
}