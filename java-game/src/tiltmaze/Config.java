package tiltmaze;

/**
 * All the game rules and settings in one place.
 * Change a number here to change the game. Nothing else needs editing.
 */
public final class Config {
    private Config() { }

    /** Time the Runner has to reach the goal, in seconds. */
    public static final int ROUND_SECONDS = 120;

    /** After the Blocker opens the trapdoor, it cannot be used again for this long. */
    public static final int COOLDOWN_SECONDS = 10;

    /** Seconds taken off the Runner's clock each time the trapdoor opens (ball dropped). */
    public static final int DROP_PENALTY_SECONDS = 10;

    /** Score for each checkpoint (CP1 and CP2 count once each per round). */
    public static final int CHECKPOINT_POINTS = 100;

    /** Score for reaching the goal, plus POINTS_PER_SECOND_LEFT for every second left. */
    public static final int GOAL_POINTS = 500;
    public static final int POINTS_PER_SECOND_LEFT = 5;

    /** Speed of the link: must match the XBee and Arduino setting (BD 9600). */
    public static final int BAUD_RATE = 9600;
}
