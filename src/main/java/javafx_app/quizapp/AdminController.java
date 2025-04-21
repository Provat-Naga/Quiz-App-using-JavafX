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

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class AdminController implements Initializable {

    // Static variable to store admin full name
    public static String adminFullName;

    // UI components
    @FXML
    private TextField adminID;
    @FXML
    private PasswordField adminPassword;
    @FXML
    private TextField visiblePass;
    @FXML
    private Button togglePassword;
    @FXML
    private AnchorPane anchorPaneadmin;
    @FXML
    private ImageView gifview;

    // Flag to track password visibility
    private boolean isPasswordVisible = false;

    // Initialize method called automatically when FXML is loaded
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // Load and set animated gif
            Image gif = new Image(getClass().getResource("/img/admin.gif").toExternalForm());
            gifview.setImage(gif);
            gifview.setFitWidth(100);
            gifview.setFitHeight(100);
            gifview.setPreserveRatio(true);
            gifview.setSmooth(true);

            // Apply rounded corners to gif after layout is ready
            Platform.runLater(() -> {
                Rectangle clip = new Rectangle(gifview.getFitWidth(), gifview.getFitHeight());
                clip.setArcWidth(30);
                clip.setArcHeight(30);
                gifview.setClip(clip);
            });

            // Apply fade-in transition on login screen
            applyFadeTransition(anchorPaneadmin, 0.0, 1.0, 350);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Toggle password visibility between masked and visible
    @FXML
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            adminPassword.setText(visiblePass.getText());
            visiblePass.setVisible(false);
            visiblePass.setManaged(false);
            adminPassword.setVisible(true);
            adminPassword.setManaged(true);
            togglePassword.setText("👁");
        } else {
            visiblePass.setText(adminPassword.getText());
            visiblePass.setVisible(true);
            visiblePass.setManaged(true);
            adminPassword.setVisible(false);
            adminPassword.setManaged(false);
            togglePassword.setText("\uD83D\uDEE9");
        }
        isPasswordVisible = !isPasswordVisible;
    }

    // Handle admin login button click
    @FXML
    public void adminLoginMethod(ActionEvent event) {
        // For testing purposes, hardcoded credentials
//        String adminid = "provat.naga@gmail.com";
//        String adminpass = "provat321";
        String adminid = adminID.getText().trim().toLowerCase();
        String adminpass = isPasswordVisible ? visiblePass.getText().trim() : adminPassword.getText().trim();


        System.out.println(adminid + " \n " + adminpass);

        // Validate email format
        Pattern pr = Pattern.compile("^([A-Za-z]+)([0-9]+)?([A-Za-z0-9\\.\\_]+)?\\@(([A-Za-z]+)([0-9]+)?([A-Za-z0-9\\.\\_]+)?)((\\.)([a-zA-Z]+))$");

        if (adminid.isEmpty()) {
            Notification.showWarning("Admin ID cannot be empty", (Stage) anchorPaneadmin.getScene().getWindow());
            return;
        }

        if (!pr.matcher(adminid).matches()) {
            Notification.showWarning("Provide a valid Admin mail ID", (Stage) anchorPaneadmin.getScene().getWindow());
            return;
        }

        if (adminpass.isEmpty()) {
            Notification.showWarning("Password cannot be empty", (Stage) anchorPaneadmin.getScene().getWindow());
            return;
        }

        // Check credentials in the database
        if (!isValidAdmin(adminid, adminpass)) {
            Notification.showWarning("Admin ID or Password is incorrect.", (Stage) anchorPaneadmin.getScene().getWindow());
            return;
        }

        // Show success and navigate to admin panel
        Notification.showSuccess("Login Success !!!", (Stage) anchorPaneadmin.getScene().getWindow());
        System.out.println("Login successful for: " + adminid + " Fullname: " + adminFullName);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminPanel.fxml"));
            Parent root = loader.load();

            // Set admin name in AdminPanelController
            AdminPanelController controller = loader.getController();
            controller.setAdminName(adminFullName);

            Stage stage = (Stage) anchorPaneadmin.getScene().getWindow();

            // Apply fade out transition and switch scene
            applyFadeTransition(anchorPaneadmin, 1.0, 0.0, 350).setOnFinished(e -> {
                stage.setScene(new Scene(root));
                stage.centerOnScreen();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Validate admin credentials against the database
    private boolean isValidAdmin(String adminid, String adminpass) {
        String url = "jdbc:sqlite:quiz.db";
        String query = "SELECT * FROM admin WHERE admin_id = ? AND password = ?";

        try (
                Connection conn = DriverManager.getConnection(url);
                PreparedStatement stmt = conn.prepareStatement(query)
        ) {
            stmt.setString(1, adminid);
            stmt.setString(2, adminpass);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String fname = rs.getString("fname");
                    String lname = rs.getString("lname");
                    adminFullName = fname + " " + lname;
                    return true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Navigate to user login screen
    @FXML
    public void callUserLogin(ActionEvent event) throws IOException {
        Parent node = FXMLLoader.load(getClass().getResource("User.fxml"));
        Stage stage = (Stage) anchorPaneadmin.getScene().getWindow();

        // Apply fade out before switching scene
        applyFadeTransition(anchorPaneadmin, 1.0, 0.0, 350).setOnFinished(e -> {
            stage.setScene(new Scene(node));
            stage.centerOnScreen();
        });
    }

    // Helper method to apply fade transition to any pane
    private FadeTransition applyFadeTransition(AnchorPane pane, double from, double to, int millis) {
        FadeTransition ft = new FadeTransition(Duration.millis(millis), pane);
        ft.setFromValue(from);
        ft.setToValue(to);
        ft.play();
        return ft;
    }
}