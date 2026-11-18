package tiltmaze;

/** No round is running. Sensor messages are ignored until a round is started. */
class IdleState implements GameState {

    @Override
    public String name() {
        return "WAITING";
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void onMessage(Game game, Message m) {
        if (m.type == Message.Type.BTN) {
            game.log("Button ignored: no round is running.");
        }
        // everything else is ignored while waiting
    }

    @Override
    public void onTick(Game game) {
        // nothing to do
    }
}
