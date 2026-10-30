package org.example.cargame.serial;

import org.example.cargame.state.CarState;
import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SerialReader {
    private final CarState carState;
    private final Consumer<String> status;

    private SerialPort port;
    private Thread readThread;
    private volatile boolean isRunning;

    public SerialReader(CarState carState, Consumer<String> status) {
        this.carState = carState;
        this.status = status;
    }

    /** Список системных имён доступных COM-портов. */
    public static List<String> getAvailablePorts() {
        List<String> names = new ArrayList<>();
        for (SerialPort p : SerialPort.getCommPorts()) {
            names.add(p.getSystemPortName());
        }
        return names;
    }

    public boolean connect(String portName){
        if (isRunning) return false;

        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (!port.openPort()) {
            status.accept("Не удалось открыть " + portName);
            return false;
        }
        port.setDTR();  
        port.setRTS();

        isRunning = true;
        readThread = new Thread(this::readLoop, "SerialReader");
        readThread.setDaemon(true);
        readThread.start();
        status.accept("Подключено: " + portName);

        return true;
    }

    public void disconnect(){
        isRunning = false;
        if (port != null && port.isOpen()){
            port.closePort();
        }
        readThread = null;
        status.accept("Отключено");
    }

    private void readLoop() {
        StringBuilder buffer = new StringBuilder();
        byte[] chunk = new byte[128];

        try (InputStream in = port.getInputStream()) {
            while (isRunning) {
                try {
                    int n = in.read(chunk);
                    if (n < 0) {
                        if (!isRunning) break;
                        continue;
                    }
                    for (int i = 0; i < n; i++) {
                        char c = (char) chunk[i];
                        if (c == '\n') {
                            parseLine(buffer.toString());
                            buffer.setLength(0);
                        } else if (c != '\r') {
                            buffer.append(c);
                        }
                    }
                } catch (com.fazecast.jSerialComm.SerialPortTimeoutException e) {
                    // Таймаут — это нормально, если данных нет. Просто идем на следующую итерацию цикла.
                } catch (IOException e) {
                    if (isRunning) {
                        status.accept("Ошибка чтения: " + e.getMessage());
                    }
                    break; // Выходим из цикла while при реальной ошибке (например, выдернули USB)
                }
            }
        } catch (IOException e) {
            // Ошибка при попытке получить InputStream (например, порт уже закрыт)
            if (isRunning) {
                status.accept("Ошибка потока: " + e.getMessage());
            }
        } finally {
            isRunning = false;
            if (port != null && port.isOpen()) port.closePort();
        }
    }

    /** Ожидает строку "S<num>,F<0|1>,B<0|1>"; некорректные — игнорирует. */
    private void parseLine(String line) {
        System.out.println("DEBUG LINE: [" + line + "]");
        if (line.isEmpty() || line.charAt(0) != 'S') return;


        int fIdx = line.indexOf(",F");
        int bIdx = line.indexOf(",B");
        if (fIdx < 0 || bIdx < 0 || bIdx <= fIdx) return;

        try {
            int steer    = Integer.parseInt(line.substring(1, fIdx).trim());
            int forward  = Integer.parseInt(line.substring(fIdx + 2, bIdx).trim());
            int backward = Integer.parseInt(line.substring(bIdx + 2).trim());

            if (steer < 0 || steer > 4095) return;
            if (forward != 0 && forward != 1) return;
            if (backward != 0 && backward != 1) return;

            if (steer > 1023) {
                steer = steer * 1023 / 4095;
            }
            carState.set(steer, forward, backward);
            System.out.println("RAW: " + line);
        } catch (NumberFormatException ignored) {

        }
    }

    // отправка событий
    public void sendEvent(char code) {
        if (port == null || !port.isOpen()) return;
        try {
            port.getOutputStream().write((code + "\n").getBytes());
            port.getOutputStream().flush();
        } catch (IOException ignored) { }
    }

    public boolean isConnected() {
        return isRunning;
    }
}
