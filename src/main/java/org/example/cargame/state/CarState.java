package org.example.cargame.state;

public class CarState {
    private volatile int hwSteer = 512;
    private volatile int hwForward = 0;
    private volatile int hwBackward = 0;
    private volatile String rawPacket = "—";

    private volatile int kbSteer = 512;
    private volatile int kbForward = 0;
    private volatile int kbBackward = 0;

    public void setHardware(int steer, int forward, int backward, String raw) {
        this.hwSteer = steer;
        this.hwForward = forward;
        this.hwBackward = backward;
        this.rawPacket = raw;
    }

    public void resetHardware() {
        this.hwSteer = 512;
        this.hwForward = 0;
        this.hwBackward = 0;
        this.rawPacket = "Отключено";
    }

    public void setKeyboard(int steer, int forward, int backward) {
        this.kbSteer = steer;
        this.kbForward = forward;
        this.kbBackward = backward;
    }

    public int getForward() {
        return (hwForward == 1 || kbForward == 1) ? 1 : 0;
    }

    public int getBackward() {
        return (hwBackward == 1 || kbBackward == 1) ? 1 : 0;
    }

    public int getSteer() {
        if (kbSteer != 512) {
            return kbSteer;
        }

        return hwSteer;
    }

    public String getRawPacket() {
        return rawPacket;
    }
}