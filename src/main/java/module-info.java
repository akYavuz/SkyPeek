module HavaDurumuApp {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    requires java.net.http;
    requires java.prefs;
    requires jdk.httpserver;
    requires java.desktop;


    opens org.example to javafx.fxml;
    exports org.example;

    opens org.example.server to javafx.fxml;
    exports org.example.server;
}