package org.example.cargame;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.example.cargame.serial.SerialReader;
import org.example.cargame.state.CarState;

public class CarGameController {
    @FXML private ComboBox<String> portCombo;

    @FXML private Button connectBtn;
    @FXML private Button refreshBtn;

    @FXML private Label statusLabel;
    @FXML private Label steerLabel;
    @FXML private Label forwardLabel;
    @FXML private Label backwardLabel;
    @FXML private Label rawLabel;

    @FXML private Canvas gameCanvas;

    private final CarState carState = new CarState();
    private SerialReader serial;

    @FXML
    public void initialize() {
        serial = new SerialReader(carState, msg ->
                Platform.runLater(() -> statusLabel.setText(msg)));

        portCombo.setFocusTraversable(false);

        refreshPorts();

        connectBtn.setFocusTraversable(false);
        refreshBtn.setFocusTraversable(false);

        AnimationTimer telemetryTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                steerLabel.setText(String.format("Руль: %4d", carState.getSteer()));
                forwardLabel.setText("Вперёд: " + (carState.getForward() == 1 ? "ВКЛ" : "—"));
                backwardLabel.setText("Назад: " + (carState.getBackward() == 1 ? "ВКЛ" : "—"));
                rawLabel.setText("Пакет: " + carState.getRawPacket());
            }
        };
        telemetryTimer.start();
    }

    @FXML
    private void refreshPorts() {
        portCombo.getItems().setAll(SerialReader.getAvailablePorts());

        if (!portCombo.getItems().isEmpty()) {
            portCombo.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void toggleConnection() {
        if (serialConnected()) {
            serial.disconnect();

            connectBtn.setText("Подключиться");
            connectBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            String port = portCombo.getValue();

            if (port == null) {
                statusLabel.setText("Порт не выбран");

                return;
            }

            if (serial.connect(port)) {
                connectBtn.setText("Отключиться");
                connectBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        }

        Platform.runLater(() -> {
            gameCanvas.setFocusTraversable(true);
            gameCanvas.requestFocus();
        });
    }

    private boolean serialConnected() {
        return "Отключиться".equals(connectBtn.getText());
    }

    public CarState getCarState() {
        return carState;
    }

    public void sendEvent(Character code) {
        if (serial != null) {
            serial.sendEvent(code);
        }
    }

    public Canvas getGameCanvas() {
        return gameCanvas;
    }
}