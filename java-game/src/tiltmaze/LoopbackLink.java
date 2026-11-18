package tiltmaze;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Test-mode link with no hardware. Lines the game sends are stored in "sent",
 * and inject() pretends a line arrived from the maze or the remote.
 * With autoDropped on, the pretend maze answers DROP with DROPPED after 1.5 s,
 * like the real trapdoor does.
 */
public class LoopbackLink implements Link {

    public final List<String> sent = new ArrayList<>();
    private Consumer<String> listener;
    private final boolean autoDropped;
    private Timer timer;

    public LoopbackLink(boolean autoDropped) {
        this.autoDropped = autoDropped;
        if (autoDropped) timer = new Timer("fake-maze", true);
    }

    @Override
    public synchronized void send(String line) {
        sent.add(line);
        if (autoDropped && line.equals("DROP")) {
            timer.schedule(new TimerTask() {
                @Override public void run() { inject("DROPPED"); }
            }, 1500);
        }
    }

    /** Pretend this line was received from the maze or the remote. */
    public void inject(String line) {
        Consumer<String> l;
        synchronized (this) { l = listener; }
        if (l != null) l.accept(line);
    }

    @Override
    public synchronized void setLineListener(Consumer<String> listener) {
        this.listener = listener;
    }

    @Override
    public void close() {
        if (timer != null) timer.cancel();
    }

    @Override
    public String describe() {
        return "test mode (no hardware)";
    }
}
