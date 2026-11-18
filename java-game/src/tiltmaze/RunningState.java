package tiltmaze;

/**
 * The round is on. This state holds the game rules:
 * checkpoints give points, GOAL wins, the Blocker's button opens the trapdoor
 * (then a cooldown), every drop costs the Runner time, and time running out loses.
 */
class RunningState implements GameState {

    @Override
    public String name() {
        return "RUNNING";
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void onMessage(Game game, Message m) {
        switch (m.type) {
            case CP1:
                if (!game.checkpoint1) {
                    game.checkpoint1 = true;
                    game.send("BUZZ,4");
                    game.log("Checkpoint 1 reached.");
                }
                break;

            case CP2:
                if (!game.checkpoint2) {
                    game.checkpoint2 = true;
                    game.send("BUZZ,4");
                    game.log("Checkpoint 2 reached.");
                }
                break;

            case GOAL:
                game.send("BUZZ,5");
                game.log("GOAL! The Runner wins.");
                game.finishRound(true);
                break;

            case DARK:
                game.darkEntries++;
                game.log("The ball entered the dark zone.");
                break;

            case BTN:
                if (game.trapReady) {
                    game.trapReady = false;
                    game.cooldownEndsMs = game.nowMs() + Config.COOLDOWN_SECONDS * 1000L;
                    game.send("DROP");
                    game.send("BUZZ,1");
                    game.setLed(Game.Led.RED);
                    game.log("Blocker pressed the button: trapdoor opens.");
                } else {
                    game.log("Button ignored: trap is cooling down.");
                }
                break;

            case DROPPED:
                game.drops++;
                game.penaltyMs += Config.DROP_PENALTY_SECONDS * 1000L;
                game.log("Trapdoor finished. Runner loses "
                        + Config.DROP_PENALTY_SECONDS + " s.");
                break;

            case READY:
                game.log("The maze board restarted.");
                break;

            default:
                break;
        }
    }

    @Override
    public void onTick(Game game) {
        // cooldown finished: the Blocker may use the trap again
        if (!game.trapReady && game.nowMs() >= game.cooldownEndsMs) {
            game.trapReady = true;
            game.send("BUZZ,2");
            game.setLed(Game.Led.GREEN);
            game.log("Trap is ready again.");
        }
        // time ran out: the Runner loses
        if (game.leftMs() <= 0) {
            game.send("BUZZ,3");
            game.log("Time is up.");
            game.finishRound(false);
        }
    }
}
