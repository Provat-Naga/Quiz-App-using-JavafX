package javafx_app.quizapp;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AddQuizController implements Initializable {

    // Declarations
    private final List<Question> tempQuestions = new ArrayList<>(); // Temporary list to store questions before saving
    private ToggleGroup toggleGroup;
    private String quizTitle;
    private int quizId;
    private boolean quizExists = false;

    private AdminPanelController adminPanelController;
    private String adminName;

    // FXML UI Components
    @FXML
    private Label questionCountLabel;
    @FXML
    private AnchorPane quizAnchor;
    @FXML
    private TextField titleField, option1Field, option2Field, option3Field, option4Field;
    @FXML
    private TextArea questionField;
    @FXML
    private RadioButton option1Radio, option2Radio, option3Radio, option4Radio;
    @FXML
    private Button okButton, addAnotherButton, saveButton, cancelButton;

    // Initialize method to set up the UI
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize the toggle group for the radio buttons
        toggleGroup = new ToggleGroup();
        option1Radio.setToggleGroup(toggleGroup);
        option2Radio.setToggleGroup(toggleGroup);
        option3Radio.setToggleGroup(toggleGroup);
        option4Radio.setToggleGroup(toggleGroup);

        setInitialState(); // Set initial state of the UI
    }

    // Method to receive admin panel reference and admin name
    public void setAdminPanelController(AdminPanelController controller, String adminName) {
        this.adminPanelController = controller;
        this.adminName = adminName;
    }

    // Reset the UI to its default state
    private void setInitialState() {
        enableTitleEntry(true); // Enable title entry
        enableQuizEntryFields(false); // Disable quiz entry fields
        clearAllFields(); // Clear all fields
        updateQuestionCount(); // Update question count
        quizTitle = null; // Reset quiz title
        quizId = -1; // Reset quiz ID
        quizExists = false; // Reset quiz existence flag
    }

    // Enable or disable the title entry section
    private void enableTitleEntry(boolean enable) {
        titleField.setDisable(!enable);
        okButton.setDisable(!enable);
    }

    // Enable or disable the quiz question entry fields
    private void enableQuizEntryFields(boolean enable) {
        questionField.setDisable(!enable);
        option1Field.setDisable(!enable);
        option2Field.setDisable(!enable);
        option3Field.setDisable(!enable);
        option4Field.setDisable(!enable);
        option1Radio.setDisable(!enable);
        option2Radio.setDisable(!enable);
        option3Radio.setDisable(!enable);
        option4Radio.setDisable(!enable);
        addAnotherButton.setDisable(!enable);
        saveButton.setDisable(!enable);
    }

    // Clear all input fields including the title field
    private void clearAllFields() {
        titleField.clear();
        clearFields();
        toggleGroup.selectToggle(null);
    }

    // Clear only the question and options fields
    private void clearFields() {
        questionField.clear();
        option1Field.clear();
        option2Field.clear();
        option3Field.clear();
        option4Field.clear();
        toggleGroup.selectToggle(null);
    }

    // Update the question count label with the number of questions added
    private void updateQuestionCount() {
        questionCountLabel.setText("Questions Added: " + tempQuestions.size());
    }

    // Handle the OK button action to check quiz title and verify if it exists
    @FXML
    private void handleOK() {
        quizTitle = titleField.getText().trim();

        // Check if quiz title is empty
        if (quizTitle.isEmpty()) {
            Notification.showWarning("Quiz title cannot be blank!", (Stage) quizAnchor.getScene().getWindow());
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db")) {
            // Check if the quiz already exists in the database
            PreparedStatement checkStmt = conn.prepareStatement("SELECT id, title FROM quiz WHERE title = ? COLLATE NOCASE");
            checkStmt.setString(1, quizTitle);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Existing quiz found, update quizId and display message
                quizExists = true;
                quizId = rs.getInt("id");
                quizTitle = rs.getString("title");

                PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) AS total FROM question WHERE quiz_id = ?");
                countStmt.setInt(1, quizId);
                ResultSet countRs = countStmt.executeQuery();
                int existingQuestions = countRs.next() ? countRs.getInt("total") : 0;

                Notification.showInfo("Quiz already exists with " + existingQuestions + " questions.\nNew questions will be added after that.", (Stage) quizAnchor.getScene().getWindow());
            } else {
                // New quiz will be created
                quizExists = false;
                quizId = -1;
                Notification.showInfo("New quiz will be created on Save.", (Stage) quizAnchor.getScene().getWindow());
            }

            enableTitleEntry(false); // Disable title entry
            enableQuizEntryFields(true); // Enable question entry fields

        } catch (SQLException e) {
            Notification.showError("Database Error", (Stage) quizAnchor.getScene().getWindow());
            e.printStackTrace();
        }
    }

    // Handle the Add Another button action to validate and store the current question
    @FXML
    private void handleAddAnother() {
        if (!validateQuestionEntry()) return;

        addCurrentQuestionToList(); // Add the current question to the temporary list
        clearFields(); // Clear question and options fields
        updateQuestionCount(); // Update question count
    }

    // Handle the Save button action to store quiz and all questions to the database
    @FXML
    private void handleSave() {
        // Check if there is any current input
        boolean hasCurrentInput = !questionField.getText().trim().isEmpty()
                || !option1Field.getText().trim().isEmpty()
                || !option2Field.getText().trim().isEmpty()
                || !option3Field.getText().trim().isEmpty()
                || !option4Field.getText().trim().isEmpty()
                || toggleGroup.getSelectedToggle() != null;

        if (hasCurrentInput) {
            // Validate the current question
            if (!validateQuestionEntry()) return;
            addCurrentQuestionToList(); // Add current question to temporary list
        }

        if (tempQuestions.isEmpty()) {
            Notification.showWarning("No questions added to save!", (Stage) quizAnchor.getScene().getWindow());
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db")) {

            // Insert new quiz if it doesn't exist
            if (!quizExists) {
                PreparedStatement insertQuiz = conn.prepareStatement("INSERT INTO quiz(title) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
                insertQuiz.setString(1, quizTitle);
                insertQuiz.executeUpdate();

                ResultSet keys = insertQuiz.getGeneratedKeys();
                if (keys.next()) {
                    quizId = keys.getInt(1);
                }
            }

            // Insert all questions into the database
            String sql = "INSERT INTO question (question, op1, op2, op3, op4, ans, quiz_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            for (Question q : tempQuestions) {
                stmt.setString(1, q.question);
                stmt.setString(2, q.op1);
                stmt.setString(3, q.op2);
                stmt.setString(4, q.op3);
                stmt.setString(5, q.op4);
                stmt.setString(6, q.ans);
                stmt.setInt(7, quizId);
                stmt.addBatch();
            }

            stmt.executeBatch();
            tempQuestions.clear(); // Clear temporary list after saving

            Notification.showSuccess("Quiz saved successfully!", (Stage) quizAnchor.getScene().getWindow());
            setInitialState(); // Reset the UI

        } catch (SQLException e) {
            e.printStackTrace();
            Notification.showError("Error saving quiz to database", (Stage) quizAnchor.getScene().getWindow());
        }
    }

    // Handle the Cancel button action to cancel current quiz entry and return to admin panel
    @FXML
    private void handleCancelQuizEntry() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        boolean confirm = StartApp.showConfirmation(stage, "Do you want to cancel the entry?");
        if (confirm) {
            tempQuestions.clear();
            setInitialState();
            adminPanelController.setAdminName(adminName);
            adminPanelController.returnToAdminPanel();
            Notification.showWarning("Quiz entry cancelled!", stage);
        }
    }

    // Validate that all quiz question fields are properly filled
    private boolean validateQuestionEntry() {
        if (questionField.getText().trim().isEmpty()
                || option1Field.getText().trim().isEmpty()
                || option2Field.getText().trim().isEmpty()
                || option3Field.getText().trim().isEmpty()
                || option4Field.getText().trim().isEmpty()
                || toggleGroup.getSelectedToggle() == null) {

            Notification.showWarning("Please complete all fields and select a correct answer!", (Stage) quizAnchor.getScene().getWindow());
            return false;
        }
        return true;
    }

    // Add the current question to the temporary list
    private void addCurrentQuestionToList() {
        String question = questionField.getText().trim();
        String op1 = option1Field.getText().trim();
        String op2 = option2Field.getText().trim();
        String op3 = option3Field.getText().trim();
        String op4 = option4Field.getText().trim();

        RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
        String correctAnswer = switch (selected.getId()) {
            case "option1Radio" -> op1;
            case "option2Radio" -> op2;
            case "option3Radio" -> op3;
            case "option4Radio" -> op4;
            default -> "";
        };

        // Check if question already exists in database for this quiz
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db")) {
            String sql = "SELECT COUNT(*) AS count FROM question WHERE quiz_id = ? AND question = ? COLLATE NOCASE";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, quizId);
            stmt.setString(2, question);
            ResultSet rs = stmt.executeQuery();

            if (rs.next() && rs.getInt("count") > 0) {
                Notification.showWarning("Question already exists!", (Stage) quizAnchor.getScene().getWindow());
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Notification.showError("Error checking question in database", (Stage) quizAnchor.getScene().getWindow());
            return;
        }

        tempQuestions.add(new Question(question, op1, op2, op3, op4, correctAnswer));
    }


    // Inner class to store question data
    static class Question {
        String question, op1, op2, op3, op4, ans;

        public Question(String question, String op1, String op2, String op3, String op4, String ans) {
            this.question = question;
            this.op1 = op1;
            this.op2 = op2;
            this.op3 = op3;
            this.op4 = op4;
            this.ans = ans;
        }
    }
}