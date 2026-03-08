package com.odilias.odms;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Quick DB Migration
        try (java.sql.Connection conn = com.odilias.odms.util.DatabaseConnection.getConnection();
                java.sql.Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("ALTER TABLE patients ADD COLUMN registered_by_name VARCHAR(100) DEFAULT 'System'");
            } catch (Exception e) {
            }
            try {
                stmt.execute("ALTER TABLE patients ADD COLUMN updated_by_name VARCHAR(100)");
            } catch (Exception e) {
            }
            try {
                stmt.execute("ALTER TABLE patients ADD COLUMN update_notes TEXT");
            } catch (Exception e) {
            }
            try {
                stmt.execute("ALTER TABLE prescriptions ADD COLUMN dispensed_at TIMESTAMP");
            } catch (Exception e) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("view/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Odilias Dispensary Management System - Login");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            event.consume();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Exit Application");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to exit Odilias Dispensary Management System?");
            java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                javafx.application.Platform.exit();
                System.exit(0);
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
