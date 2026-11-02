package org.example.cargame.game;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import org.example.cargame.state.CarState;

public class KeyboardStub {
    private final CarState state;

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;

    public KeyboardStub(CarState state, Scene scene) {
        this.state = state;

        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();

            switch (code) {
                case LEFT  -> leftPressed = true;
                case RIGHT -> rightPressed = true;
                case UP    -> upPressed = true;
                case DOWN  -> downPressed = true;
                default    -> { return; }
            }

            updateState();
        });

        scene.setOnKeyReleased(e -> {
            KeyCode code = e.getCode();

            switch (code) {
                case LEFT  -> leftPressed = false;
                case RIGHT -> rightPressed = false;
                case UP    -> upPressed = false;
                case DOWN  -> downPressed = false;
                default    -> { return; }
            }

            updateState();
        });
    }

    private void updateState() {
        int steer = 512;

        if (leftPressed && !rightPressed) {
            steer = 0;
        } else if (rightPressed && !leftPressed) {
            steer = 1023;
        }

        int forward = upPressed ? 1 : 0;
        int backward = downPressed ? 1 : 0;

        state.setKeyboard(steer, forward, backward);
    }
}