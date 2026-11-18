package tiltmaze;

/**
 * One line received from the maze or the remote, already understood.
 * The lines follow docs/messages.md. A line that is not in the dictionary
 * (damaged, unknown, or empty) becomes UNKNOWN and is ignored by the game.
 */
public final class Message {

    /** Every message the laptop can receive. */
    public enum Type { READY, TILT, CP1, CP2, GOAL, DARK, DROPPED, RREADY, BTN, UNKNOWN }

    public final Type type;
    public final int x;   // tilt left/right in degrees (only for TILT)
    public final int y;   // tilt forward/back in degrees (only for TILT)

    private Message(Type type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    /** True for messages that come from the maze board. */
    public boolean fromMaze() {
        switch (type) {
            case READY: case TILT: case CP1: case CP2: case GOAL: case DARK: case DROPPED:
                return true;
            default:
                return false;
        }
    }

    /** True for messages that come from the remote. */
    public boolean fromRemote() {
        return type == Type.RREADY || type == Type.BTN;
    }

    /** Turn one text line into a Message. Never throws: bad input gives UNKNOWN. */
    public static Message parse(String line) {
        if (line == null) return new Message(Type.UNKNOWN, 0, 0);
        String s = line.trim();

        if (s.startsWith("TILT,")) {
            String[] parts = s.split(",");
            if (parts.length == 3) {
                try {
                    int x = clamp(Integer.parseInt(parts[1].trim()));
                    int y = clamp(Integer.parseInt(parts[2].trim()));
                    return new Message(Type.TILT, x, y);
                } catch (NumberFormatException e) {
                    // damaged number: fall through to UNKNOWN
                }
            }
            return new Message(Type.UNKNOWN, 0, 0);
        }

        switch (s) {
            case "READY":   return new Message(Type.READY, 0, 0);
            case "CP1":     return new Message(Type.CP1, 0, 0);
            case "CP2":     return new Message(Type.CP2, 0, 0);
            case "GOAL":    return new Message(Type.GOAL, 0, 0);
            case "DARK":    return new Message(Type.DARK, 0, 0);
            case "DROPPED": return new Message(Type.DROPPED, 0, 0);
            case "RREADY":  return new Message(Type.RREADY, 0, 0);
            case "BTN":     return new Message(Type.BTN, 0, 0);
            default:        return new Message(Type.UNKNOWN, 0, 0);
        }
    }

    private static int clamp(int v) {
        return Math.max(-45, Math.min(45, v));
    }
}
