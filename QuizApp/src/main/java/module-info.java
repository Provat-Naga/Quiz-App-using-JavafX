module javafx_app.quizapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires java.sql;

    opens javafx_app.quizapp to javafx.fxml;
    exports javafx_app.quizapp;
}