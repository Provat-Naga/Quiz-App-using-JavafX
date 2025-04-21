package javafx_app.quizapp;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class LeaderboardController implements Initializable {

    // Mapping quiz titles to their IDs
    private final Map<String, Integer> quizTitleToId = new HashMap<>();

    // FXML UI components
    @FXML
    private ComboBox<String> quizSelector;

    @FXML
    private TableView<RowData> leaderboardTable;

    @FXML
    private TableColumn<RowData, String> rankColumn;

    @FXML
    private TableColumn<RowData, String> nameColumn;

    @FXML
    private TableColumn<RowData, String> scoreColumn;

    @FXML
    private Button backButton;

    // Current user info
    private String userId;
    private String fullName;

    // Called automatically after FXML is loaded
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up table column value factories
        rankColumn.setCellValueFactory(data -> data.getValue().rankProperty());
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());
        scoreColumn.setCellValueFactory(data -> data.getValue().scoreProperty());

        // Load quiz titles into the ComboBox
        loadQuizzes();

        // Event: When a quiz is selected from the dropdown
        quizSelector.setOnAction(e -> {
            String selected = quizSelector.getValue();
            if (selected.equals("Global Leaderboard")) {
                loadGlobalLeaderboard();
            } else {
                Integer quizId = quizTitleToId.get(selected);
                if (quizId != null) {
                    loadQuizLeaderboard(quizId);
                }
            }
        });

        // Default selection: Global Leaderboard
        quizSelector.getSelectionModel().select("Global Leaderboard");
        loadGlobalLeaderboard();
    }

    // Load quiz titles from the database into the ComboBox
    private void loadQuizzes() {
        quizSelector.getItems().add("Global Leaderboard");

        String query = "SELECT id, title FROM quiz";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                quizTitleToId.put(title, id);
                quizSelector.getItems().add(title);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load leaderboard for a specific quiz
    private void loadQuizLeaderboard(int quizId) {
        ObservableList<RowData> data = FXCollections.observableArrayList();

        String query = """
                    SELECT u.first_name || ' ' || u.last_name AS full_name,
                           SUM(CASE WHEN ur.is_correct = 'true' THEN 1 ELSE 0 END) AS correct,
                           COUNT(*) AS total
                    FROM user_response ur
                    JOIN user u ON u.user_id = ur.user_id
                    WHERE ur.quiz_id = ?
                    GROUP BY ur.user_id
                    ORDER BY correct * 100.0 / total DESC
                    LIMIT 10
                """;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db");
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, quizId);
            ResultSet rs = stmt.executeQuery();
            int rank = 1;

            while (rs.next()) {
                String name = rs.getString("full_name");
                int correct = rs.getInt("correct");
                int total = rs.getInt("total");
                int percent = total > 0 ? (correct * 100 / total) : 0;
                data.add(new RowData(String.valueOf(rank++), name, percent + "%"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        leaderboardTable.setItems(data);
    }

    // Load global leaderboard across all quizzes
    private void loadGlobalLeaderboard() {
        ObservableList<RowData> data = FXCollections.observableArrayList();

        String query = """
                    SELECT u.first_name || ' ' || u.last_name AS full_name,
                           SUM(CASE WHEN ur.is_correct = 'true' THEN 1 ELSE 0 END) AS total_correct,
                           COUNT(*) AS total_questions
                    FROM user_response ur
                    JOIN user u ON u.user_id = ur.user_id
                    GROUP BY ur.user_id
                    ORDER BY total_correct * 100.0 / total_questions DESC
                    LIMIT 10
                """;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:Quiz.db");
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            int rank = 1;
            while (rs.next()) {
                String name = rs.getString("full_name");
                int correct = rs.getInt("total_correct");
                int total = rs.getInt("total_questions");
                int percent = total > 0 ? (correct * 100 / total) : 0;
                data.add(new RowData(String.valueOf(rank++), name, percent + "%"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        leaderboardTable.setItems(data);
    }

    // Sets user info passed from previous screen
    public void setUserDetails(String userId, String fullName) {
        this.userId = userId;
        this.fullName = fullName;
        System.out.println("User for leaderboard: " + userId + " " + fullName);
    }

    // Handle 'Back' button to return to the user panel
    @FXML
    private void handleBackToUserPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UserPanel.fxml"));
            Parent root = loader.load();

            UserPanelController controller = loader.getController();
            controller.setUserGreeting(fullName, userId);

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inner class for holding leaderboard table row data
    public static class RowData {
        private final StringProperty rank;
        private final StringProperty name;
        private final StringProperty score;

        public RowData(String rank, String name, String score) {
            this.rank = new SimpleStringProperty(rank);
            this.name = new SimpleStringProperty(name);
            this.score = new SimpleStringProperty(score);
        }

        public StringProperty rankProperty() {
            return rank;
        }

        public StringProperty nameProperty() {
            return name;
        }

        public StringProperty scoreProperty() {
            return score;
        }
    }
}