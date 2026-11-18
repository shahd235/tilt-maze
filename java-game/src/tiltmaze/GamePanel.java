package tiltmaze;

import javax.swing.*;
import java.awt.*;

/**
 * The game screen. It is a GameListener (OBSERVER pattern): the Game tells it
 * when something changed and it redraws. It contains the connection bar, the
 * tilt display, the status, the buttons and the event log.
 */
public class GamePanel extends JPanel implements GameListener {

    private Game game;
    private Link link;
    private LoopbackLink loopback;      // not null in test mode
    private javax.swing.Timer clockTimer;

    // connection bar
    private final JComboBox<String> portBox = new JComboBox<>();
    private final JLabel linkLabel = new JLabel("Not connected");

    // status labels
    private final JLabel stateLabel = big("WAITING");
    private final JLabel timeLabel = big("--");
    private final JLabel scoreLabel = big("0");
    private final JLabel checkLabel = new JLabel("-");
    private final JLabel trapLabel = new JLabel("-");
    private final JLabel ledLabel = new JLabel("-");
    private final JLabel darkLabel = new JLabel("-");
    private final JLabel dropLabel = new JLabel("-");
    private final JLabel mazeLabel = new JLabel("maze: no messages yet");
    private final JLabel remoteLabel = new JLabel("remote: no messages yet");

    private final TiltView tiltView = new TiltView();
    private final JTextArea logArea = new JTextArea(8, 40);
    private final JButton startButton = new JButton("Start round");
    private final JButton stopButton = new JButton("Stop round");
    private final JPanel testPanel = new JPanel();

