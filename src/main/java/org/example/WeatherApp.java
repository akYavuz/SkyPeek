package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class WeatherApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // we start with login screen
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("login_view.fxml")));
        Scene scene = new Scene(root); // Login screen (400x450)


        String cssPath = "styles.css";
        try {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm());
        } catch (NullPointerException e) {
            System.err.println("Main CSS file cant load: " + cssPath + ". Using only FXML.");
        }


        primaryStage.setTitle("SkyPeek - User Login"); // Title (Login)
        primaryStage.setScene(scene);
        //primaryStage.setResizable(false); // Resize true or false for login page
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}