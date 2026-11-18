package tiltmaze;

/**
 * STATE pattern: the game is always in exactly one state (waiting, running,
 * finished) and the state decides what each message and each clock tick does.
 * The Game object only forwards messages to its current state.
 */
public interface GameState {

    /** Name shown in the window. */
    String name();

    /** True while the Runner's clock is running. */
    boolean isRunning();

    /** A message arrived from the maze or the remote (TILT is handled by Game itself). */
    void onMessage(Game game, Message message);

    /** Called about 10 times a second. */
    void onTick(Game game);
}
