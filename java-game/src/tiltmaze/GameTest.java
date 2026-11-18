package tiltmaze;

import java.util.Arrays;
import java.util.List;

/**
 * Test without hardware. It plays a pretend maze and remote against the game,
 * with a fake clock, and checks every command the game sends back.
 * Run it:  java -cp out tiltmaze.GameTest      (prints PASS or FAIL for each check)
 */
public class GameTest {

    private static int failures = 0;
    private static long fakeNow = 1_000_000L;     // the fake clock, in milliseconds

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + what);
        if (!ok) failures++;
    }

    private static void checkSent(String what, LoopbackLink link, String... expected) {
        List<String> want = Arrays.asList(expected);
        boolean ok = link.sent.equals(want);
        check(what + (ok ? "" : "   expected " + want + " got " + link.sent), ok);
        link.sent.clear();
    }

    private static void advance(Game game, int seconds) {
        for (int i = 0; i < seconds * 10; i++) {   // the window calls tick() 10 times a second
            fakeNow += 100;
            game.tick();
        }
    }

    public static void main(String[] args) {
        // ---------------- message parsing ----------------
        Message m = Message.parse("TILT,-7,12");
        check("TILT,-7,12 is read as x=-7 y=12", m.type == Message.Type.TILT && m.x == -7 && m.y == 12);
        m = Message.parse("TILT,99,-99");
        check("TILT values are limited to -45..45", m.x == 45 && m.y == -45);
        check("TILT,abc,1 is ignored", Message.parse("TILT,abc,1").type == Message.Type.UNKNOWN);
        check("TILT,1 (damaged) is ignored", Message.parse("TILT,1").type == Message.Type.UNKNOWN);
        check("garbage is ignored", Message.parse("xx#!").type == Message.Type.UNKNOWN);
        check("empty line is ignored", Message.parse("").type == Message.Type.UNKNOWN);
        check("null is ignored", Message.parse(null).type == Message.Type.UNKNOWN);
        check("READY with a carriage return still works", Message.parse("READY\r").type == Message.Type.READY);

        // ---------------- game ----------------
        LoopbackLink link = new LoopbackLink(false);
        Game game = new Game(link, () -> fakeNow);
        game.addListener(new GameListener() {
            @Override public void onUpdate(Game g) { }
            @Override public void onLog(String text) { System.out.println("      log: " + text); }
        });

        check("starts in WAITING", game.stateName().equals("WAITING"));
        link.inject("BTN");
        checkSent("BTN before a round sends nothing", link);
        link.inject("GOAL");
        check("GOAL before a round does nothing", game.stateName().equals("WAITING"));

        link.inject("TILT,5,-3");
        check("TILT is stored", game.tiltX() == 5 && game.tiltY() == -3);
        check("maze is marked as seen", game.mazeSeen());

        link.inject("RREADY");
        checkSent("remote start-up gets the current LED colour", link, "LED,BLUE");
        check("remote is marked as seen", game.remoteSeen());

        game.startRound();
        checkSent("start round: LED green", link, "LED,GREEN");
        check("state is RUNNING", game.stateName().equals("RUNNING"));
        check("clock starts at the full time", game.secondsLeft() == Config.ROUND_SECONDS);

        link.inject("CP1");
        checkSent("CP1 sends BUZZ,4", link, "BUZZ,4");
        link.inject("CP1");
        checkSent("CP1 a second time sends nothing", link);
        link.inject("CP2");
        checkSent("CP2 sends BUZZ,4", link, "BUZZ,4");
        check("both checkpoints are marked", game.checkpoint1() && game.checkpoint2());

        link.inject("DARK");
        check("dark zone entry is counted", game.darkEntries() == 1);
        checkSent("DARK sends nothing back", link);

        link.inject("BTN");
        checkSent("button: DROP, BUZZ,1, LED red", link, "DROP", "BUZZ,1", "LED,RED");
        check("trap is not ready during cooldown", !game.trapReady());
        link.inject("BTN");
        checkSent("button during cooldown sends nothing", link);

        int before = game.secondsLeft();
        link.inject("DROPPED");
        check("DROPPED counts the drop", game.drops() == 1);
        check("DROPPED costs " + Config.DROP_PENALTY_SECONDS + " s",
                before - game.secondsLeft() == Config.DROP_PENALTY_SECONDS);

        advance(game, Config.COOLDOWN_SECONDS - 1);
        check("trap still cooling after " + (Config.COOLDOWN_SECONDS - 1) + " s", !game.trapReady());
        advance(game, 2);
        checkSent("cooldown over: BUZZ,2 and LED green", link, "BUZZ,2", "LED,GREEN");
        check("trap is ready again", game.trapReady());

        int leftAtGoal = game.secondsLeft();
        link.inject("GOAL");
        checkSent("GOAL: BUZZ,5 and LED blue", link, "BUZZ,5", "LED,BLUE");
        check("state is GOAL REACHED", game.stateName().equals("GOAL REACHED"));
        int expectedScore = 2 * Config.CHECKPOINT_POINTS + Config.GOAL_POINTS
                + leftAtGoal * Config.POINTS_PER_SECOND_LEFT;
        check("score = checkpoints + goal + time bonus (" + expectedScore + ")", game.score() == expectedScore);
        check("clock is frozen after the goal", game.secondsLeft() == leftAtGoal);
        link.inject("BTN");
        link.inject("CP1");
        checkSent("messages after the goal are ignored", link);

        // ---------------- a round that runs out of time ----------------
        game.startRound();
        checkSent("second round: LED green", link, "LED,GREEN");
        check("counters were reset", !game.checkpoint1() && game.drops() == 0 && game.score() == 0);
        advance(game, Config.ROUND_SECONDS - 1);
        check("still running one second before the end", game.stateName().equals("RUNNING"));
        advance(game, 2);
        checkSent("time up: BUZZ,3 and LED blue", link, "BUZZ,3", "LED,BLUE");
        check("state is TIME UP", game.stateName().equals("TIME UP"));
        check("clock shows 0", game.secondsLeft() == 0);

        // ---------------- stop ----------------
        game.startRound();
        link.sent.clear();
        game.stopRound();
        checkSent("stop round: LED blue", link, "LED,BLUE");
        check("back to WAITING", game.stateName().equals("WAITING"));

        // ---------------- noise on the line ----------------
        link.inject("####");
        link.inject("");
        link.inject("TILT,x,y");
        checkSent("noise sends nothing and does not crash", link);

        System.out.println();
        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
