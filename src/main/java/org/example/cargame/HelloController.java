package org.example.cargame;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.example.cargame.serial.SerialReader;
import org.example.cargame.state.CarState;

public class HelloController {
    @FXML private ComboBox<String> portCombo;
    @FXML private Button connectBtn;
    @FXML private Button refreshBtn;
    @FXML private Label statusLabel;
    @FXML private Canvas gameCanvas;
    @FXML private StackPane gamePane;

    private final CarState carState = new CarState();
    private SerialReader serial;

    @FXML
    public void initialize() {
        serial = new SerialReader(carState, msg ->
                Platform.runLater(() -> statusLabel.setText(msg)));

        refreshPorts();
        returnFocusToGame();
    }

    @FXML
    private void refreshPorts() {
        portCombo.getItems().setAll(SerialReader.getAvailablePorts());
        if (!portCombo.getItems().isEmpty()) portCombo.getSelectionModel().selectFirst();
        returnFocusToGame();
    }

    @FXML
    private void toggleConnection() {
        if (serialConnected()) {
            serial.disconnect();
            connectBtn.setText("Подключиться");
        } else {
            String port = portCombo.getValue();
            if (port == null) {
                statusLabel.setText("Порт не выбран");
                return;
            }
            if (serial.connect(port)) {
                connectBtn.setText("Отключиться");
                carState.set(512, 0, 0);
            }
        }
        returnFocusToGame();
    }

    private boolean serialConnected() {
        return "Отключиться".equals(connectBtn.getText());
    }

    public CarState getCarState(){
        return carState;
    }

    public void bindCanvasToParent(StackPane pane) {
        gameCanvas.widthProperty().bind(pane.widthProperty());
        gameCanvas.heightProperty().bind(pane.heightProperty());
    }

    public void sendEvent(Character code) {
        if (serial != null) serial.sendEvent(code);
    }
    public Canvas getGameCanvas() { return gameCanvas; }

    public void returnFocusToGame() {
        gamePane.requestFocus();
    }

    public boolean isSerialConnected() {
        return serial != null && serial.isConnected();
    }
}