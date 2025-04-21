package javafx_app.quizapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EditQuizController implements Initializable {

    private final List<Question> questions = new ArrayList<>();
    private final ToggleGroup group = new ToggleGroup();
    // UI components
    @FXML
    private ComboBox<String> quizComboBox;
    @FXML
    private Button loadButton, saveButton, deleteButton, previousButton, nextButton, doneButton, deleteQuizButton;
    @FXML
    private TextField quizTitleField, option1Field, option2Field, option3Field, option4Field;
    @FXML
    private RadioButton option1Radio, option2Radio, option3Radio, option4Radio;
    @FXML
    private ToggleGroup answerToggleGroup;
    @FXML
    private Label questionCounterLabel;
    @FXML
    private TextArea questionField;
    @FXML
    private StackPane stackPane;
    // DB and quiz state
    private Connection conn;
    private int quizId = -1;
    private int currentIndex = 0;
    private AdminPanelController adminPanelController;

    // Initialize controller
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connectDB();
        loadQuizTitles();

        // Assign toggle group for radio buttons
        option1Radio.setToggleGroup(group);
        option2Radio.setToggleGroup(group);
        option3Radio.setToggleGroup(group);
        option4Radio.setToggleGroup(group);

        // Set up button actions
        quizComboBox.setOnAction(e -> loadQuiz());
        saveButton.setOnAction(e -> saveQuestion());
        deleteButton.setOnAction(e -> deleteCurrentQuestion());
        previousButton.setOnAction(e -> showPreviousQuestion());
        nextButton.setOnAction(e -> showNextQuestion());
        doneButton.setOnAction(e -> returnToAdminPanel());
        deleteQuizButton.setOnAction(e -> deleteQuiz());
    }

    // Establish connection to SQLite database
    private void connectDB() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:quiz.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load quiz titles into ComboBox
    private void loadQuizTitles() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT title FROM quiz");
            while (rs.next()) {
                quizComboBox.getItems().add(rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load selected quiz and its questions
    private void loadQuiz() {
        String selectedTitle = quizComboBox.getValue();
        if (selectedTitle == null) return;

        questions.clear();
        currentIndex = 0;

        try {
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM quiz WHERE title = ?");
            ps.setString(1, selectedTitle);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                quizId = rs.getInt("id");
                quizTitleField.setText(selectedTitle);

                ps = conn.prepareStatement("SELECT * FROM question WHERE quiz_id = ?");
                ps.setInt(1, quizId);
                rs = ps.executeQuery();

                while (rs.next()) {
                    questions.add(new Question(
                            rs.getInt("id"),
                            rs.getString("question"),
                            rs.getString("op1"),
                            rs.getString("op2"),
                            rs.getString("op3"),
                            rs.getString("op4"),
                            rs.getString("ans")
                    ));
                }

                if (!questions.isEmpty()) {
                    displayQuestion(0);
                } else {
                    clearFields();
                    questionCounterLabel.setText("No questions found.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Display question at specified index
    private void displayQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;

        Question q = questions.get(index);
        questionField.setText(q.question);
        option1Field.setText(q.op1);
        option2Field.setText(q.op2);
        option3Field.setText(q.op3);
        option4Field.setText(q.op4);

        if (q.ans.equals(q.op1)) {
            group.selectToggle(option1Radio);
        } else if (q.ans.equals(q.op2)) {
            group.selectToggle(option2Radio);
        } else if (q.ans.equals(q.op3)) {
            group.selectToggle(option3Radio);
        } else if (q.ans.equals(q.op4)) {
            group.selectToggle(option4Radio);
        } else {
            group.selectToggle(null);
        }

        questionCounterLabel.setText("Question " + (index + 1) + " of " + questions.size());
    }

    // Save changes to current question
    private void saveQuestion() {
        if (questions.isEmpty()) {
            Notification.showInfo("Nothing to save !!!", (Stage) stackPane.getScene().getWindow());
            return;
        }

        Question current = questions.get(currentIndex);

        String newQuestion = questionField.getText();
        String newOp1 = option1Field.getText();
        String newOp2 = option2Field.getText();
        String newOp3 = option3Field.getText();
        String newOp4 = option4Field.getText();

        String selectedAnswer = "";
        if (group.getSelectedToggle() == option1Radio) selectedAnswer = newOp1;
        else if (group.getSelectedToggle() == option2Radio) selectedAnswer = newOp2;
        else if (group.getSelectedToggle() == option3Radio) selectedAnswer = newOp3;
        else if (group.getSelectedToggle() == option4Radio) selectedAnswer = newOp4;

        boolean unchanged = newQuestion.equals(current.question)
                && newOp1.equals(current.op1)
                && newOp2.equals(current.op2)
                && newOp3.equals(current.op3)
                && newOp4.equals(current.op4)
                && selectedAnswer.equals(current.ans);

        if (unchanged) {
            Notification.showInfo("Nothing to change !!!", (Stage) stackPane.getScene().getWindow());
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE question SET question=?, op1=?, op2=?, op3=?, op4=?, ans=? WHERE id=?");
            ps.setString(1, newQuestion);
            ps.setString(2, newOp1);
            ps.setString(3, newOp2);
            ps.setString(4, newOp3);
            ps.setString(5, newOp4);
            ps.setString(6, selectedAnswer);
            ps.setInt(7, current.id);
            ps.executeUpdate();

            questions.set(currentIndex, new Question(current.id, newQuestion, newOp1, newOp2, newOp3, newOp4, selectedAnswer));
            Notification.showSuccess("Changes saved !!!", (Stage) stackPane.getScene().getWindow());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Delete current question
    private void deleteCurrentQuestion() {
        if (questions.isEmpty()) {
            Notification.showInfo("No questions to delete !!!", (Stage) stackPane.getScene().getWindow());
            return;
        }

        Question current = questions.get(currentIndex);

        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM question WHERE id=?");
            ps.setInt(1, current.id);
            ps.executeUpdate();

            questions.remove(currentIndex);
            if (currentIndex >= questions.size()) currentIndex--;

            if (!questions.isEmpty()) {
                displayQuestion(currentIndex);
                Notification.showSuccess("Question Deleted !!!", (Stage) stackPane.getScene().getWindow());
            } else {
                clearFields();
                questionCounterLabel.setText("No questions remaining.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Navigate to previous question
    private void showPreviousQuestion() {
        if (currentIndex > 0) {
            currentIndex--;
            displayQuestion(currentIndex);
        } else {
            Notification.showInfo("No more previous questions to load !!!", (Stage) stackPane.getScene().getWindow());
        }
    }

    // Navigate to next question
    private void showNextQuestion() {
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            displayQuestion(currentIndex);
        } else {
            Notification.showInfo("No more next questions to load !!!", (Stage) stackPane.getScene().getWindow());
        }
    }

    // Delete entire quiz and its questions
    private void deleteQuiz() {
        if (quizId == -1) {
            Notification.showInfo("No quiz to load !!!", (Stage) stackPane.getScene().getWindow());
            return;
        }

        Stage stage = (Stage) stackPane.getScene().getWindow();
        boolean confirmed = StartApp.showConfirmation(stage, "Do you want to delete the quiz and all its questions?");
        if (!confirmed) return;

        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM question WHERE quiz_id=?");
            ps.setInt(1, quizId);
            ps.executeUpdate();

            ps = conn.prepareStatement("DELETE FROM quiz WHERE id=?");
            ps.setInt(1, quizId);
            ps.executeUpdate();

            Notification.showSuccess("Quiz deleted along with the questions !!!", stage);
            quizComboBox.getItems().remove(quizComboBox.getValue());

            clearFields();
            questionCounterLabel.setText("");
            questions.clear();
            quizId = -1;
            currentIndex = 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Return to admin panel scene
    @FXML
    private void returnToAdminPanel() {
        Stage stage = (Stage) doneButton.getScene().getWindow();
        boolean confirmed = StartApp.showConfirmation(stage, "Do you want to return to the admin panel?");
        if (!confirmed) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("adminpanel.fxml"));
            Parent root = loader.load();

            AdminPanelController controller = loader.getController();
            if (adminPanelController != null) {
                controller.setAdminName(adminPanelController.getAdminName());
            }

            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clear all UI fields
    private void clearFields() {
        quizTitleField.clear();
        questionField.clear();
        option1Field.clear();
        option2Field.clear();
        option3Field.clear();
        option4Field.clear();
        group.selectToggle(null);
    }

    // Pass reference to AdminPanelController
    public void setAdminPanelController(AdminPanelController controller) {
        this.adminPanelController = controller;
    }

    // Question data structure
    private static class Question {
        int id;
        String question, op1, op2, op3, op4, ans;

        public Question(int id, String question, String op1, String op2, String op3, String op4, String ans) {
            this.id = id;
            this.question = question;
            this.op1 = op1;
            this.op2 = op2;
            this.op3 = op3;
            this.op4 = op4;
            this.ans = ans;
        }
    }
}