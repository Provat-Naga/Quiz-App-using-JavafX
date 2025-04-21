package javafx_app.quizapp;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class UserController implements Initializable {
    // Static variable to store the logged-in user's full name
    public static String loggedInFullName;

    // FXML components
    @FXML
    private AnchorPane anchorPaneuser;
    @FXML
    private TextField uid;
    @FXML
    private ImageView gifview;
    @FXML
    private PasswordField upass;
    @FXML
    private TextField visiblePass;
    @FXML
    private Button togglePassword;

    private boolean isPasswordVisible = false;

    // Method to toggle password visibility
    @FXML
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            upass.setText(visiblePass.getText());
            visiblePass.setVisible(false);
            visiblePass.setManaged(false);
            upass.setVisible(true);
            upass.setManaged(true);
            togglePassword.setText("👁");
        } else {
            visiblePass.setText(upass.getText());
            visiblePass.setVisible(true);
            visiblePass.setManaged(true);
            upass.setVisible(false);
            upass.setManaged(false);
            togglePassword.setText("\uD83D\uDEE9");
        }
        isPasswordVisible = !isPasswordVisible;
    }

    // Method to initialize the view (GIF animation and visual styling)
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Set GIF for visual display
            Image gif = new Image(getClass().getResource("/img/admin.gif").toExternalForm());
            gifview.setImage(gif);
            gifview.setFitWidth(100);
            gifview.setFitHeight(100);
            gifview.setPreserveRatio(true);
            gifview.setSmooth(true);

            // Apply rounded corner clip to the GIF
            Platform.runLater(() -> {
                Rectangle clip = new Rectangle(gifview.getFitWidth(), gifview.getFitHeight());
                clip.setArcWidth(30);
                clip.setArcHeight(30);
                gifview.setClip(clip);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to handle user login action
    @FXML
    public void userLoginMethod(ActionEvent event) {
        String usrid = uid.getText().trim().toLowerCase();  // User ID
        String upaswd = isPasswordVisible ? visiblePass.getText().trim() : upass.getText().trim();

        // Email validation regex pattern
        Pattern pr = Pattern.compile("^([A-Za-z]+)([0-9]+)?([A-Za-z0-9\\.\\_]+)?\\@(([A-Za-z]+)([0-9]+)?([A-Za-z0-9\\.\\_]+)?)((\\.)([a-zA-Z]+))$");

        // Validating user input
        if (usrid.isEmpty()) {
            Notification.showWarning("USER ID cannot be empty.", (Stage) anchorPaneuser.getScene().getWindow());
            return;
        }
        if (!pr.matcher(usrid).matches()) {
            Notification.showWarning("Provide a valid User mail ID", (Stage) anchorPaneuser.getScene().getWindow());
            return;
        }
        if (upaswd.isEmpty()) {
            Notification.showWarning("Password cannot be empty.", (Stage) anchorPaneuser.getScene().getWindow());
            return;
        }

        // Validate credentials
        if (!validateAndStoreFullName(usrid, upaswd)) {
            Notification.showWarning("User ID or Password is incorrect.", (Stage) anchorPaneuser.getScene().getWindow());
            return;
        }

        Notification.showSuccess("Login successful!", (Stage) anchorPaneuser.getScene().getWindow());
        System.out.println("Login successful for: " + usrid + ", Full Name: " + loggedInFullName);

        // Redirect to user panel
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UserPanel.fxml"));
            Parent userPanelRoot = loader.load();

            UserPanelController controller = loader.getController();
            controller.setUserGreeting(loggedInFullName, usrid);  // Pass full name and user_id

            Stage stage = (Stage) anchorPaneuser.getScene().getWindow();
            stage.setScene(new Scene(userPanelRoot));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to validate user credentials and store the full name if valid
    private boolean validateAndStoreFullName(String userId, String inputPassword) {
        String url = "jdbc:sqlite:quiz.db";
        String query = "SELECT first_name, last_name, password FROM user WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    String[] parts = storedPassword.split(":");
                    if (parts.length != 2) return false;

                    String storedHash = parts[0];
                    String storedSalt = parts[1];

                    String inputHash = hashPassword(inputPassword, storedSalt);

                    if (storedHash.equals(inputHash)) {
                        String firstName = rs.getString("first_name");
                        String lastName = rs.getString("last_name");
                        loggedInFullName = ((firstName != null ? firstName : "") + " " +
                                (lastName != null ? lastName : "")).trim();
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Method to hash the password with the given salt
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedBytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // Method to navigate to the Registration page
    @FXML
    public void callUserRegistration(ActionEvent event) throws IOException {
        Parent node = FXMLLoader.load(getClass().getResource("Registration.fxml"));
        Stage stage = (Stage) anchorPaneuser.getScene().getWindow();
        stage.setScene(new Scene(node));
        stage.centerOnScreen();
    }

    // Method to navigate to the Admin login page
    @FXML
    public void callAdminLogin(ActionEvent event) throws IOException {
        Parent node = FXMLLoader.load(getClass().getResource("Admin.fxml"));
        Stage stage = (Stage) anchorPaneuser.getScene().getWindow();
        stage.setScene(new Scene(node));
        stage.centerOnScreen();
    }
}