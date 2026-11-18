package tiltmaze;

import javax.swing.*;

/**
 * Start the game program.
 *   java -jar tilt-maze.jar            window with a port list (choose the XBee port)
 *   java -jar tilt-maze.jar COM4       connect to COM4 straight away
 *   java -jar tilt-maze.jar --test     test mode with no hardware
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel panel = new GamePanel();
            JFrame frame = new JFrame("Smart Tilt-Maze with Remote Sabotage - game controller");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);

            if (args.length > 0) {
                if (args[0].equals("--test")) {
                    LoopbackLink l = new LoopbackLink(true);
                    panel.use(l, l);
                } else {
                    try {
                        panel.use(new SerialLink(args[0]), null);
                    } catch (Throwable t) {
                        JOptionPane.showMessageDialog(frame, t.getMessage(), "Cannot connect",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }
}
