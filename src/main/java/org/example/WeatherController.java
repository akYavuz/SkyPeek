package org.example;

import javafx.application.Platform;
import javafx.event.ActionEvent; // Required if methods take ActionEvent
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.Desktop;         // For opening web browser
import java.io.IOException;
import java.net.URI;             // For opening web browser
import java.net.URISyntaxException; // For opening web browser
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class WeatherController {

    // --- FXML Fields ---
    @FXML private StackPane rootStackPane;
    @FXML private ImageView backgroundImageView;
    @FXML private TextField cityInput;
    @FXML private VBox todayWeatherContainer;
    @FXML private Label currentCityNameLabel;
    @FXML private Label todayFullDateLabel;
    @FXML private ImageView todayWeatherIcon;
    @FXML private Label todayTemperatureLabel;
    @FXML private Label todayDescriptionLabel;
    @FXML private Label todayMinMaxLabel;
    @FXML private Label todayMainDescriptionLabel;
    @FXML private Label todayHumidityLabel;
    @FXML private Label todayWindSpeedLabel;
    @FXML private Label todayPopLabel;
    @FXML private ScrollPane otherDaysScrollPane;
    @FXML private FlowPane otherDaysFlowPane;
    @FXML private Label errorLabel;
    @FXML private HBox fixedCitiesHBox;
    @FXML private Hyperlink infoLink;
    @FXML private ToggleButton unitToggle;

    // --- Constants and Variables ---
    private static final String API_KEY = "7825f07ddb68dfd7418787832e21b86a"; //API KEY
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final DateTimeFormatter dayNameFormatter = DateTimeFormatter.ofPattern("E", Locale.ENGLISH);
    private final DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy, EEEE", Locale.ENGLISH);
    private final String[] PREDEFINED_CITIES = {"Ankara", "Bayburt", "London", "Paris", "Tokyo"};
    private final boolean FETCH_FIXED_CITIES_WEATHER = true;

    private final Preferences prefs = Preferences.userNodeForPackage(WeatherController.class);
    private static final String PREF_LAST_CITY = "lastSearchedCity";
    private static final String PREF_LAST_UNIT = "lastSelectedUnit"; // For unit preference

    // Current selected unit, symbol, and wind speed unit
    private String currentUnitsParameter = "metric"; // For API: "metric" (C) or "imperial" (F)
    private String currentTemperatureSymbol = "°C";
    private String currentWindSpeedUnit = "km/h";

    @FXML
    public void initialize() {
        if (rootStackPane != null && backgroundImageView != null) {
            backgroundImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            backgroundImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
        }
        updateBackground("default");
        loadUnitPreference();

        if (unitToggle != null) {
            unitToggle.setSelected(currentUnitsParameter.equals("imperial"));
        }

        clearUIArea(false); // clearFixedCitiesToo is passed as false

        if (FETCH_FIXED_CITIES_WEATHER && PREDEFINED_CITIES != null && PREDEFINED_CITIES.length > 0) {
            if (fixedCitiesHBox != null) {
                fixedCitiesHBox.setVisible(true);
                fixedCitiesHBox.setManaged(true);
                loadFixedCitiesWeather();
            }
        }

        String lastCity = getLastSearchedCity();
        if (lastCity != null && !lastCity.isEmpty()) {
            cityInput.setText(lastCity);
            if (currentCityNameLabel != null) {
                currentCityNameLabel.setText("Loading: " + lastCity + "...");
            }
            fetchWeatherData(lastCity, true);
        }
    }

    private void loadUnitPreference() {
        String savedUnit = prefs.get(PREF_LAST_UNIT, "metric"); // Default to Celsius
        if (savedUnit.equals("imperial")) {
            currentUnitsParameter = "imperial";
            currentTemperatureSymbol = "°F";
            currentWindSpeedUnit = "mph";
        } else {
            currentUnitsParameter = "metric";
            currentTemperatureSymbol = "°C";
            currentWindSpeedUnit = "km/h";
        }
    }

    private void saveUnitPreference() {
        prefs.put(PREF_LAST_UNIT, currentUnitsParameter);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            System.err.println("Error saving unit preference: " + e.getMessage());
        }
    }

    private void clearUIArea(boolean clearFixedCitiesToo) {
        if (errorLabel != null) { errorLabel.setText(""); errorLabel.setVisible(false); errorLabel.setManaged(false); }
        if (todayWeatherContainer != null) {
            if (currentCityNameLabel != null && (currentCityNameLabel.getText() == null || !currentCityNameLabel.getText().startsWith("Loading:"))) {
                currentCityNameLabel.setText("");
            }
            if (todayFullDateLabel != null) todayFullDateLabel.setText("");
            if (todayWeatherIcon != null) todayWeatherIcon.setImage(null);
            if (todayTemperatureLabel != null) todayTemperatureLabel.setText("");
            if (todayDescriptionLabel != null) todayDescriptionLabel.setText("");
            if (todayMinMaxLabel != null) todayMinMaxLabel.setText("");
            if (todayMainDescriptionLabel != null) todayMainDescriptionLabel.setText("");
            if (todayHumidityLabel != null) todayHumidityLabel.setText("");
            if (todayWindSpeedLabel != null) todayWindSpeedLabel.setText("");
            if (todayPopLabel != null) todayPopLabel.setText("");
            todayWeatherContainer.setVisible(false);
            todayWeatherContainer.setManaged(false);
        }
        if (otherDaysFlowPane != null) otherDaysFlowPane.getChildren().clear();
        if (otherDaysScrollPane != null) { otherDaysScrollPane.setVisible(false); otherDaysScrollPane.setManaged(false); }
        if (clearFixedCitiesToo && fixedCitiesHBox != null) {
            fixedCitiesHBox.getChildren().clear();
            fixedCitiesHBox.setVisible(false);
            fixedCitiesHBox.setManaged(false);
        }
    }

    @FXML
    protected void handleSearchAction() { // Kept protected as it's called from FXML
        String cityName = cityInput.getText().trim();
        if (cityName.isEmpty()) {
            displayError("Please enter a city name.");
            return;
        }
        clearUIArea(false);
        if (currentCityNameLabel != null) currentCityNameLabel.setText("Loading: " + cityName + "...");
        fetchWeatherData(cityName, false);
    }

    // Method to get the currently displayed city name, if valid
    private String getCurrentlyDisplayedCityName() {
        if (currentCityNameLabel != null && !currentCityNameLabel.getText().isEmpty() &&
                !currentCityNameLabel.getText().startsWith("Loading:") &&
                !currentCityNameLabel.getText().equalsIgnoreCase("Error Occurred") &&
                !currentCityNameLabel.getText().equalsIgnoreCase("Not Found") &&
                !currentCityNameLabel.getText().contains("Data Error") &&
                !currentCityNameLabel.getText().equalsIgnoreCase("API Error") &&
                !currentCityNameLabel.getText().trim().isEmpty()) {
            return currentCityNameLabel.getText().trim();
        }
        return null; // Return null if no valid city name is displayed
    }

    @FXML
    private void handleUnitToggleAction(ActionEvent event) { // ActionEvent parameter kept
        if (unitToggle.isSelected()) {
            currentUnitsParameter = "imperial";
            currentTemperatureSymbol = "°F";
            currentWindSpeedUnit = "mph";
        } else {
            currentUnitsParameter = "metric";
            currentTemperatureSymbol = "°C";
            currentWindSpeedUnit = "km/h";
        }
        saveUnitPreference();

        String displayedCity = getCurrentlyDisplayedCityName();

        if (displayedCity != null) { // Re-fetch data only if a valid city is displayed
            clearUIArea(false);
            if (currentCityNameLabel != null) currentCityNameLabel.setText("Loading: " + displayedCity + "...");
            fetchWeatherData(displayedCity, false);
        }

        if (FETCH_FIXED_CITIES_WEATHER && PREDEFINED_CITIES != null && PREDEFINED_CITIES.length > 0) {
            loadFixedCitiesWeather();
        }
    }

    private void fetchWeatherData(String cityName, boolean isInitialStaticLoad) {
        Thread apiCallThread = new Thread(() -> {
            try {
                String geocodingUrl = String.format("https://api.openweathermap.org/geo/1.0/direct?q=%s&limit=1&appid=%s", cityName.replace(" ", "%20"), API_KEY);
                HttpRequest geoRequest = HttpRequest.newBuilder().uri(URI.create(geocodingUrl)).build();
                HttpResponse<String> geoResponse = httpClient.send(geoRequest, HttpResponse.BodyHandlers.ofString());

                if (geoResponse.statusCode() != 200) { final String errorMsg = "Geo API Error: " + geoResponse.statusCode(); Platform.runLater(() -> displayError(errorMsg)); return; }
                JSONArray geoArray = new JSONArray(geoResponse.body());
                if (geoArray.isEmpty()) { final String errorMsg = "City not found: " + cityName; Platform.runLater(() -> displayError(errorMsg)); return; }

                JSONObject geoData = geoArray.getJSONObject(0);
                double lat = geoData.getDouble("lat");
                double lon = geoData.getDouble("lon");
                String extractedFoundCityName = geoData.optString("local_names_en", geoData.optString("name", cityName));
                if (geoData.has("local_names") && geoData.getJSONObject("local_names").has("en")) { extractedFoundCityName = geoData.getJSONObject("local_names").getString("en"); }
                final String foundCityName = extractedFoundCityName;
                saveLastSearchedCity(foundCityName);

                String weatherUrl = String.format("https://api.openweathermap.org/data/3.0/onecall?lat=%f&lon=%f&exclude=current,minutely,hourly,alerts&units=%s&lang=en&appid=%s", lat, lon, currentUnitsParameter, API_KEY);
                HttpRequest weatherRequest = HttpRequest.newBuilder().uri(URI.create(weatherUrl)).build();
                HttpResponse<String> weatherResponse = httpClient.send(weatherRequest, HttpResponse.BodyHandlers.ofString());

                if (weatherResponse.statusCode() != 200) { final String errorMsg = "Weather API Error: " + weatherResponse.statusCode(); Platform.runLater(() -> displayError(errorMsg)); return; }
                JSONObject weatherData = new JSONObject(weatherResponse.body());
                final String iconCodeForBackground = getIconFromWeatherData(weatherData);

                Platform.runLater(() -> {
                    processAndDisplayWeatherData(weatherData, foundCityName);
                    if (!isInitialStaticLoad) { updateBackground(iconCodeForBackground); }
                });

            } catch (IOException | InterruptedException | JSONException e) {
                e.printStackTrace(); // Keep for debugging during development
                Platform.runLater(() -> { String errMsg = "API/Data Error: " + e.getMessage(); if (e instanceof JSONException) errMsg = "Data Parsing Error."; displayError(errMsg); });
            }
        });
        apiCallThread.setDaemon(true); apiCallThread.start();
    }

    private String getIconFromWeatherData(JSONObject weatherData) {
        JSONArray dailyForecasts = weatherData.optJSONArray("daily");
        if (dailyForecasts != null && !dailyForecasts.isEmpty()) {
            JSONObject todayForecast = dailyForecasts.getJSONObject(0);
            JSONArray weatherArray = todayForecast.optJSONArray("weather");
            if (weatherArray != null && !weatherArray.isEmpty()) { return weatherArray.getJSONObject(0).getString("icon"); }
        }
        return "default";
    }

    private void processAndDisplayWeatherData(JSONObject weatherData, String cityName) {
        if (errorLabel != null) { errorLabel.setVisible(false); errorLabel.setManaged(false); errorLabel.setText("");}
        if (currentCityNameLabel != null) currentCityNameLabel.setText(cityName);
        JSONArray dailyForecasts = weatherData.optJSONArray("daily");
        if (dailyForecasts == null || dailyForecasts.isEmpty()) { displayError("Daily forecast data unavailable."); if (todayWeatherContainer != null) {todayWeatherContainer.setVisible(false); todayWeatherContainer.setManaged(false);} return; }
        JSONObject todayForecastData = dailyForecasts.getJSONObject(0);
        displayTodayWeather(todayForecastData);
        if (otherDaysFlowPane != null && otherDaysScrollPane != null) {
            otherDaysFlowPane.getChildren().clear();
            for (int i = 1; i < dailyForecasts.length() && i < 7; i++) {
                JSONObject dayForecastData = dailyForecasts.getJSONObject(i);
                VBox dayCard = createSmallForecastCard(dayForecastData);
                if (dayCard != null) { otherDaysFlowPane.getChildren().add(dayCard); }
            }
            if (!otherDaysFlowPane.getChildren().isEmpty()) { otherDaysScrollPane.setVisible(true); otherDaysScrollPane.setManaged(true); }
            else { otherDaysScrollPane.setVisible(false); otherDaysScrollPane.setManaged(false); }
        }
    }

    private void displayTodayWeather(JSONObject todayData) {
        if (todayWeatherContainer == null || todayData == null) { return; }
        todayWeatherContainer.setVisible(true); todayWeatherContainer.setManaged(true);
        long dt = todayData.getLong("dt"); LocalDate date = Instant.ofEpochSecond(dt).atZone(ZoneId.systemDefault()).toLocalDate();
        if (todayFullDateLabel != null) { todayFullDateLabel.setText(date.format(fullDateFormatter)); }
        JSONObject temp = todayData.getJSONObject("temp");
        double dayTemp = temp.getDouble("day"); double minTemp = temp.getDouble("min"); double maxTemp = temp.getDouble("max");
        JSONArray weatherArray = todayData.optJSONArray("weather");
        String detailedDescription = "N/A"; String mainWeatherCondition = "N/A"; String iconCode = "01d";
        if (weatherArray != null && !weatherArray.isEmpty()) { JSONObject weather = weatherArray.getJSONObject(0); detailedDescription = weather.getString("description"); detailedDescription = detailedDescription.substring(0, 1).toUpperCase() + detailedDescription.substring(1); mainWeatherCondition = weather.optString("main", detailedDescription); iconCode = weather.getString("icon"); }
        if (todayWeatherIcon != null) { try { Image image = new Image("https://openweathermap.org/img/wn/" + iconCode + "@4x.png", true); todayWeatherIcon.setImage(image); } catch (Exception e) { todayWeatherIcon.setImage(null); } }
        if (todayTemperatureLabel != null) todayTemperatureLabel.setText(String.format("%.0f%s", dayTemp, currentTemperatureSymbol));
        if (todayDescriptionLabel != null) todayDescriptionLabel.setText(detailedDescription);
        if (todayMinMaxLabel != null) todayMinMaxLabel.setText(String.format("L: %.0f° / H: %.0f°", minTemp, maxTemp));
        if (todayMainDescriptionLabel != null) todayMainDescriptionLabel.setText(mainWeatherCondition);
        int humidity = todayData.optInt("humidity", -1);
        double windSpeedVal = todayData.optDouble("wind_speed", -1.0);
        double pop = todayData.optDouble("pop", -1.0) * 100;
        if (todayHumidityLabel != null) todayHumidityLabel.setText(humidity != -1 ? String.format("%d%%", humidity) : "N/A");
        if (todayWindSpeedLabel != null) {
            if (windSpeedVal != -1.0) {
                double displayWindSpeed = windSpeedVal;
                if (currentUnitsParameter.equals("metric")) { displayWindSpeed = windSpeedVal * 3.6; }
                todayWindSpeedLabel.setText(String.format("%.1f %s", displayWindSpeed, currentWindSpeedUnit));
            } else { todayWindSpeedLabel.setText("N/A"); }
        }
        if (todayPopLabel != null) todayPopLabel.setText(pop != -100.0 ? String.format("%.0f%%", pop) : "N/A");
    }

    private VBox createSmallForecastCard(JSONObject dayData) {
        if (dayData == null) { return null; }
        VBox card = new VBox(5); card.getStyleClass().add("small-forecast-card"); card.setAlignment(Pos.CENTER);
        try {
            long dt = dayData.getLong("dt"); LocalDate date = Instant.ofEpochSecond(dt).atZone(ZoneId.systemDefault()).toLocalDate();
            JSONObject temp = dayData.getJSONObject("temp");
            double dayTempForecast = temp.getDouble("day");
            JSONArray weatherArray = dayData.optJSONArray("weather"); String iconCode = "01d";
            if (weatherArray != null && !weatherArray.isEmpty()) { JSONObject weatherObject = weatherArray.optJSONObject(0); if (weatherObject != null) { iconCode = weatherObject.getString("icon"); } }
            Label dateLabel = new Label(date.format(dayNameFormatter)); dateLabel.getStyleClass().add("small-card-day-name");
            ImageView iconView = new ImageView();
            try { Image image = new Image("https://openweathermap.org/img/wn/" + iconCode + "@2x.png", true); iconView.setImage(image); iconView.setFitHeight(28); iconView.setFitWidth(28); } catch (Exception e) { /* Leave icon blank on error */ }
            Label tempLabel = new Label(String.format("%.0f°", dayTempForecast));
            tempLabel.getStyleClass().add("small-card-temperature");
            card.getChildren().addAll(dateLabel, iconView, tempLabel);
            return card;
        } catch (JSONException e) { System.err.println("Error creating small forecast card: " + e.getMessage()); return null; }
    }

    private void loadFixedCitiesWeather() {
        if (fixedCitiesHBox == null) { return; }
        fixedCitiesHBox.setVisible(true); fixedCitiesHBox.setManaged(true);
        fixedCitiesHBox.getChildren().clear();
        if (!FETCH_FIXED_CITIES_WEATHER || PREDEFINED_CITIES.length == 0) { fixedCitiesHBox.setVisible(false); fixedCitiesHBox.setManaged(false); return; }
        for (String cityName : PREDEFINED_CITIES) {
            final String currentFixedCity = cityName;
            Thread fixedCityThread = new Thread(() -> {
                try {
                    String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=%s&lang=en", currentFixedCity.replace(" ", "%20"), API_KEY, currentUnitsParameter);
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        JSONObject data = new JSONObject(response.body());
                        Platform.runLater(() -> { VBox cityCard = createFixedCityWeatherCard(data); if (cityCard != null) { fixedCitiesHBox.getChildren().add(cityCard); } });
                    } else { System.err.println("Fixed City (" + currentFixedCity + ") Error: " + response.statusCode()); }
                } catch (Exception e) { System.err.println("Fixed City (" + currentFixedCity + ") Fetch Error: " + e.getMessage()); }
            });
            fixedCityThread.setDaemon(true); fixedCityThread.start();
        }
    }

    private VBox createFixedCityWeatherCard(JSONObject cityWeatherData) {
        if (cityWeatherData == null) { return null; }
        String cityNameVal = cityWeatherData.optString("name", "N/A");
        JSONObject main = cityWeatherData.optJSONObject("main");
        JSONArray weatherArray = cityWeatherData.optJSONArray("weather");
        JSONObject weatherObject = (weatherArray != null && !weatherArray.isEmpty()) ? weatherArray.optJSONObject(0) : null;
        if (main == null || weatherObject == null) { System.err.println("Fixed city data incomplete for: " + cityNameVal); return null; }
        double tempVal = main.optDouble("temp", 0.0);
        String iconCodeVal = weatherObject.optString("icon", "01d");
        VBox card = new VBox(3); card.getStyleClass().add("fixed-city-card"); card.setAlignment(Pos.CENTER);
        Label nameLabel = new Label(cityNameVal); nameLabel.getStyleClass().add("fixed-city-name");
        ImageView iconView = new ImageView();
        try { Image image = new Image("https://openweathermap.org/img/wn/" + iconCodeVal + ".png", true); iconView.setImage(image); iconView.setFitHeight(30); iconView.setFitWidth(30);
        } catch (Exception e) { /* Leave icon blank on error */ }
        Label tempLabel = new Label(String.format("%.0f°", tempVal));
        tempLabel.getStyleClass().add("fixed-city-temp");
        card.getChildren().addAll(nameLabel, iconView, tempLabel);
        return card;
    }

    @FXML
    private void handleInfoLinkAction(ActionEvent event) { // ActionEvent parameter kept
        String projectInfoUrl = "https://github.com/akYavuz/SkyPeek"; // !!! CHANGE THIS URL !!!
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try { desktop.browse(new URI(projectInfoUrl)); } catch (IOException | URISyntaxException e) { e.printStackTrace(); displayTemporaryError("Could not open web page: " + e.getMessage()); }
            } else { displayTemporaryError("Browser action not supported on this platform."); }
        } else { displayTemporaryError("Desktop features not supported. Link: " + projectInfoUrl); }
    }

    private void displayTemporaryError(String message) {
        if (errorLabel != null) { errorLabel.setText(message); errorLabel.setVisible(true); errorLabel.setManaged(true); }
    }

    private void updateBackground(String conditionIndicator) {
        if (backgroundImageView == null) { return; }
        String imageName = "default_bg.jpg";
        if (conditionIndicator == null) conditionIndicator = "default";
        switch (conditionIndicator.length() >= 2 ? conditionIndicator.substring(0, 2) : "xx") {
            case "01": imageName = "sunny.gif"; break; case "02": case "03": case "04": imageName = "cloudy.gif"; break;
            case "09": case "10": imageName = "rainy.gif"; break; case "11": imageName = "stormy.gif"; break;
            case "13": imageName = "snowy.gif"; break; case "50": imageName = "misty.gif"; break;
            default: if ("error".equals(conditionIndicator) || "not_found".equals(conditionIndicator)) { imageName = "error_bg.jpg"; } break;
        }
        try { String imagePath = "/images/" + imageName; Image bgImage = new Image(Objects.requireNonNull(getClass().getResource(imagePath)).toExternalForm()); backgroundImageView.setImage(bgImage);
        } catch (Exception e) { loadFallbackBackground(); }
    }

    private void loadFallbackBackground() {
        if (backgroundImageView == null) { return; }
        String actualFallbackPath = "/images/default_bg.jpg";
        try { Image fallbackImage = new Image(Objects.requireNonNull(getClass().getResource(actualFallbackPath)).toExternalForm()); backgroundImageView.setImage(fallbackImage);
        } catch (Exception ex) { backgroundImageView.setImage(null); }
    }

    private void displayError(String message) {
        clearUIArea(false);
        if (currentCityNameLabel != null) currentCityNameLabel.setText("Error Occurred");
        if (errorLabel != null) { errorLabel.setText(message); errorLabel.setVisible(true); errorLabel.setManaged(true); }
        updateBackground("error");
    }

    private void saveLastSearchedCity(String cityName) {
        if (cityName != null && !cityName.isEmpty()) {
            prefs.put(PREF_LAST_CITY, cityName);
            try { prefs.flush(); } catch (BackingStoreException e) { System.err.println("Error saving city preference: " + e.getMessage()); }
        }
    }

    private String getLastSearchedCity() {
        return prefs.get(PREF_LAST_CITY, null);
    }
}