package javafx_app.quizapp;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

public class Notification {
    // Public Notification Methods

    public static void showSuccess(String message, Stage stage) {
        createNotification("✔ Success", message, stage, "#1B4D3E").show();
    }

    public static void showError(String message, Stage stage) {
        createNotification("❌ Error", message, stage, "#DC143C").show();
    }

    public static void showWarning(String message, Stage stage) {
        createNotification("⚠ Warning", message, stage, "#6e1423").show();
    }

    public static void showInfo(String message, Stage stage) {
        createNotification("ℹ Info", message, stage, "#00416A").show();
    }

    // Private Notification Builder

    private static Notifications createNotification(String title, String message, Stage stage, String bgColor) {
        StackPane graphic = new StackPane();

        Label label = new Label(message);
        label.setWrapText(true);
        label.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;"
        );

        graphic.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 10px;" +
                        "-fx-padding: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);"
        );
        graphic.getChildren().add(label);
        graphic.setOpacity(0); // Start transparent

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), graphic);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Fade out after delay
        fadeIn.setOnFinished(event -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), graphic);
            fadeOut.setDelay(Duration.seconds(2.7));
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.play();
        });

        return Notifications.create()
                .title(title)
                .graphic(graphic)
                .hideAfter(Duration.seconds(3))
                .position(Pos.BOTTOM_RIGHT)
                .owner(stage);
    }
}