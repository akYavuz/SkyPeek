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
import java.util.Objects;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;
    @FXML private Hyperlink registerLink;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String SERVER_BASE_URL = "http://5.231.26.86:8081"; // VDS IP!

    @FXML
    public void initialize() {
        messageLabel.setText("");
    }

    @FXML
    void handleLoginAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Username and password cannot be empty.");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        messageLabel.setText("Attempting login...");
        messageLabel.setStyle("-fx-text-fill: black;");


        Thread loginThread = new Thread(() -> {
            try {
                String formData = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                        "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_BASE_URL + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formData))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200 && response.body().contains("Login successful")) {
                        messageLabel.setText("Login Successful! Loading weather app...");
                        messageLabel.setStyle("-fx-text-fill: green;");

                        loadWeatherApplication();
                    } else {
                        messageLabel.setText("Login Failed: " + response.body());
                        messageLabel.setStyle("-fx-text-fill: red;");
                    }
                });

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    messageLabel.setText("Error connecting to server: " + e.getMessage());
                    messageLabel.setStyle("-fx-text-fill: red;");
                });
            }
        });
        loginThread.setDaemon(true);
        loginThread.start();
    }

    @FXML
    void handleRegisterLinkAction(ActionEvent event) {
        try {
            Stage stage = (Stage) registerLink.getScene().getWindow();
            Parent registerRoot = FXMLLoader.load(getClass().getResource("register_view.fxml"));
            Scene scene = new Scene(registerRoot);
            // we load old login css file no diff
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());
            stage.setScene(scene);
            stage.setTitle("SkyPeek Registration");
        } catch (IOException e) { /* ... */ }
    }

    private void loadWeatherApplication() {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();

            Parent weatherRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("weather_view.fxml")));
            Scene weatherScene = new Scene(weatherRoot); // Use FXML

            String mainCssPath = "styles.css";
            weatherScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(mainCssPath)).toExternalForm());

            stage.setScene(weatherScene);
            stage.setTitle("SkyPeek");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Failed to load weather application.");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
}