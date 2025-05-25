1.1 Project Definition and Objectives

SkyPeek is a JavaFX-based desktop application whose goal is to present current and forecasted weather quickly through a polished, user friendly interface.
Included Features:

•	User Authentication: Users can log in; credentials and profiles are stored on a remote server rather than locally.

•	City Search: Retrieve current weather and forecast by entering any valid city name.

•	Popular Cities Display: The app automatically shows weather information for a predefined list of popular cities on startup.

•	Dynamic Background: The application's background visually adapts to the current weather conditions of the displayed city, utilizing a selection of animated GIF. 

•	Unit Conversion: Toggle between metric (°C, mm) and imperial (°F, in) units. 

•	Project Info Link: A clickable hyperlink opens a github with detailed project documentation. 

•	Last-Session Persistence: The most recently searched city and the selected unit system are saved locally and restored on the next launch.

•	Error Handling: Informative error messages are displayed for network failures, invalid inputs, and other exceptional conditions.

2. Overall Architectural Approach
   
•	Client–Server Model: The system is split into a JavaFX client (the desktop application) and a simple HTTP server (SimpleUserServer) that handles registration and login.
2.1	    Client Side (JavaFX):

o	Views: Defined in FXML files (login_view.fxml, register_view.fxml, weather_view.fxml), which describe the UI structure.

o	Controllers: LoginController, RegisterController, and WeatherController manage user interactions, business logic, and API calls.

o	Styling: A centralized styles.css file controls the application’s look and feel.

 2.2  Server Side (SimpleUserServer):
 
o	Built using Java’s built in HttpServer, it exposes /register and /login endpoints.

o	User data (username and SHA 256–hashed passwords) are stored in a plain text file (users.txt).

o	Responds to client POST requests to authenticate or register users 
***********************************************************************
NOTE: If you want to start it on your own server, you can run SımpleUserServer in the server file (after configuring the port and IP settings according to your preferences). (My server is already up and running for now).
![resim](https://github.com/user-attachments/assets/f9cb1d49-cb96-42c8-a453-e7efa94c4589)
![resim](https://github.com/user-attachments/assets/35b81345-514c-4863-a8c2-05c908769ba5)

