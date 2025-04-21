package javafx_app.quizapp;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class QuizViewController implements Initializable {

    // --------------------- DB and Quiz Variables ---------------------
    private final String DB_URL = "jdbc:sqlite:Quiz.db";
    private List<UserPanelController.Question> questions;
    private Integer[] selectedAnswers;
    private List<StackPane> progressCircles;
    private int currentQuestionIndex = 0;
    private long quizStartTime;
    private int timeTakenInSeconds;
    private int totalTimeInSeconds;
    private ToggleGroup toggleGroup;
    private String quizTitle;
    private String userId;
    private String name;
    private Timeline countdownTimer;

    // --------------------- FXML UI Elements ---------------------
    @FXML
    private Label quizTitleLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private Label questionLabel;
    @FXML
    private Label questionNumberLabel;
    @FXML
    private VBox questionContainer;
    @FXML
    private Button nextButton;
    @FXML
    private Button finishButton, quitButton, prevButton;
    @FXML
    private FlowPane progressBox;
    @FXML
    private ImageView gifview;
    @FXML
    private SplitPane splitpaneview;

    // --------------------- Initialization ---------------------
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        toggleGroup = new ToggleGroup();
        try {
            Image gif = new Image(getClass().getResource("/img/clock.gif").toExternalForm());
            gifview.setImage(gif);
            gifview.setFitWidth(38);
            gifview.setFitHeight(38);
            gifview.setPreserveRatio(true);
            gifview.setSmooth(true);

            Platform.runLater(() -> {
                Rectangle clip = new Rectangle(gifview.getFitWidth(), gifview.getFitHeight());
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                gifview.setClip(clip);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Quiz Setup
    public void initializeQuiz(List<UserPanelController.Question> questions, String title, String userId, String name) {
        this.questions = questions;
        this.selectedAnswers = new Integer[questions.size()];
        this.totalTimeInSeconds = questions.size() * 60;
        this.progressCircles = new ArrayList<>();
        this.quizStartTime = System.currentTimeMillis();
        this.quizTitle = title;
        this.userId = userId;
        this.name = name;

        quizTitleLabel.setText(title);
        setupProgressTracker();
        displayCurrentQuestion();
        startCountdownTimer();
    }

    private void setupProgressTracker() {
        progressBox.getChildren().clear();
        for (int i = 0; i < questions.size(); i++) {
            Circle circle = new Circle(15);
            circle.getStyleClass().add("progress-circle-default");

            Text number = new Text(String.valueOf(i + 1));
            number.getStyleClass().add("progress-number");

            StackPane stack = new StackPane(circle, number);
            stack.setPadding(new Insets(5));
            final int index = i;
            stack.setOnMouseClicked(e -> {
                currentQuestionIndex = index;
                displayCurrentQuestion();
            });

            progressCircles.add(stack);
            progressBox.getChildren().add(stack);
        }
    }

    // Question Display and Answer Selection
    private void displayCurrentQuestion() {
        if (currentQuestionIndex < questions.size()) {
            UserPanelController.Question question = questions.get(currentQuestionIndex);
            questionContainer.getChildren().clear();
            questionLabel.setText(question.getQuestionText());
            questionNumberLabel.setText("Q." + (currentQuestionIndex + 1));

            List<String> options = List.of(question.getOp1(), question.getOp2(), question.getOp3(), question.getOp4());

            for (int i = 0; i < options.size(); i++) {
                final int index = i;
                RadioButton rb = new RadioButton(options.get(i));
                rb.setToggleGroup(toggleGroup);
                questionContainer.getChildren().add(rb);

                if (selectedAnswers[currentQuestionIndex] != null) {
                    rb.setDisable(true);
                    if (selectedAnswers[currentQuestionIndex] == i) {
                        rb.setSelected(true);
                    }
                }

                rb.setOnAction(e -> handleAnswerSelected());
            }

            if (selectedAnswers[currentQuestionIndex] == null) {
                StackPane circlePane = progressCircles.get(currentQuestionIndex);
                if (!circlePane.getStyleClass().contains("visited")) {
                    circlePane.getStyleClass().add("visited");
                }
            }

            updateButtonStates();
        }
    }

    @FXML
    private void handleAnswerSelected() {
        RadioButton selectedRadioButton = (RadioButton) toggleGroup.getSelectedToggle();
        if (selectedRadioButton != null) {
            String selectedText = selectedRadioButton.getText();
            UserPanelController.Question question = questions.get(currentQuestionIndex);
            List<String> options = List.of(question.getOp1(), question.getOp2(), question.getOp3(), question.getOp4());
            int selectedIndex = options.indexOf(selectedText);
            if (selectedIndex == -1) return;
            if (selectedAnswers[currentQuestionIndex] != null) return;

            selectedAnswers[currentQuestionIndex] = selectedIndex;
            int correctIndex = question.getCorrectOptionIndex();

            if (selectedIndex == correctIndex) {
                progressCircles.get(currentQuestionIndex).getChildren().get(0).setStyle("-fx-fill: #228b22;");
            } else {
                progressCircles.get(currentQuestionIndex).getChildren().get(0).setStyle("-fx-fill: #c71585;");
            }

            for (javafx.scene.Node node : questionContainer.getChildren()) {
                if (node instanceof RadioButton radioButton) {
                    radioButton.setDisable(true);
                }
            }
        }
    }

    // Navigation Handlers
    @FXML
    private void handleNext() {
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            displayCurrentQuestion();
        }
    }

    @FXML
    private void handlePrevious() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            displayCurrentQuestion();
        }
    }

    //  Timer
    private void startCountdownTimer() {
        AtomicInteger seconds = new AtomicInteger(totalTimeInSeconds);
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int remaining = seconds.getAndDecrement();
            int min = remaining / 60;
            int sec = remaining % 60;
            timerLabel.setText(String.format("Time Left: %02d:%02d", min, sec));
            if (remaining <= 0) autoSubmitQuiz();
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void autoSubmitQuiz() {
        if (countdownTimer != null) countdownTimer.stop();
        finishQuiz();
        Notification.showInfo("Time's Up !!!", (Stage) splitpaneview.getScene().getWindow());
        System.out.println("Time's up! Quiz auto-submitted.");
    }

    private String formatTime(int totalSeconds) {
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private void updateButtonStates() {
        prevButton.setDisable(currentQuestionIndex == 0);
        nextButton.setDisable(currentQuestionIndex == questions.size() - 1);
        finishButton.setVisible(currentQuestionIndex == questions.size() - 1);
    }

    // Quiz Completion
    @FXML
    private void finishQuiz() {
        String attemptId = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        if (countdownTimer != null) countdownTimer.stop();

        long finishTime = System.currentTimeMillis();
        timeTakenInSeconds = (int) ((finishTime - quizStartTime) / 1000);

        for (int i = 0; i < selectedAnswers.length; i++) {
            if (selectedAnswers[i] == null) {
                progressCircles.get(i).getStyleClass().add("visited");
            }
        }

        int correctCount = 0;

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // 1. Get Quiz ID
            String getQuizIdQuery = "SELECT id FROM quiz WHERE title = ?";
            PreparedStatement quizIdStmt = conn.prepareStatement(getQuizIdQuery);
            quizIdStmt.setString(1, quizTitle);
            ResultSet rs = quizIdStmt.executeQuery();
            int quizId = rs.next() ? rs.getInt("id") : -1;
            if (quizId == -1) return;

            // 2. Get Attempt Number
            int attemptNumber = 1;
            String getMaxAttempt = """
                    SELECT MAX(attempt_number) AS max_attempt FROM quiz_attempt
                    WHERE user_id = ? AND quiz_id = ?
                    """;
            PreparedStatement maxAttemptStmt = conn.prepareStatement(getMaxAttempt);
            maxAttemptStmt.setString(1, userId);
            maxAttemptStmt.setInt(2, quizId);
            ResultSet attemptRs = maxAttemptStmt.executeQuery();
            if (attemptRs.next()) {
                attemptNumber = attemptRs.getInt("max_attempt") + 1;
            }

            // 3. Insert Responses
            for (int i = 0; i < questions.size(); i++) {
                UserPanelController.Question q = questions.get(i);
                int userAnswerIndex = selectedAnswers[i] != null ? selectedAnswers[i] : -1;
                String userAnswer = (userAnswerIndex >= 0)
                        ? List.of(q.getOp1(), q.getOp2(), q.getOp3(), q.getOp4()).get(userAnswerIndex)
                        : "";

                String isCorrectValue;
                if (userAnswerIndex == -1) {
                    isCorrectValue = "unattended";
                } else {
                    boolean isCorrect = (userAnswerIndex == q.getCorrectOptionIndex());
                    isCorrectValue = isCorrect ? "true" : "false";
                    if (isCorrect) correctCount++;
                }

                String insertResponse = """
                        INSERT INTO user_response (user_id, quiz_id, question_id, user_answer, is_correct, attempt_id)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;
                PreparedStatement stmt = conn.prepareStatement(insertResponse);
                stmt.setString(1, userId);
                stmt.setInt(2, quizId);
                stmt.setInt(3, q.getId());
                stmt.setString(4, userAnswer);
                stmt.setString(5, isCorrectValue);
                stmt.setString(6, attemptId);
                stmt.executeUpdate();
            }

            // 4. Insert Attempt Record
            int score = correctCount * 100 / questions.size();
            String insertAttempt = """
                    INSERT INTO quiz_attempt (attempt_id, user_id, quiz_id, score, attempt_date, time_taken, attempt_number)
                    VALUES (?, ?, ?, ?, datetime('now','localtime'), ?, ?)
                    """;
            PreparedStatement stmt = conn.prepareStatement(insertAttempt);
            stmt.setString(1, attemptId);
            stmt.setString(2, userId);
            stmt.setInt(3, quizId);
            stmt.setInt(4, score);
            stmt.setString(5, formatTime(timeTakenInSeconds));
            stmt.setInt(6, attemptNumber);
            stmt.executeUpdate();

            // 5. Load Result View
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Result.fxml"));
            Parent root = loader.load();

            ResultController controller = loader.getController();
            int totalQuestions = questions.size();
            int unattempted = 0;
            int wrong = 0;

            for (int i = 0; i < totalQuestions; i++) {
                if (selectedAnswers[i] == null) {
                    unattempted++;
                } else if (selectedAnswers[i] != questions.get(i).getCorrectOptionIndex()) {
                    wrong++;
                }
            }

            int correct = totalQuestions - unattempted - wrong;
            controller.setResultData(name, userId, totalQuestions, correct, wrong, unattempted,
                    formatTime(timeTakenInSeconds), correct * 100 / totalQuestions);

            Stage stage = (Stage) splitpaneview.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.centerOnScreen();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    //Quit Handling
    @FXML
    private void handleQuit(ActionEvent event) {
        if (countdownTimer != null)
            countdownTimer.stop();

        Stage stage = (Stage) quitButton.getScene().getWindow();
        boolean confirm = new StartApp().showCloseConfirmation(stage, "Do you want to quit & return to User Panel?");

        if (confirm) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("UserPanel.fxml"));
                Parent root = loader.load();
                UserPanelController userPanelController = loader.getController();
                userPanelController.setUserGreeting(name, userId);

                Stage currentStage = (Stage) quitButton.getScene().getWindow();
                currentStage.setScene(new javafx.scene.Scene(root));
                currentStage.centerOnScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}