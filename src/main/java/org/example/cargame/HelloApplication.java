package org.example.cargame;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.cargame.game.GameLoop;
import org.example.cargame.game.KeyboardStub;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));

        BorderPane root = fxmlLoader.load();

        HelloController controller = fxmlLoader.getController();


        stage.setResizable(false);

        GameLoop loop = new GameLoop(
                controller.getGameCanvas(),
                controller.getCarState(),
                code -> controller.sendEvent(code)
        );
        loop.start();

        Scene scene = new Scene(root);
        // управление с клавиатуры
        new KeyboardStub(controller.getCarState(), scene);

        stage.setTitle("Car game");
        stage.setScene(scene);
        stage.show();

        root.setFocusTraversable(true);
        root.requestFocus();
    }

    public static void main(String[] args) {
        launch();
    }
}