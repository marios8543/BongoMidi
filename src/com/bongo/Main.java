package com.bongo;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

class Canvas extends JComponent {
    public void paintComponent(Graphics g){
        g.clearRect(0, 0, Main.window.getWidth(), Main.window.getHeight());
        for(int i=0;i<Main.bongos.length;i++){
            Renderer.Bongo bongo = Main.bongos[i];
            if(bongo!=null){
                if (TimeUnit.MILLISECONDS.toSeconds(Instant.now().toEpochMilli()) - 1 > bongo.lastSecondValue) {
                    Main.bongos[i] = null;
                } else {
                    g.drawImage(bongo.note.cpatch.getAsset(), bongo.x, bongo.y, this);
                    g.drawImage(Renderer.get_lhand(bongo.l_hand), bongo.x, bongo.y, this);
                    g.drawImage(Renderer.get_rhand(bongo.r_hand), bongo.x, bongo.y, this);
                }
            }
        }
    }
}

class Main{
    static Renderer.Bongo[] bongos = new Renderer.Bongo[16];
    static final JFrame window = new JFrame();
    private static MidiParser parser;
    private static File file = null;
    private static JButton pauseButton;
    private static boolean seekselftrigger = false;
    private static boolean finishTrigger = true;
    private static String lenstr;
    private static JSlider seekslider = new JSlider(0,0);
    private static JLabel timeLabel;

    private static final Thread renderThread = new Thread(() -> {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice d = ge.getDefaultScreenDevice();
        int refreshRate = (d.getDisplayMode().getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) ?
                60 : d.getDisplayMode().getRefreshRate();
        long ms = (long) ((1. / ((double) refreshRate)) * 1000);
        while (true){
            try {
                Main.timeLabel.setText(String.format("%02d:%02d.%03d/%s",
                        TimeUnit.MICROSECONDS.toMinutes(Main.parser.sequencer.getMicrosecondPosition()),
                        TimeUnit.MICROSECONDS.toSeconds(Main.parser.sequencer.getMicrosecondPosition()) % 60,
                        Main.parser.sequencer.getMicrosecondPosition() % 1000,
                        Main.lenstr));
                Main.seekslider.setValue((int)Main.parser.sequencer.getMicrosecondPosition());
                Main.seekselftrigger=true;
                Thread.sleep(ms);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(window, e.getMessage(), "Rendering thread error", JOptionPane.ERROR_MESSAGE);
                break;
            }
            if(Main.parser.sequencer.getMicrosecondPosition()==Main.parser.sequencer.getMicrosecondLength()){
                if (finishTrigger) Main.finish();
            }
            if (!parser.sequencer.isRunning()) continue;
            window.repaint();
        }
    });

    private static void togglePlayPause() {
        if (parser.sequencer.isRunning()) {
            parser.sequencer.stop();
            pauseButton.setText("Play");
        } else {
            parser.sequencer.start();
            pauseButton.setText("Pause");
        }
    }

    private static void finish() {
        pauseButton.setText("Restart");
        pauseButton.setActionCommand("restart");
        parser.sequencer.stop();
        bongos = new Renderer.Bongo[16];
        window.repaint();
        finishTrigger = false;
        int ok = JOptionPane.showOptionDialog(window,
                "BongoCat MIDI Player\nhttps://github.com/marios8543/BongoMidi\nRestart?",
                "Thanks for playing!",JOptionPane.YES_NO_OPTION,JOptionPane.PLAIN_MESSAGE,
                new ImageIcon(Objects.requireNonNull(Renderer.load_asset("bongo.png"))),
                null,null);
        if(ok==JOptionPane.YES_OPTION){
            restart();
        }
    }

    private static void restart() {
        parser.sequencer.setMicrosecondPosition(0);
        bongos = new Renderer.Bongo[16];
        window.repaint();
        window.setName("Bongo Cat MIDI Player - " + file.getName());
        window.setTitle("Bongo Cat MIDI Player - " + file.getName());
        togglePlayPause();
        pauseButton.setActionCommand("toggle");
        finishTrigger = true;
    }

