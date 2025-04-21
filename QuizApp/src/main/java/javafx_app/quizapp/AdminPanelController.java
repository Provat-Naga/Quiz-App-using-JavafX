package javafx_app.quizapp;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AdminPanelController implements Initializable {

    // FXML UI Components
    @FXML
    private Label adminNameLabel;

    @FXML
    private BorderPane adminPanelRoot;

    @FXML
    private StackPane contentPane;

    @FXML
    private Button btnAddQuiz;

    @FXML
    private Button btnEditDeleteQuiz;

    @FXML
    private Button logoutButton;

    @FXML
    private Label dateTimeLabel;

    @FXML
    private Button editQuizButton;

    // Other instance variables
    private String adminName;
    private Label savedGreetingLabel;

    // Initialization logic
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        savedGreetingLabel = adminNameLabel;

        // Set button event handlers
        btnAddQuiz.setOnAction(e -> loadQuizView());
        btnEditDeleteQuiz.setOnAction(e -> loadViewWithFade("EditQuiz.fxml"));

        // Clock setup
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm:ss a");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();
            dateTimeLabel.setText(formatter.format(now));
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        dateTimeLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 16px; -fx-font-weight: bold;");
    }

    // Handles logout action
    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Stage stage = (Stage) adminPanelRoot.getScene().getWindow();
            StartApp.handleClose(stage, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Loads the Add Quiz view
    private void loadQuizView() {
        try {
            Stage stage = (Stage) adminPanelRoot.getScene().getWindow();

            // Prevent reloading if already in Add Quiz view
            if (!contentPane.getChildren().isEmpty() && contentPane.getChildren().get(0).getId() != null
                    && contentPane.getChildren().get(0).getId().equals("quizView")) {
                Notification.showWarning("You are already in Add Quiz mode!", stage);
                return;
            }

            if (savedGreetingLabel != null) {
                contentPane.getChildren().remove(savedGreetingLabel);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddQuiz.fxml"));
            Parent view = loader.load();
            view.setId("quizView");

            AddQuizController quizController = loader.getController();
            quizController.setAdminPanelController(this, adminName);

            btnEditDeleteQuiz.setDisable(true);
            Notification.showInfo("First add title to enable other fields", stage);

            // Apply fade animation
            view.setTranslateY(-20);
            FadeTransition fade = new FadeTransition(Duration.millis(350), view);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();

            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Loads a generic view with fade effect
    private void loadViewWithFade(String fxmlFile) {
        try {
            Stage stage = (Stage) adminPanelRoot.getScene().getWindow();

            // Prevent reloading if already in Edit/Delete Quiz view
            if (!contentPane.getChildren().isEmpty() && contentPane.getChildren().get(0).getId() != null
                    && contentPane.getChildren().get(0).getId().equals("editQuizView")
                    && fxmlFile.equals("EditQuiz.fxml")) {
                Notification.showWarning("You are already in Edit/Delete Quiz mode!", stage);
                return;
            }

            if (savedGreetingLabel != null) {
                contentPane.getChildren().remove(savedGreetingLabel);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_app/quizapp/" + fxmlFile));
            Parent view = loader.load();

            if (fxmlFile.equals("EditQuiz.fxml")) {
                view.setId("editQuizView");
                EditQuizController editController = loader.getController();
                editController.setAdminPanelController(this);
                btnAddQuiz.setDisable(true);
            }

            // Apply fade animation
            view.setTranslateY(-40);
            FadeTransition fade = new FadeTransition(Duration.millis(350), view);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();

            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Returns the admin's name
    public String getAdminName() {
        return adminName;
    }

    // Sets the admin's name and updates label
    public void setAdminName(String name) {
        this.adminName = name;
        if (adminNameLabel != null) {
            adminNameLabel.setText("Welcome, " + name + " !");
        }
    }

    // Resets the view to default admin panel state
    public void returnToAdminPanel() {
        btnEditDeleteQuiz.setDisable(false);
        btnAddQuiz.setDisable(false);
        contentPane.getChildren().clear();

        if (adminName != null && savedGreetingLabel != null) {
            savedGreetingLabel.setText("Welcome, " + adminName + " !");
            contentPane.getChildren().add(savedGreetingLabel);
        }
    }
}