package javafx_app.quizapp;

import javafx.animation.FadeTransition;
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
import javafx.util.Duration;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class RegistrationController implements Initializable {

    // FXML Elements
    @FXML
    private TextField firstname, lastname, username;
    @FXML
    private PasswordField newpassword, confirmpassword;
    @FXML
    private AnchorPane anchorPaneregister;
    @FXML
    private ImageView gifview;
    @FXML
    private TextField visibleNewPassword;
    @FXML
    private TextField visibleConfirmPassword;
    @FXML
    private Button toggleNewPassword;
    @FXML
    private Button toggleConfirmPassword;

    // Initialization method
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeGifView();
        visibleNewPassword.textProperty().bindBidirectional(newpassword.textProperty());
        visibleConfirmPassword.textProperty().bindBidirectional(confirmpassword.textProperty());
    }

    // Initializes GIF animation in the view
    private void initializeGifView() {
        try {
            Image gif = new Image(getClass().getResource("/img/admin.gif").toExternalForm());
            gifview.setImage(gif);
            gifview.setFitWidth(100);
            gifview.setFitHeight(100);
            gifview.setPreserveRatio(true);
            gifview.setSmooth(true);

            Platform.runLater(() -> {
                Rectangle clip = new Rectangle(gifview.getFitWidth(), gifview.getFitHeight());
                clip.setArcWidth(30);
                clip.setArcHeight(30);
                gifview.setClip(clip);
            });

            applyFadeTransition(anchorPaneregister, 0.0, 1.0, 500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Toggle visibility of the new password field
    @FXML
    private void toggleNewPasswordVisibility() {
        togglePasswordVisibility(visibleNewPassword, newpassword, toggleNewPassword);
    }

    // Toggle visibility of the confirm password field
    @FXML
    private void toggleConfirmPasswordVisibility() {
        togglePasswordVisibility(visibleConfirmPassword, confirmpassword, toggleConfirmPassword);
    }

    // Helper method to toggle visibility of password fields
    private void togglePasswordVisibility(TextField visibleField, PasswordField hiddenField, Button toggleButton) {
        boolean isVisible = visibleField.isVisible();
        visibleField.setVisible(!isVisible);
        visibleField.setManaged(!isVisible);
        hiddenField.setVisible(isVisible);
        hiddenField.setManaged(isVisible);
        toggleButton.setText(isVisible ? "👁" : "\uD83D\uDEE9");
    }

    // Navigate to user login page
    @FXML
    public void callUserLogin(ActionEvent event) throws Exception {
        Parent node = FXMLLoader.load(getClass().getResource("User.fxml"));
        Stage stage = (Stage) anchorPaneregister.getScene().getWindow();

        applyFadeTransition(anchorPaneregister, 1.0, 0.0, 500).setOnFinished(e -> {
            stage.setScene(new Scene(node));
            stage.centerOnScreen();
        });
    }

    // Handle user registration
    @FXML
    public void registerUser(ActionEvent event) {
        String fname = firstname.getText().trim();
        String lname = lastname.getText().trim();
        String uname = username.getText().trim().toLowerCase();
        String newpwd = newpassword.getText();
        String confpwd = confirmpassword.getText();

        // Validate input fields
        if (isInvalidInput(fname, lname, uname, newpwd, confpwd)) return;

        // Validate password length and match
        if (!newpwd.equals(confpwd)) {
            Notification.showWarning("Password didn't match.", (Stage) anchorPaneregister.getScene().getWindow());
            return;
        }

        if (newpwd.length() < 6) {
            Notification.showInfo("Password must be atleast 6 characters.", (Stage) anchorPaneregister.getScene().getWindow());
            return;
        }

        // Generate password salt and hash
        String salt = generateSalt();
        String hashedPassword = hashPassword(newpwd, salt);

        // Database interaction
        String url = "jdbc:sqlite:quiz.db";
        String checkQuery = "SELECT * FROM user WHERE user_id = ?";
        String insertQuery = "INSERT INTO user (first_name, last_name, user_id, password) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (isUserAlreadyExists(conn, uname)) {
                Notification.showInfo("User ID already taken.", (Stage) anchorPaneregister.getScene().getWindow());
                return;
            }

            if (registerNewUser(conn, fname, lname, uname, hashedPassword, salt)) {
                Notification.showSuccess("Registration successful for " + fname + " " + lname, (Stage) anchorPaneregister.getScene().getWindow());
                clearFields();
            } else {
                Notification.showWarning("Registration failed... Please try again later", (Stage) anchorPaneregister.getScene().getWindow());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Validate input fields for registration
    private boolean isInvalidInput(String fname, String lname, String uname, String newpwd, String confpwd) {
        Pattern pr = Pattern.compile("^([A-Za-z]+)([0-9]+)?([A-Za-z0-9\\.\\_]+)?\\@(([A-Za-z]+)([0-9]+)?([A-Za-z0-9\\.\\_]+)?)((\\.)([a-zA-Z]+))$");

        if (fname.isEmpty() || lname.isEmpty() || uname.isEmpty() || newpwd.isEmpty() || confpwd.isEmpty()) {
            Notification.showWarning("All fields are necessary.", (Stage) anchorPaneregister.getScene().getWindow());
            return true;
        }
        if (!pr.matcher(uname).matches()) {
            Notification.showWarning("Provide a valid User mail ID", (Stage) anchorPaneregister.getScene().getWindow());
            return true;
        }
        return false;
    }

    // Check if the user already exists in the database
    private boolean isUserAlreadyExists(Connection conn, String uname) throws SQLException {
        String checkQuery = "SELECT * FROM user WHERE user_id = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
        checkStmt.setString(1, uname);
        ResultSet rs = checkStmt.executeQuery();
        return rs.next();
    }

    // Register a new user in the database
    private boolean registerNewUser(Connection conn, String fname, String lname, String uname, String hashedPassword, String salt) throws SQLException {
        String insertQuery = "INSERT INTO user (first_name, last_name, user_id, password) VALUES (?, ?, ?, ?)";
        PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
        insertStmt.setString(1, fname);
        insertStmt.setString(2, lname);
        insertStmt.setString(3, uname);
        insertStmt.setString(4, hashedPassword + ":" + salt);
        int rowsAffected = insertStmt.executeUpdate();
        return rowsAffected > 0;
    }

    // Clear input fields after successful registration
    private void clearFields() {
        firstname.clear();
        lastname.clear();
        username.clear();
        newpassword.clear();
        confirmpassword.clear();
    }

    // Apply fade transition to a pane
    private FadeTransition applyFadeTransition(AnchorPane pane, double from, double to, int millis) {
        FadeTransition ft = new FadeTransition(Duration.millis(millis), pane);
        ft.setFromValue(from);
        ft.setToValue(to);
        ft.play();
        return ft;
    }

    // Generate a random salt for password hashing
    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Hash the password using SHA-256 and salt
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
}