    private static MidiParser init_player(){
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MIDI Files", "mid", "midi");
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        } else {
            return null;
        }
        if (!file.exists()) {
            int result = JOptionPane.showConfirmDialog(window,
                    file.getAbsolutePath() + " does not exist. Try again?",
                    "File not found", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                return init_player();
            } else {
                return null;
            }
        }
        if (!file.canRead()) {
            JOptionPane.showMessageDialog(window,
                    file.getAbsolutePath() + " cannot be read. Please fix your permissions and try again.",
                    "File cannot be read", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        MidiParser private_parser;
        try {
            private_parser = new MidiParser(file);
        } catch (MidiUnavailableException m) {
            JOptionPane.showMessageDialog(window, m.getMessage(), "MIDI Unavailable", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (InvalidMidiDataException i) {
            JOptionPane.showMessageDialog(window, i.getMessage(), "Invalid MIDI Data", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(window, e.getMessage(), "IO Exception", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        window.setName("Bongo Cat MIDI Player - " + file.getName());
        window.setTitle("Bongo Cat MIDI Player - " + file.getName());
        lenstr = String.format("%02d:%02d.%03d",
                TimeUnit.MICROSECONDS.toMinutes(private_parser.sequencer.getMicrosecondLength()),
                TimeUnit.MICROSECONDS.toSeconds(private_parser.sequencer.getMicrosecondLength()) % 60,
                private_parser.sequencer.getMicrosecondLength() % 1000);
        seekslider.setMaximum((int)private_parser.sequencer.getMicrosecondLength());
        return private_parser;
    }

    public static void main(String[] args) {
        Renderer.build_coords();

        parser = init_player();
        if (parser == null) {
            System.exit(0);
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBackground(Color.WHITE);

        timeLabel = new JLabel("00:00");
        pauseButton = new JButton("Pause");
        pauseButton.setActionCommand("toggle");
        pauseButton.setMnemonic(KeyEvent.VK_P);
        pauseButton.addActionListener((ActionEvent e) -> {
            switch (e.getActionCommand()) {
                case "toggle": {
                    togglePlayPause();
                    break;
                }
                case "restart": {
                    restart();
                    break;
                }
                default: {
                    System.err.println("Invalid action: " + e.getActionCommand());
                    break;
                }
            }
        });
        pauseButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox loop = new JCheckBox("Loop");
        loop.addActionListener(e -> {
            JCheckBox chk = (JCheckBox) e.getSource();
            if (chk.isSelected()) {
                parser.sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            } else {
                parser.sequencer.setLoopCount(0);
            }
        });
        loop.setBackground(Color.WHITE);

        JLabel speed = new JLabel("Speed:");
        JSlider speedSlider = new JSlider(33, 300);
        speedSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            parser.sequencer.setTempoInBPM((float) source.getValue());
        });
        speedSlider.setBackground(Color.WHITE);

        seekslider = new JSlider(0, (int) parser.sequencer.getMicrosecondLength());
        seekslider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            if (seekselftrigger) {
                seekselftrigger = false;
                return;
            }
            parser.sequencer.setMicrosecondPosition(source.getValue());
            parser.sequencer.setTempoInBPM(speedSlider.getValue());
        });
        seekslider.setBackground(Color.WHITE);

        JButton openButton = new JButton("Open file");
        openButton.addActionListener(e -> {
            try {
                MidiParser private_parser = init_player();
                if (private_parser != null) {
                    parser.sequencer.stop();
                    bongos = new Renderer.Bongo[16];
                    parser = private_parser;
                    parser.sequencer.start();

                    speedSlider.setValue((int) parser.sequencer.getTempoInBPM());
                }
            } catch (Exception ee) {
                JOptionPane.showMessageDialog(window, ee.getMessage(), "File open error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton forkButton = new JButton("Fork me on GitHub!");
        forkButton.addActionListener(e -> {
            switch (e.getActionCommand()) {
                case "fork": {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(new URI("https://github.com/marios8543/BongoMidi"));
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(window, ex.getMessage(), "Error opening GitHub page",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
        forkButton.setActionCommand("fork");

        openButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
        buttonPanel.add(openButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(pauseButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(forkButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(loop);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(timeLabel);
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        buttonPanel.add(seekslider);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(speed);
        buttonPanel.add(speedSlider);


        Canvas c = new Canvas();
        c.setLocation(new Point(0, 0));

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setBounds(30, 30, 1350, 850);
        window.getContentPane().setLayout(new BoxLayout(window.getContentPane(), BoxLayout.PAGE_AXIS));
        window.getContentPane().add(c);
        window.getContentPane().add(buttonPanel, BorderLayout.CENTER);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0, 5))); // top padding to button
        window.getContentPane().setBackground(Color.WHITE);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0, 5))); // bottom padding
        window.setVisible(true);
        renderThread.start();
        parser.sequencer.start();

        speedSlider.setValue((int) parser.sequencer.getTempoInBPM());
    }
}
