package com.odilias.odms.util;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ToastUtil {

    public enum ToastType {
        SUCCESS("#2ecc71"),
        ERROR("#e74c3c"),
        INFO("#3498db");

        private final String color;

        ToastType(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }
    }

    public static void show(Stage ownerStage, String message, ToastType type) {
        Platform.runLater(() -> {
            Stage toastStage = new Stage();
            toastStage.initOwner(ownerStage);
            toastStage.setResizable(false);
            toastStage.initStyle(StageStyle.TRANSPARENT);

            Label label = new Label(message);
            label.setTextFill(Color.WHITE);
            label.setFont(Font.font("System Bold", 14));
            label.setStyle(
                    "-fx-background-color: " + type.getColor() + "; -fx-padding: 10 20; -fx-background-radius: 20;");

            StackPane root = new StackPane(label);
            root.setStyle("-fx-background-color: transparent;");
            root.setAlignment(Pos.CENTER);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            toastStage.setScene(scene);

            // Position toast at the bottom center of the owner stage
            toastStage.setOnShown(e -> {
                toastStage.setX(ownerStage.getX() + ownerStage.getWidth() / 2 - toastStage.getWidth() / 2);
                toastStage.setY(ownerStage.getY() + ownerStage.getHeight() * 0.85 - toastStage.getHeight() / 2);
            });

            toastStage.show();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.seconds(2.5));
            fadeOut.setOnFinished(e -> toastStage.close());

            fadeIn.play();
            fadeOut.play();
        });
    }
}
