package javafx_app.quizapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserPanelController {

    // Database URL for SQLite
    private final String DB_URL = "jdbc:sqlite:Quiz.db";

    // FXML fields
    @FXML
    private FlowPane cardContainer;
    @FXML
    private Label greetingLabel;

    // Instance variables to store user information
    private String userId;
    private String name;

    // Set user's name and ID after login
    public void setUserGreeting(String name, String uid) {
        this.userId = uid;  // Store user ID
        this.name = name;
        greetingLabel.setText("Welcome, " + name + "!");
    }

    // Handle the action of navigating to leaderboard screen
    @FXML
    private void handleLeaderboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Leaderboard.fxml"));
            Parent root = loader.load();

            // Pass user data to LeaderboardController
            LeaderboardController controller = loader.getController();
            controller.setUserDetails(userId, name);

            Stage stage = (Stage) cardContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Initialize method for setting up the quiz cards in the UI
    @FXML
    public void initialize() {
        List<QuizInfo> quizzes = getQuizzesFromDatabase(); // Get quizzes from DB
        for (QuizInfo quiz : quizzes) {
            VBox card = createQuizCard(quiz);
            cardContainer.getChildren().add(card);
        }
    }

    // Create a card for each quiz with title, question count, and start button
    private VBox createQuizCard(QuizInfo quiz) {
        VBox card = new VBox();
        card.getStyleClass().add("quiz-card");
        card.setSpacing(10);
        card.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(quiz.title);
        titleLabel.getStyleClass().add("quiz-title");

        Label questionCountLabel = new Label("Questions: " + quiz.questionCount);
        questionCountLabel.getStyleClass().add("quiz-count");

        Button startButton = new Button("Start →");
        startButton.getStyleClass().add("start-btn");
        startButton.setOnAction(e -> startQuiz(quiz.title));

        card.getChildren().addAll(titleLabel, questionCountLabel, startButton);
        return card;
    }

    // Start quiz when start button is clicked
    private void startQuiz(String title) {
        try {
            List<Question> questions = getQuestionsByQuizTitle(title);

            // Get the current stage from cardContainer
            Stage stage = (Stage) cardContainer.getScene().getWindow();

            // Check if there are any questions
            if (questions.isEmpty()) {
                Notification.showInfo("Questions were not set by the admin yet!", stage);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("QuizView.fxml"));
            Parent root = loader.load();

            // Pass questions and user details to QuizViewController
            QuizViewController controller = loader.getController();
            controller.initializeQuiz(questions, title, userId, name);

            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Fetch questions related to a quiz title from the database
    private List<Question> getQuestionsByQuizTitle(String title) {
        List<Question> questions = new ArrayList<>();
        String query = """
                SELECT que.id, que.question, que.op1, que.op2, que.op3, que.op4, que.ans
                FROM question que
                JOIN quiz q ON que.quiz_id = q.id
                WHERE q.title = ?
                """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, title);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String questionText = rs.getString("question");
                String op1 = rs.getString("op1");
                String op2 = rs.getString("op2");
                String op3 = rs.getString("op3");
                String op4 = rs.getString("op4");
                String ans = rs.getString("ans");

                questions.add(new Question(id, questionText, op1, op2, op3, op4, ans));
            }

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }

        return questions;
    }

    // Fetch quiz data from the database
    private List<QuizInfo> getQuizzesFromDatabase() {
        List<QuizInfo> list = new ArrayList<>();

        String query = """
                    SELECT q.title, COUNT(que.id) AS question_count
                    FROM quiz q
                    LEFT JOIN question que ON q.id = que.quiz_id
                    GROUP BY q.id, q.title
                """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String title = rs.getString("title");
                int count = rs.getInt("question_count");
                list.add(new QuizInfo(title, count));
            }

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }

        return list;
    }

    // Handle user logout and navigate to the login screen
    @FXML
    private void handleLogout() {
        Stage stage = (Stage) cardContainer.getScene().getWindow();
        boolean confirm = new StartApp().showCloseConfirmation(stage, "Do you want to logout and close the User Panel?");

        if (confirm) {
            closeDatabaseConnection();  // Close DB connection if necessary

            try {
                Parent root = FXMLLoader.load(getClass().getResource("User.fxml"));
                stage.setScene(new Scene(root));
                stage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Close database connection when logging out
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

    // Handle the action of navigating to progress screen
    @FXML
    private void handleProgress() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MyProgress.fxml"));
            Parent root = loader.load();

            // Pass user info to the controller
            MyProgressController controller = loader.getController();
            controller.setUserDetails(userId, name);

            Stage stage = (Stage) cardContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Inner class to store quiz data
    private static class QuizInfo {
        String title;
        int questionCount;

        QuizInfo(String title, int questionCount) {
            this.title = title;
            this.questionCount = questionCount;
        }
    }

    // Inner class to store question data with getCorrectOptionIndex()
    public static class Question {
        private final int id;
        private final String questionText;
        private final String op1;
        private final String op2;
        private final String op3;
        private final String op4;
        private final String correctAnswer;

        public Question(int id, String questionText, String op1, String op2, String op3, String op4, String correctAnswer) {
            this.id = id;
            this.questionText = questionText;
            this.op1 = op1;
            this.op2 = op2;
            this.op3 = op3;
            this.op4 = op4;
            this.correctAnswer = correctAnswer;
        }

        // Get the option based on the index
        public String getOptionByIndex(int index) {
            return switch (index) {
                case 0 -> getOp1();
                case 1 -> getOp2();
                case 2 -> getOp3();
                case 3 -> getOp4();
                default -> "";
            };
        }

        // Getter methods
        public int getId() {
            return id;
        }

        public String getQuestionText() {
            return questionText;
        }

        public String getOp1() {
            return op1;
        }

        public String getOp2() {
            return op2;
        }

        public String getOp3() {
            return op3;
        }

        public String getOp4() {
            return op4;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        // Get the index of the correct option
        public int getCorrectOptionIndex() {
            if (correctAnswer.equalsIgnoreCase(op1)) return 0;
            if (correctAnswer.equalsIgnoreCase(op2)) return 1;
            if (correctAnswer.equalsIgnoreCase(op3)) return 2;
            if (correctAnswer.equalsIgnoreCase(op4)) return 3;
            return -1; // fallback if not matched
        }
    }
}