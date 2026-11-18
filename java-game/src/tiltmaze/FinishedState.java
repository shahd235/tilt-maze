package tiltmaze;

/** The round is over (goal reached or time ran out). Messages are ignored. */
class FinishedState implements GameState {

    private final boolean won;

    FinishedState(boolean won) {
        this.won = won;
    }

    @Override
    public String name() {
        return won ? "GOAL REACHED" : "TIME UP";
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void onMessage(Game game, Message m) {
        if (m.type == Message.Type.BTN) {
            game.log("Button ignored: the round is over.");
        }
    }

    @Override
    public void onTick(Game game) {
        // nothing to do
    }
}
