package tiltmaze;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

/**
 * The game controller on the laptop.
 *
 * It receives every line from the XBee (through a Link), turns it into a
 * Message, and hands it to the current GameState. It sends back the commands
 * from docs/messages.md: DROP to the maze, LED,... and BUZZ,n to the remote.
 *
 * Design patterns used:
 *   OBSERVER - GameListener: the window follows the game without the game knowing it.
 *   STATE    - GameState: IdleState, RunningState, FinishedState decide what messages do.
 *
 * All public methods are synchronized because messages arrive on the serial
 * thread while the window's timer calls tick() on another thread.
 */
public final class Game {

    /** The colour of the remote's RGB LED. */
    public enum Led { GREEN, RED, BLUE }

    private final Link link;
    private final LongSupplier clock;
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

    private GameState state = new IdleState();

    // ---- what the game knows (read by the window, changed by the states) ----
    int tiltX, tiltY;                  // latest tilt of the maze, in degrees
    boolean checkpoint1, checkpoint2;  // checkpoints reached this round
    int darkEntries;                   // times the ball entered the dark zone
    int drops;                         // times the trapdoor opened this round
    boolean won;                       // the Runner reached the goal
    boolean trapReady = true;          // false during the cooldown
    long cooldownEndsMs;               // when the cooldown ends
    long roundStartMs;                 // when the round began
    long penaltyMs;                    // time lost to trapdoor drops
    private long frozenLeftMs;         // time left when the round ended
    private Led led = Led.BLUE;
    private boolean mazeSeen, remoteSeen;

    public Game(Link link) {
        this(link, System::currentTimeMillis);
    }

    /** The clock can be replaced, so tests do not have to wait in real time. */
    public Game(Link link, LongSupplier clock) {
        this.link = link;
        this.clock = clock;
        link.setLineListener(this::handleLine);
    }

    // ------------------------------------------------------------ observer
    public void addListener(GameListener l) {
        listeners.add(l);
    }

    private void changed() {
        for (GameListener l : listeners) l.onUpdate(this);
    }

    void log(String text) {
        for (GameListener l : listeners) l.onLog(text);
    }

    // ------------------------------------------------------------ incoming
    /** Called for every line received from the XBee. */
    public synchronized void handleLine(String line) {
        Message m = Message.parse(line);
        if (m.type == Message.Type.UNKNOWN) {
            if (line != null && !line.trim().isEmpty()) log("Ignored: " + line.trim());
            return;
        }
        if (m.fromMaze()) mazeSeen = true;
        if (m.fromRemote()) remoteSeen = true;

        if (m.type == Message.Type.TILT) {
            tiltX = m.x;
            tiltY = m.y;
        } else if (m.type == Message.Type.RREADY) {
            send("LED," + led);                  // a remote that just started learns the colour
            log("Remote connected.");
        } else {
            if (m.type == Message.Type.READY) log("Maze connected.");
            state.onMessage(this, m);
        }
        changed();
    }

    /** Called about 10 times a second by the window's timer. */
    public synchronized void tick() {
        state.onTick(this);
        changed();
    }

    // ------------------------------------------------------------ controls
    /** Start a new round (from waiting or after a finished round). */
    public synchronized void startRound() {
        checkpoint1 = false;
        checkpoint2 = false;
        darkEntries = 0;
        drops = 0;
        won = false;
        penaltyMs = 0;
        trapReady = true;
        roundStartMs = nowMs();
        state = new RunningState();
        setLed(Led.GREEN);
        log("Round started: " + Config.ROUND_SECONDS + " s on the clock.");
        changed();
    }

    /** Stop the round and go back to waiting. */
    public synchronized void stopRound() {
        state = new IdleState();
        trapReady = true;
        setLed(Led.BLUE);
        log("Round stopped.");
        changed();
    }

    /** Used by RunningState when the round ends. */
    void finishRound(boolean runnerWon) {
        frozenLeftMs = Math.max(0, leftMs());
        won = runnerWon;
        state = new FinishedState(runnerWon);
        trapReady = true;
        setLed(Led.BLUE);
    }

    // ------------------------------------------------------------ outgoing
    /** Send one command line to the XBee. */
    void send(String line) {
        link.send(line);
        log("Sent: " + line);
    }

    /** Change the remote's LED colour (only sends when the colour changes). */
    void setLed(Led newLed) {
        if (newLed != led) {
            led = newLed;
            send("LED," + led);
        }
    }

    // ------------------------------------------------------------ queries
    long nowMs() {
        return clock.getAsLong();
    }

    /** Milliseconds left on the Runner's clock. */
    long leftMs() {
        if (!state.isRunning()) {
            return (state instanceof FinishedState) ? frozenLeftMs : Config.ROUND_SECONDS * 1000L;
        }
        return Config.ROUND_SECONDS * 1000L - (nowMs() - roundStartMs) - penaltyMs;
    }

    public synchronized int secondsLeft() {
        return (int) Math.ceil(Math.max(0, leftMs()) / 1000.0);
    }

    public synchronized int cooldownSecondsLeft() {
        if (trapReady || !state.isRunning()) return 0;
        return (int) Math.ceil(Math.max(0, cooldownEndsMs - nowMs()) / 1000.0);
    }

    public synchronized int score() {
        int points = (checkpoint1 ? Config.CHECKPOINT_POINTS : 0)
                   + (checkpoint2 ? Config.CHECKPOINT_POINTS : 0);
        if (won) points += Config.GOAL_POINTS + secondsLeft() * Config.POINTS_PER_SECOND_LEFT;
        return points;
    }

    public synchronized String stateName()   { return state.name(); }
    public synchronized boolean isRunning()  { return state.isRunning(); }
    public synchronized int tiltX()          { return tiltX; }
    public synchronized int tiltY()          { return tiltY; }
    public synchronized boolean checkpoint1() { return checkpoint1; }
    public synchronized boolean checkpoint2() { return checkpoint2; }
    public synchronized int darkEntries()    { return darkEntries; }
    public synchronized int drops()          { return drops; }
    public synchronized boolean trapReady()  { return trapReady; }
    public synchronized boolean mazeSeen()   { return mazeSeen; }
    public synchronized boolean remoteSeen() { return remoteSeen; }
    public synchronized Led led()            { return led; }
}
