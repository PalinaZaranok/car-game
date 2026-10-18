package org.example.cargame.game;


import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import org.example.cargame.state.CarState;

/**
 * Временная заглушка для управления машиной с клавиатуры.
 * Используется, пока Arduino не подключена или для отладки физики.
 *
 *   ← / →  — руль (шаг 100, диапазон 0..1023, 512 = прямо)
 *   ↑      — газ (пока удерживается)
 *   ↓      — тормоз / задний ход (пока удерживается)
 */
public class KeyboardStub {

    private static final int STEER_STEP = 100;

    private final CarState state;
    private int steer = 512;
    private int forward = 0;
    private int backward = 0;

    public KeyboardStub(CarState state, Scene scene) {
        this.state = state;

        scene.setOnKeyPressed(e -> {
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
            switch (e.getCode()) {
                case UP   -> forward = 0;
                case DOWN -> backward = 0;
                default   -> { return; }
            }
            state.set(steer, forward, backward);
        });
    }
}
