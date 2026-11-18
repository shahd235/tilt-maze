package tiltmaze;

/**
 * OBSERVER pattern: anything that wants to follow the game (the window, a log
 * file, a test) implements this interface and registers with Game.addListener().
 * The Game (the subject) calls these methods whenever something changes, and
 * does not need to know who is listening.
 */
public interface GameListener {

    /** Something in the game changed (tilt, state, score, trap, time...). */
    void onUpdate(Game game);

    /** A line of text for the event log. */
    void onLog(String text);
}
