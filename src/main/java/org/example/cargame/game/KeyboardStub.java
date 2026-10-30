package org.example.cargame.game;


import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import org.example.cargame.state.CarState;

import java.util.function.BooleanSupplier;

public class KeyboardStub {

    private final BooleanSupplier serialConnected;
    private boolean steering = false;
    private static final int STEER_STEP = 100;
    private final CarState state;
    private int steer = 512;
    private int forward = 0;
    private int backward = 0;

    public KeyboardStub(CarState state, Scene scene, BooleanSupplier serialConnected) {
        this.state = state;
        this.serialConnected = serialConnected;

        scene.setOnKeyPressed(e -> {
            if (serialConnected.getAsBoolean()) return;
            KeyCode code = e.getCode();
            switch (code) {
                case LEFT  -> steer = Math.max(0,    steer - STEER_STEP);
                case RIGHT -> steer = Math.min(1023, steer + STEER_STEP);
                case UP    -> forward = 1;
                case DOWN  -> backward = 1;
                default    -> { return; }
            }
            state.set(steer, forward, backward);
        });

        scene.setOnKeyReleased(e -> {
            if (serialConnected.getAsBoolean()) return;
            switch (e.getCode()) {
                case UP   -> forward = 0;
                case DOWN -> backward = 0;
                default   -> { return; }
            }
            state.set(steer, forward, backward);
        });
    }
}
