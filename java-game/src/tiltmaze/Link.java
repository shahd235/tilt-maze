package tiltmaze;

import java.util.function.Consumer;

/**
 * The connection between the game and the XBee on the laptop.
 * The game only knows this interface, so the real serial port (SerialLink)
 * and the no-hardware test mode (LoopbackLink) can be swapped freely.
 */
public interface Link {

    /** Send one line of text (the newline is added by the link). */
    void send(String line);

    /** The game gives the link a listener that is called for every received line. */
    void setLineListener(Consumer<String> listener);

    /** Close the connection. */
    void close();

    /** A short name shown in the window, for example "COM4" or "test mode". */
    String describe();
}
