package javafx_app.quizapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;

public class StartApp extends Application {

    // Main method: application entry point
    public static void main(String[] args) {
        launch(args);
    }

    // Handles application close or logout
    public static void handleClose(Stage stage, boolean goToLogin) {
        StartApp app = new StartApp();

        // Determine the confirmation message based on the scene root ID
        Parent root = stage.getScene().getRoot();
        String message = root.getId() != null && root.getId().equals("adminPanelRoot")
                ? "Do you want to logout and close the Admin Panel?"
                : "Do you want to close the application?";

        // Show confirmation dialog
        boolean confirm = app.showCloseConfirmation(stage, message);
        if (confirm) {
            // Attempt to close the database connection
            try {
                Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db");
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    System.out.println("Database connection closed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // If goToLogin is true, load the login screen; otherwise close the stage
            if (goToLogin) {
                try {
                    Parent loginRoot = FXMLLoader.load(StartApp.class.getResource("Admin.fxml"));
                    Scene scene = new Scene(loginRoot);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                stage.close();
            }
        }
    }

    // Shows a confirmation dialog with static context
    public static boolean showConfirmation(Stage stage, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(message);

        // Set application icon for the dialog
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(StartApp.class.getResourceAsStream("/img/appicon.png")));

        // Define Yes and No buttons
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);

        // Apply custom CSS styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(StartApp.class.getResource("css/alert.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        // Return true if user clicked Yes
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;
    }

    // Initializes and shows the application window
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("User.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("Quiz Application");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/appicon.png")));
        stage.setResizable(false);
        stage.show();

        // Intercept the window close request to show confirmation
        stage.setOnCloseRequest(event -> {
            event.consume(); // Cancel default close behavior
            handleClose(stage, false); // Exit app
        });
    }

    // Shows a close confirmation dialog with custom message
    public boolean showCloseConfirmation(Stage stage, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(message);

        // Set application icon for the dialog
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image(getClass().getResourceAsStream("/img/appicon.png")));

        // Define Yes and No buttons
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);

        // Apply custom CSS styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("css/alert.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        // Return true if user clicked Yes
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;
    }
}