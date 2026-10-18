package org.example.cargame.state;

public class CarState {
    private volatile int steer = 512; // руль направлен прямо 0 - влево, 1023 - вправо
    private volatile int forward = 0;
    private volatile int backward = 0;

    public void set(int steer, int forward, int backward){
        this.steer = steer;
        this.forward = forward;
        this.backward = backward;
    }

    public int getSteer(){
        return steer;
    }
    public int getForward(){
        return forward;
    }
    public int getBackward(){
        return backward;
    }
}
