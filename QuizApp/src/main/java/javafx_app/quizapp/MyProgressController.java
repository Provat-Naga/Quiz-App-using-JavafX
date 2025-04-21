package javafx_app.quizapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class MyProgressController implements Initializable {

    @FXML
    private TilePane attemptsTilePane;
    @FXML
    private VBox detailsBox;
    @FXML
    private VBox questionDetailBox;
    @FXML
    private Label dashboardLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private Button backButton;
    @FXML
    private Button hideDetailsButton;
    @FXML
    private Label quizTitleLabel;
    @FXML
    private HBox dragHandle;

    private String userId;
    private String fullName;

    // Add at the top of the class, with other fields
    private double dragAnchorY;
    private double startTranslateY;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Dragging functionality
        dragHandle.setOnMousePressed(event -> {
            dragAnchorY = event.getSceneY();
            startTranslateY = detailsBox.getTranslateY();
        });

        dragHandle.setOnMouseDragged(event -> {
            double dragOffset = event.getSceneY() - dragAnchorY;
            double newTranslateY = startTranslateY + dragOffset;

            double minY = 0;     // fully up
            double maxY = 400;   // fully down

            if (newTranslateY < minY) newTranslateY = minY;
            if (newTranslateY > maxY) newTranslateY = maxY;

            detailsBox.setTranslateY(newTranslateY);
        });
    }

    public void setUserDetails(String uid, String fName) {
        this.userId = uid;
        this.fullName = fName;
        System.out.println("User ID received in MyProgress: " + userId + fullName);

        dashboardLabel.setText(fullName + "'s Progress !");
        loadUserAttempts();
    }

    private void loadUserAttempts() {
        attemptsTilePane.getChildren().clear();  // Clear previous attempts

        // Create the message label to show if no attempts are found
        Label noAttemptsLabel = new Label("Attempt Quizzes To View Progress !");
        noAttemptsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #888; -fx-alignment: center;  -fx-pref-width:180px");
        noAttemptsLabel.setVisible(false);  // Initially hidden

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db")) {
            String sql = "SELECT qa.quiz_id, q.title, MAX(qa.attempt_date) AS latest_date " +
                    "FROM quiz_attempt qa JOIN quiz q ON qa.quiz_id = q.id " +
                    "WHERE qa.user_id = ? GROUP BY qa.quiz_id ORDER BY latest_date DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            boolean hasAttempts = false;  // Flag to track if any attempts were found

            while (rs.next()) {
                int quizId = rs.getInt("quiz_id");
                String title = rs.getString("title");
                String latestDate = rs.getString("latest_date");

                String detailSql = "SELECT score, time_taken FROM quiz_attempt WHERE quiz_id = ? AND user_id = ? AND attempt_date = ?";
                PreparedStatement detailStmt = conn.prepareStatement(detailSql);
                detailStmt.setInt(1, quizId);
                detailStmt.setString(2, userId);
                detailStmt.setString(3, latestDate);
                ResultSet detailRs = detailStmt.executeQuery();

                if (detailRs.next()) {
                    int score = detailRs.getInt("score");
                    String timeTaken = detailRs.getString("time_taken");

                    VBox card = createAttemptCard(quizId, title, score, timeTaken, latestDate);
                    attemptsTilePane.getChildren().add(card);
                    hasAttempts = true;  // Set flag to true as an attempt is found
                }
            }

            // If no attempts were found, show the "no attempts" label
            if (!hasAttempts) {
                attemptsTilePane.getChildren().add(noAttemptsLabel);
                noAttemptsLabel.setVisible(true);  // Show the message
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private VBox createAttemptCard(int quizId, String title, int score, String time, String date) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("quiz-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("tilabel");

        Label scoreLabel = new Label("Score: " + score + "%");
        scoreLabel.getStyleClass().add("all-inOne");
        Label timeLabel = new Label("Time: " + time);
        timeLabel.getStyleClass().add("all-inOne");

        String formattedDate = formatDate(date);
        Label dateLabel = new Label("Last Attempt:\n" + formattedDate);  // Make last attempt date multi-line
        dateLabel.getStyleClass().add("all-inOne");

        int attemptsCount = getNumberOfAttemptsForQuiz(quizId);

        Label attemptsLabel = new Label("Attempts: " + attemptsCount);
        attemptsLabel.getStyleClass().add("all-inOne");

        Button detailsButton = new Button("Details →");
        detailsButton.getStyleClass().add("dtail");
        detailsButton.setOnAction(e -> showDetailedAttempt(quizId, title));

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.BOTTOM_RIGHT);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().add(detailsButton);

        card.getChildren().addAll(titleLabel, scoreLabel, timeLabel, dateLabel, attemptsLabel, buttonBox);

        return card;
    }

    private int getNumberOfAttemptsForQuiz(int quizId) {
        int attemptsCount = 0;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db")) {
            String countSql = "SELECT COUNT(*) AS attempts FROM quiz_attempt WHERE quiz_id = ? AND user_id = ?";
            PreparedStatement countStmt = conn.prepareStatement(countSql);
            countStmt.setInt(1, quizId);
            countStmt.setString(2, userId);
            ResultSet countRs = countStmt.executeQuery();

            if (countRs.next()) {
                attemptsCount = countRs.getInt("attempts");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attemptsCount;
    }

    private void showDetailedAttempt(int quizId, String quizTitle) {
        quizTitleLabel.setText(quizTitle);
        questionDetailBox.getChildren().clear();
        detailsBox.setVisible(true);
        detailsBox.setManaged(true);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db")) {
            String sql = "SELECT qa.attempt_date, qa.attempt_number, q.question, q.ans AS correct, ur.user_answer " +
                    "FROM question q " +
                    "JOIN user_response ur ON q.id = ur.question_id " +
                    "JOIN quiz_attempt qa ON qa.quiz_id = ur.quiz_id AND qa.user_id = ur.user_id " +
                    "WHERE ur.quiz_id = ? AND ur.user_id = ? " +
                    "ORDER BY qa.attempt_date DESC, qa.attempt_number DESC";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, quizId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            Map<String, List<VBox>> groupedQuestions = new LinkedHashMap<>();

            while (rs.next()) {
                String attemptDate = rs.getString("attempt_date");
                int attemptNumber = rs.getInt("attempt_number");
                String question = rs.getString("question");
                String correct = rs.getString("correct");
                String userAnswer = rs.getString("user_answer");

                String formattedDate = formatDate(attemptDate);
                String key = formattedDate + " (Attempt #" + attemptNumber + ")";

                VBox questionBox = new VBox(4);
                questionBox.setPadding(new Insets(10));
                questionBox.setStyle("-fx-background-color: #fff; -fx-border-color: #dcdcdc; -fx-border-radius: 10; -fx-background-radius: 10;");

                Label qLabel = new Label("Q: " + question);
                qLabel.setWrapText(true);
                Label uLabel = new Label("Your Answer: " + (userAnswer == null || userAnswer.isBlank() ? "Not Attempted" : userAnswer));
                Label statusLabel;

                if (userAnswer == null || userAnswer.isBlank()) {
                    statusLabel = new Label("→ Not Attempted");
                    statusLabel.setTextFill(Color.GRAY);
                } else if (userAnswer.equalsIgnoreCase(correct)) {
                    statusLabel = new Label("→ Correct");
                    statusLabel.setTextFill(Color.GREEN);
                } else {
                    statusLabel = new Label("→ Wrong");
                    statusLabel.setTextFill(Color.RED);
                }

                questionBox.getChildren().addAll(qLabel, uLabel, statusLabel);
                groupedQuestions.computeIfAbsent(key, k -> new ArrayList<>()).add(questionBox);
            }

            for (Map.Entry<String, List<VBox>> entry : groupedQuestions.entrySet()) {
                String groupTitle = entry.getKey();
                List<VBox> questions = entry.getValue();

                VBox groupBox = new VBox(6);
                groupBox.setPadding(new Insets(6));

                TitledPane togglePane = new TitledPane();
                togglePane.setText("Attempt on " + groupTitle);
                VBox content = new VBox(6);
                content.getChildren().addAll(questions);
                togglePane.setContent(content);
                togglePane.setExpanded(false);

                groupBox.getChildren().add(togglePane);
                questionDetailBox.getChildren().add(groupBox);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String formatDate(String rawDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = inputFormat.parse(rawDate);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yy hh:mm a");
            return outputFormat.format(date);
        } catch (Exception e) {
            return rawDate;
        }
    }

    @FXML
    private void handleHideDetails() {
        detailsBox.setVisible(false);
        detailsBox.setManaged(false);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx_app/quizapp/UserPanel.fxml"));
            Scene scene = new Scene(loader.load());
            UserPanelController controller = loader.getController();
            controller.setUserGreeting(fullName, userId);
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        Stage stage = (Stage) attemptsTilePane.getScene().getWindow();
        boolean confirm = new StartApp().showCloseConfirmation(stage, "Do you want to logout and close the User Panel?");

        if (confirm) {
            closeDatabaseConnection();

            try {
                Parent root = FXMLLoader.load(getClass().getResource("User.fxml"));
                stage.setScene(new Scene(root));
                stage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void closeDatabaseConnection() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db");
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}