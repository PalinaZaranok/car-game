package org.example.cargame.game;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.example.cargame.state.CarState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class GameLoop extends AnimationTimer {
    private final List<Coin> coins = new ArrayList<>();
    private final List<Person> pedestrians = new ArrayList<>();
    private final Random rng = new Random();
    private final Consumer<Character> eventSink;

    private int score = 0;
    private int collisions = 0;
    private long nextCoinSpawn = 0;
    private long nextPedSpawn  = 0;
    private long stunnedUntil  = 0;
    private long flashUntil    = 0;

    //Настройки автомобиля
    private static final double MAX_STEER_ANGLE = 30.0;
    private static final double MAX_SPEED_FWD = 300.0;
    private static final double MAX_SPEED_BWD = 120.0;
    private static final double ACCEL = 200.0;
    private static final double BRAKE = 450.0;
    private static final double FRICTION = 120.0;
    private static final double TURN_FACTOR = 4.0;
    private static final double CAR_W = 26;
    private static final double CAR_H = 46;

    private final Canvas canvas;
    private final CarState state;

    private double x, y;
    private double angle = 0;
    private double speed = 0;
    private long lastTime = 0;

    public GameLoop(Canvas canvas, CarState state, Consumer<Character> eventSink) {
        this.eventSink = eventSink;
        this.canvas = canvas;
        this.state = state;
        this.x = 0;
        this.y = 0;
    }

    @Override
    public void handle(long now) {
        if (lastTime == 0) {
            x = canvas.getWidth() / 2;
            y = canvas.getHeight() * 0.75;
            lastTime = now;
            nextCoinSpawn = now;
            nextPedSpawn  = now + 2_000_000_000L;
            return;
        }

        double dt = (now - lastTime) / 1_000_000_000.0;
        lastTime = now;

        if (dt > 0.1) dt = 0.1;

        trySpawn(now);
        update(dt);
        render();
    }

    private void update(double dt) {
        int steer    = state.getSteer();
        int forward  = state.getForward();
        int backward = state.getBackward();

        long now = System.nanoTime();
        if (now < stunnedUntil) {
            speed *= 0.9;
        } else {
            if (forward == 1) {
                speed += ACCEL * dt;
                if (speed > MAX_SPEED_FWD) speed = MAX_SPEED_FWD;
            } else if (backward == 1) {
                if (speed > 0) {
                    speed -= BRAKE * dt;
                    if (speed < 0) speed = 0;
                } else {
                    speed -= ACCEL * 0.6 * dt;
                    if (speed < -MAX_SPEED_BWD) speed = -MAX_SPEED_BWD;
                }
            } else {
                if (speed > 0) {
                    speed -= FRICTION * dt;
                    if (speed < 0) speed = 0;
                } else if (speed < 0) {
                    speed += FRICTION * dt;
                    if (speed > 0) speed = 0;
                }
            }

            double wheelAngle = (steer - 512) / 512.0 * MAX_STEER_ANGLE;
            if (Math.abs(speed) > 1) {
                double speedFactor = speed / MAX_SPEED_FWD; // отриц. при заднем ходе
                angle += wheelAngle * speedFactor * TURN_FACTOR * dt;
            }
        }

        double rad = Math.toRadians(angle);
        x += Math.sin(rad) * speed * dt;
        y -= Math.cos(rad) * speed * dt;

        double carHalfX = Math.abs(Math.cos(rad)) * (CAR_W / 2)
                + Math.abs(Math.sin(rad)) * (CAR_H / 2);

        double leftLimit  = getRoadLeft() + carHalfX;
        double rightLimit = getRoadRight() - carHalfX;

        boolean hitCurb = false;
        if (x < leftLimit)  { x = leftLimit;  hitCurb = true; }
        if (x > rightLimit) { x = rightLimit; hitCurb = true; }

        if (hitCurb) {
            speed *= 0.7;
            if (Math.abs(speed) < 5) speed = 0;
        }

        Iterator<Coin> ci = coins.iterator();
        while (ci.hasNext()) {
            Coin c = ci.next();
            if (carHitsCircle(c.x, c.y, c.radius)) {
                ci.remove();
                score++;
                eventSink.accept('C');
            }
        }

        Iterator<Person> pi = pedestrians.iterator();
        while (pi.hasNext()) {
            Person p = pi.next();
            if (carHitsCircle(p.x, p.y, Math.max(p.w, p.h) / 2)) {
                pi.remove();
                collisions++;
                eventSink.accept('X');
                speed = 0;
                stunnedUntil = System.nanoTime() + 500_000_000L;
                flashUntil   = System.nanoTime() + 500_000_000L;
            }
        }

        double h = canvas.getHeight();
        if (y < -20) y = h + 20;
        if (y > h + 20) y = -20;
    }

    private void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        g.setFill(Color.web("#2f2f2f"));
        g.fillRect(0, 0, w, h);

        double roadWidth = getRoadWidth();
        double roadX = getRoadLeft();
        g.setFill(Color.web("#555555"));
        g.fillRect(roadX, 0, roadWidth, h);

        g.setStroke(Color.WHITE);
        g.setLineWidth(3);
        g.strokeLine(roadX, 0, roadX, h);
        g.strokeLine(roadX + roadWidth, 0, roadX + roadWidth, h);

        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        g.setLineDashes(20, 20);
        g.strokeLine(w / 2, 0, w / 2, h);
        g.setLineDashes(null);

        drawCoins(g);
        drawPerson(g);
        drawCar(g);
        drawHud(g);

        drawCar(g);
    }

    private void drawCar(GraphicsContext g) {
        g.save();
        g.translate(x, y);
        g.rotate(angle);
        g.setFill(Color.web("#e63946"));
        g.fillRoundRect(-CAR_W / 2, -CAR_H / 2, CAR_W, CAR_H, 6, 6);
        g.setStroke(Color.BLACK);
        g.setLineWidth(1.5);
        g.strokeRoundRect(-CAR_W / 2, -CAR_H / 2, CAR_W, CAR_H, 6, 6);

        g.setFill(Color.web("#a8dadc"));
        g.fillRect(-CAR_W / 2 + 4, -CAR_H / 2 + 4, CAR_W - 8, 8);

        g.restore();
    }

    private double getRoadWidth() {
        double w = canvas.getWidth();
        return Math.min(w * 0.5, 300);
    }

    private double getRoadLeft() {
        return (canvas.getWidth() - getRoadWidth()) / 2;
    }

    private double getRoadRight() {
        return getRoadLeft() + getRoadWidth();
    }

    private void trySpawn(long now) {
        if (now > nextCoinSpawn && coins.size() < 5) {
            spawnCoin();
            nextCoinSpawn = now + 1_500_000_000L;
        }
        if (now > nextPedSpawn && pedestrians.size() < 3) {
            spawnPedestrian();
            nextPedSpawn = now + 3_000_000_000L;
        }
    }

    private void spawnCoin() {
        double left  = getRoadLeft()  + 30;
        double right = getRoadRight() - 30;
        if (right <= left) return;

        for (int tries = 0; tries < 10; tries++) {
            double cx = left + rng.nextDouble() * (right - left);
            double cy = 60 + rng.nextDouble() * (canvas.getHeight() - 120);
            if (Math.hypot(cx - x, cy - y) > 100) {     // не прямо под машиной
                coins.add(new Coin(cx, cy));
                return;
            }
        }
    }

    private void spawnPedestrian() {
        double left  = getRoadLeft()  + 20;
        double right = getRoadRight() - 20;
        if (right <= left) return;

        for (int tries = 0; tries < 10; tries++) {
            double px = left + rng.nextDouble() * (right - left);
            double py = 60 + rng.nextDouble() * (canvas.getHeight() - 120);
            if (Math.hypot(px - x, py - y) > 150) {
                pedestrians.add(new Person(px, py));
                return;
            }
        }
    }

    private boolean carHitsCircle(double cx, double cy, double r) {
        double dx = cx - x;
        double dy = cy - y;
        double rad = Math.toRadians(-angle);
        double lx =  dx * Math.cos(rad) - dy * Math.sin(rad);
        double ly =  dx * Math.sin(rad) + dy * Math.cos(rad);
        return Math.abs(lx) < CAR_W / 2 + r
                && Math.abs(ly) < CAR_H / 2 + r;
    }

    private void drawCoins(GraphicsContext g) {
        for (Coin c : coins) {
            g.setFill(Color.GOLD);
            g.fillOval(c.x - c.radius, c.y - c.radius, c.radius * 2, c.radius * 2);
            g.setStroke(Color.web("#b8860b"));
            g.setLineWidth(2);
            g.strokeOval(c.x - c.radius, c.y - c.radius, c.radius * 2, c.radius * 2);
            g.setFill(Color.web("#b8860b"));
            g.setFont(Font.font("System", 12));
            g.fillText("$", c.x - 4, c.y + 4);
        }
    }

    private void drawPerson(GraphicsContext g) {
        for (Person p : pedestrians) {

            g.setFill(Color.web("#f4a261"));
            g.fillRoundRect(p.x - p.w / 2, p.y - p.h / 2, p.w, p.h, 4, 4);

            g.setFill(Color.web("#ffd7a8"));
            g.fillOval(p.x - 5, p.y - p.h / 2 - 6, 10, 10);

            g.setStroke(Color.BLACK);
            g.setLineWidth(1);
            g.strokeRoundRect(p.x - p.w / 2, p.y - p.h / 2, p.w, p.h, 4, 4);
        }
    }

    private void drawHud(GraphicsContext g) {
        g.setFill(Color.WHITE);
        g.setFont(Font.font("System", 16));
        g.fillText("Счёт: " + score, 12, 26);
        g.fillText("Столкновений: " + collisions, 12, 48);

        if (System.nanoTime() < flashUntil) {

        }
    }
}
