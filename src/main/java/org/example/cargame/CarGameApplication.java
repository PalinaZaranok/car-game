package org.example.cargame;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.cargame.game.GameLoop;
import org.example.cargame.game.KeyboardStub;

import java.io.IOException;

public class CarGameApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CarGameApplication.class.getResource("main-view.fxml"));
        BorderPane root = fxmlLoader.load();

        CarGameController controller = fxmlLoader.getController();

        stage.setResizable(false);

        GameLoop loop = new GameLoop(
                controller.getGameCanvas(),
                controller.getCarState(),
                controller::sendEvent
        );
        loop.start();

        Scene scene = new Scene(root);
        new KeyboardStub(controller.getCarState(), scene);

        stage.setTitle("Car Game");
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> {
            controller.getGameCanvas().setFocusTraversable(true);
            controller.getGameCanvas().requestFocus();
        });
    }
}