package javafx_app.quizapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ResultController implements Initializable {

    // FXML fields
    @FXML
    private AnchorPane resultPane;

    @FXML
    private VBox resultPaneVBox;

    @FXML
    private ImageView userImage;

    @FXML
    private Text userName;

    @FXML
    private Text score;

    @FXML
    private Text totalQuestions;

    @FXML
    private Text not_attemptedQuestions;

    @FXML
    private Text correctQuestions;

    @FXML
    private Text wrongQuestions;

    @FXML
    private Text time_taken;

    @FXML
    private Button backButton;

    // Data passed from QuizViewController
    private String userId;
    private String name;
    private int totalQ;
    private int correctQ;
    private int wrongQ;
    private int notAttemptedQ;
    private String timeTaken;
    private int scoreValue;

    // Flag to indicate if data has been set
    private boolean dataSet = false;

    // Initialize method to set up UI components
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        resultPaneVBox.setPadding(new Insets(30, 30, 30, 30));  // Padding for result pane

        // Set a default avatar image
        Image avatar = new Image(getClass().getResource("/img/award.gif").toExternalForm());
        userImage.setImage(avatar);

        // Set the action for the back button
        backButton.setOnAction(event -> goToDashboard());

        // If data is set, update the UI with the result information
        if (dataSet) {
            updateUI();
        }
    }

    // Navigate to the dashboard (User Panel)
    private void goToDashboard() {
        try {
            // Load User Panel (Dashboard) scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UserPanel.fxml"));
            BorderPane userPanel = loader.load();

            // Get the controller for UserPanel
            UserPanelController userPanelController = loader.getController();

            // Pass the user's name to the UserPanelController
            userPanelController.setUserGreeting(name, userId);

            // Set up and display the User Panel scene
            Stage stage = (Stage) resultPane.getScene().getWindow();
            Scene scene = new Scene(userPanel);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Set result data passed from QuizViewController
    public void setResultData(String name, String userID, int totalQ, int correctQ, int wrongQ, int notAttemptedQ, String timeTaken, int scoreValue) {
        this.name = name;
        this.userId = userID;
        this.totalQ = totalQ;
        this.correctQ = correctQ;
        this.wrongQ = wrongQ;
        this.notAttemptedQ = notAttemptedQ;
        this.timeTaken = timeTaken;
        this.scoreValue = scoreValue;
        this.dataSet = true;
        updateUI();
        System.out.println("result " + this.userId + this.name);
    }

    // Update UI with the result data
    private void updateUI() {
        userName.setText("Name: " + name);
        score.setText("Score: " + scoreValue + "%");
        totalQuestions.setText("Total Questions: " + totalQ);
        correctQuestions.setText("Correct Answers: " + correctQ);
        wrongQuestions.setText("Wrong Answers: " + wrongQ);
        not_attemptedQuestions.setText("Not Attempted: " + notAttemptedQ);
        time_taken.setText("Time Taken: " + timeTaken);
    }
}