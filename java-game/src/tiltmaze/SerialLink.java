package tiltmaze;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * The real link: the XBee Explorer on a USB serial port (for example COM4).
 * Uses the jSerialComm library. A background thread reads characters and
 * hands every complete line to the game.
 */
public class SerialLink implements Link {

    private final SerialPort port;
    private final String name;
    private volatile Consumer<String> listener;
    private volatile boolean running = true;
    private final Thread reader;

    /** Open the port. Throws IOException if it cannot be opened. */
    public SerialLink(String portName) throws IOException {
        this.name = portName;
        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(Config.BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 0);
        if (!port.openPort()) {
            throw new IOException("Cannot open " + portName
                    + " (is XCTU or another program using it?)");
        }
        reader = new Thread(this::readLoop, "serial-reader");
        reader.setDaemon(true);
        reader.start();
    }

    /** Names of the serial ports found on this computer, for the drop-down list. */
    public static String[] listPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName();
        }
        return names;
    }

    // Collect characters into lines. A line ends at '\n'; '\r' is ignored.
    private void readLoop() {
        StringBuilder line = new StringBuilder();
        InputStream in = port.getInputStream();
        while (running) {
            try {
                int c = in.read();
                if (c < 0) continue;
                if (c == '\n') {
                    deliver(line.toString());
                    line.setLength(0);
                } else if (c != '\r' && line.length() < 80) {
                    line.append((char) c);
                }
            } catch (IOException e) {
                if (running) {
                    deliver("");          // ignored by the game
                }
            }
        }
    }

    private void deliver(String text) {
        Consumer<String> l = listener;
        if (l != null && !text.isEmpty()) l.accept(text);
    }

    @Override
    public synchronized void send(String line) {
        try {
            OutputStream out = port.getOutputStream();
            out.write((line + "\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
        } catch (IOException e) {
            // A lost line is allowed: the game keeps going.
        }
    }

    @Override
    public void setLineListener(Consumer<String> listener) {
        this.listener = listener;
    }

    @Override
    public void close() {
        running = false;
        port.closePort();
    }

    @Override
    public String describe() {
        return name;
    }
}
