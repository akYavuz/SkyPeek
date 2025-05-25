package org.example.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SimpleUserServer {

    private static final String USERS_FILE_PATH = "users.txt";
    private static Map<String, String> usersCredentials = new HashMap<>(); // username -> hashedPassword

    public static void main(String[] args) throws IOException {
        loadUsersFromFile();

        // Port addres
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        System.out.println("Attempting to start server on all interfaces, port 8081...");

        server.createContext("/register", new RegisterHandler());
        server.createContext("/login", new LoginHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("SimpleUserServer started successfully on port 8081.");
        System.out.println("Listening on all available network interfaces.");
        System.out.println("Endpoints: /register (POST), /login (POST)");
        System.out.println("User data will be stored in: " + new File(USERS_FILE_PATH).getAbsolutePath());
    }

    // Helper method to hash passwords using SHA-256
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            // This should ideally not happen as SHA-256 is standard
            System.err.println("Critical Error: SHA-256 Algorithm not found. " + e.getMessage());
            return "PLAIN:" + password; // Mark as plain if hashing fails
        }
    }

    private static synchronized void loadUsersFromFile() {
        File usersFile = new File(USERS_FILE_PATH);
        usersCredentials.clear();
        if (usersFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(usersFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2 && !parts[0].trim().isEmpty()) {
                            usersCredentials.put(parts[0].trim(), parts[1]); // Store as is (hashed passwords from file)
                        }
                    }
                }
                System.out.println("Users loaded from " + usersFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error loading users from file: " + e.getMessage());
            }
        } else {
            System.out.println(USERS_FILE_PATH + " not found. Will be created on first registration.");
        }
    }

    private static synchronized boolean saveUserToFile(String username, String password) {
        String trimmedUsername = username.trim();
        if (usersCredentials.containsKey(trimmedUsername)) {
            System.out.println("Registration attempt for existing user: " + trimmedUsername);
            return false;
        }
        String hashedPassword = hashPassword(password); // Hash the password before saving
        if (hashedPassword.startsWith("PLAIN:")) { // Check if hashing failed
            System.err.println("Password hashing failed for user: " + trimmedUsername + ". Registration aborted.");
            return false; // Do not save plain password if hashing was intended but failed
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE_PATH, StandardCharsets.UTF_8, true))) {
            writer.write(trimmedUsername + ":" + hashedPassword); // Save the hashed password
            writer.newLine();
            usersCredentials.put(trimmedUsername, hashedPassword); // Store hashed password in memory map
            System.out.println("User registered and saved to file: " + trimmedUsername);
            return true;
        } catch (IOException e) {
            System.err.println("Error saving user to file: " + e.getMessage());
            return false;
        }
    }

    private static Map<String, String> parseRequestParameters(HttpExchange exchange) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        String query = null;
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {
                query = br.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } else if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            URI requestedUri = exchange.getRequestURI();
            query = requestedUri.getRawQuery();
        }

        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0 && idx < pair.length() - 1) {
                    parameters.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name()),
                            URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()));
                } else if (idx == -1 && !pair.isEmpty()){
                    parameters.put(URLDecoder.decode(pair, StandardCharsets.UTF_8.name()), "");
                }
            }
        }
        System.out.println("Parsed parameters: " + parameters);
        return parameters;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String responseMessage) throws IOException {
        byte[] responseBytes = responseMessage.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String responseMessage;
            int statusCode = 200;
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseRequestParameters(exchange);
                String username = params.get("username");
                String password = params.get("password");
                System.out.println("Register attempt: user=" + username + ", pass_len=" + (password != null ? password.length() : "null"));
                if (username != null && !username.trim().isEmpty() && password != null && !password.isEmpty()) {
                    if (saveUserToFile(username, password)) { // Password will be hashed inside saveUserToFile
                        responseMessage = "Registration successful for " + username.trim() + "!";
                    } else {
                        responseMessage = "Username '" + username.trim() + "' already exists or error saving/hashing.";
                        statusCode = 409;
                    }
                } else {
                    responseMessage = "Username or password cannot be empty.";
                    statusCode = 400;
                }
            } else {
                responseMessage = "Only POST method is allowed for /register.";
                statusCode = 405;
            }
            sendResponse(exchange, statusCode, responseMessage);
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String responseMessage;
            int statusCode = 200;
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseRequestParameters(exchange);
                String username = params.get("username");
                String password = params.get("password");
                System.out.println("Login attempt: user=" + username + ", pass_len=" + (password != null ? password.length() : "null"));

                if (username != null && password != null) {
                    String trimmedUsername = username.trim();
                    String hashedPasswordAttempt = hashPassword(password); // Hash the provided password for comparison

                    // Check if hashing failed
                    if (hashedPasswordAttempt.startsWith("PLAIN:")) {
                        responseMessage = "Server hashing error during login.";
                        statusCode = 500; // Internal Server Error
                    } else if (usersCredentials.containsKey(trimmedUsername) &&
                            usersCredentials.get(trimmedUsername).equals(hashedPasswordAttempt)) {
                        responseMessage = "Login successful for " + trimmedUsername + "!";
                    } else {
                        responseMessage = "Invalid username or password.";
                        statusCode = 401;
                    }
                } else {
                    responseMessage = "Username or password cannot be null.";
                    statusCode = 400;
                }
            } else {
                responseMessage = "Only POST method is allowed for /login.";
                statusCode = 405;
            }
            sendResponse(exchange, statusCode, responseMessage);
        }
    }
}