    public GamePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildConnectionBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);
        refreshPorts();
        enableControls(false);
    }

    // ------------------------------------------------------------ layout
    private JPanel buildConnectionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton refresh = new JButton("Refresh");
        JButton connect = new JButton("Connect");
        JButton test = new JButton("Test mode (no hardware)");
        refresh.addActionListener(e -> refreshPorts());
        connect.addActionListener(e -> connectSerial());
        test.addActionListener(e -> startTestMode());
        bar.add(new JLabel("XBee port:"));
        bar.add(portBox);
        bar.add(refresh);
        bar.add(connect);
        bar.add(test);
        bar.add(linkLabel);
        return bar;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(8, 8));

        JPanel top = new JPanel(new GridLayout(1, 2, 8, 8));
        tiltView.setPreferredSize(new Dimension(220, 220));
        top.add(titled("Maze tilt", tiltView));

        JPanel info = new JPanel(new GridLayout(0, 2, 4, 4));
        addRow(info, "State", stateLabel);
        addRow(info, "Time left (s)", timeLabel);
        addRow(info, "Score", scoreLabel);
        addRow(info, "Checkpoints", checkLabel);
        addRow(info, "Trap", trapLabel);
        addRow(info, "Remote LED", ledLabel);
        addRow(info, "Dark zone", darkLabel);
        addRow(info, "Trapdoor drops", dropLabel);
        info.add(mazeLabel);
        info.add(remoteLabel);
        top.add(titled("Game", info));
        center.add(top, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        center.add(titled("Event log", new JScrollPane(logArea)), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout(4, 4));

        JPanel main = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        startButton.addActionListener(e -> { if (game != null) game.startRound(); });
        stopButton.addActionListener(e -> { if (game != null) game.stopRound(); });
        main.add(startButton);
        main.add(stopButton);
        bottom.add(main, BorderLayout.NORTH);

        // test mode: pretend to be the maze and the remote
        testPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));
        testPanel.setBorder(BorderFactory.createTitledBorder("Test mode: pretend messages"));
        for (String msg : new String[]{"READY", "RREADY", "CP1", "CP2", "GOAL", "DARK", "BTN"}) {
            JButton b = new JButton(msg);
            b.addActionListener(e -> { if (loopback != null) loopback.inject(msg); });
            testPanel.add(b);
        }
        JSlider sx = new JSlider(-45, 45, 0);
        JSlider sy = new JSlider(-45, 45, 0);
        javax.swing.event.ChangeListener tilt = e -> {
            if (loopback != null) loopback.inject("TILT," + sx.getValue() + "," + sy.getValue());
        };
        sx.addChangeListener(tilt);
        sy.addChangeListener(tilt);
        testPanel.add(new JLabel(" tilt x"));
        testPanel.add(sx);
        testPanel.add(new JLabel("y"));
        testPanel.add(sy);
        testPanel.setVisible(false);
        bottom.add(testPanel, BorderLayout.SOUTH);
        return bottom;
    }

    private static JLabel big(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 20f));
        return l;
    }

    private static void addRow(JPanel p, String name, JLabel value) {
        p.add(new JLabel(name + ":"));
        p.add(value);
    }

    private static JComponent titled(String title, JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------ connecting
    private void refreshPorts() {
        portBox.removeAllItems();
        try {
            for (String name : SerialLink.listPorts()) portBox.addItem(name);
        } catch (Throwable t) {
            // serial library not available: test mode still works
        }
        if (portBox.getItemCount() == 0) portBox.addItem("COM4");
        portBox.setEditable(true);
    }

    private void connectSerial() {
        String name = String.valueOf(portBox.getSelectedItem()).trim();
        try {
            use(new SerialLink(name), null);
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(this, t.getMessage(), "Cannot connect",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startTestMode() {
        LoopbackLink l = new LoopbackLink(true);
        use(l, l);
    }

    /** Switch to a new link: build a new Game and start the clock timer. */
    public void use(Link newLink, LoopbackLink newLoopback) {
        if (clockTimer != null) clockTimer.stop();
        if (link != null) link.close();
        link = newLink;
        loopback = newLoopback;
        game = new Game(link);
        game.addListener(this);
        linkLabel.setText("Connected: " + link.describe());
        testPanel.setVisible(loopback != null);
        enableControls(true);
        logArea.setText("");
        clockTimer = new javax.swing.Timer(100, e -> game.tick());
        clockTimer.start();
        refresh(game);
        revalidate();
    }

    private void enableControls(boolean on) {
        startButton.setEnabled(on);
        stopButton.setEnabled(on);
    }

    // ------------------------------------------------------------ observer
    @Override
    public void onUpdate(Game g) {
        SwingUtilities.invokeLater(() -> refresh(g));
    }

    @Override
    public void onLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /** Copy the game's numbers onto the screen. */
    void refresh(Game g) {
        stateLabel.setText(g.stateName());
        timeLabel.setText(String.valueOf(g.secondsLeft()));
        scoreLabel.setText(String.valueOf(g.score()));
        checkLabel.setText("CP1 " + (g.checkpoint1() ? "reached" : "-")
                + "   CP2 " + (g.checkpoint2() ? "reached" : "-"));
        if (!g.isRunning()) trapLabel.setText("idle");
        else trapLabel.setText(g.trapReady() ? "READY" : "cooldown " + g.cooldownSecondsLeft() + " s");
        ledLabel.setText(g.led().toString());
        darkLabel.setText(g.darkEntries() + " time(s) entered");
        dropLabel.setText(String.valueOf(g.drops()));
        mazeLabel.setText(g.mazeSeen() ? "maze: online" : "maze: no messages yet");
        remoteLabel.setText(g.remoteSeen() ? "remote: online" : "remote: no messages yet");
        tiltView.set(g.tiltX(), g.tiltY());
    }

    // ------------------------------------------------------------ tilt picture
    /** A square that shows where the maze is tilted: the dot moves with the board. */
    private static class TiltView extends JComponent {
        private int x, y;

        void set(int x, int y) {
            this.x = x;
            this.y = y;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 20;
            int left = (getWidth() - size) / 2;
            int top = (getHeight() - size) / 2;
            g.setColor(new Color(235, 235, 235));
            g.fillRect(left, top, size, size);
            g.setColor(Color.GRAY);
            g.drawRect(left, top, size, size);
            g.drawLine(left + size / 2, top, left + size / 2, top + size);
            g.drawLine(left, top + size / 2, left + size, top + size / 2);
            int cx = left + size / 2 + x * (size / 2) / 45;
            int cy = top + size / 2 - y * (size / 2) / 45;     // forward (y up) moves the dot up
            g.setColor(new Color(30, 90, 200));
            g.fillOval(cx - 9, cy - 9, 18, 18);
            g.setColor(Color.DARK_GRAY);
            g.drawString("x " + x + "   y " + y, left + 4, top + size - 4);
            g.dispose();
        }
    }
}
