package com.bongo;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;



public class Main {
    static Renderer.Bongo[] bongos = new Renderer.Bongo[16];
    static final JFrame window = new JFrame();
    static final GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    public static MidiParser parser;
    private static File file = null;
    private static JButton pauseButton;
    private static boolean seekselftrigger = false;
    private static boolean finishTrigger = true;
    private static String lenstr;
    public static float initialBpm;
    public static JSlider speedSlider;
    private static JSlider seekslider = new JSlider(0, 0);
    private static JLabel timeLabel;

    private static final Thread renderThread = new Thread(new Runnable() {
        @Override
        public void run() {
            int refreshRate = (screen.getDisplayMode().getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) ?
                    60 : screen.getDisplayMode().getRefreshRate();
            long ms = (long) ((1. / ((double) refreshRate)) * 1000);
            while (true) {
                try {
                    Main.timeLabel.setText(String.format("%02d:%02d/%s",
                            TimeUnit.MICROSECONDS.toMinutes(Main.parser.sequencer.getMicrosecondPosition()),
                            TimeUnit.MICROSECONDS.toSeconds(Main.parser.sequencer.getMicrosecondPosition()) % 60,
                            Main.lenstr));
                    Main.seekslider.setValue((int) Main.parser.sequencer.getMicrosecondPosition());
                    Main.seekselftrigger = true;
                    Thread.sleep(ms);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(window, e.getMessage(), "Rendering thread error", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (Main.parser.sequencer.getLoopCount() != Sequencer.LOOP_CONTINUOUSLY) {
                    if (Main.parser.sequencer.getMicrosecondPosition() == Main.parser.sequencer.getMicrosecondLength()) {
                        if (finishTrigger) Main.finish();
                    }
                }
                if (Main.parser.sequencer.getTickPosition() < 0) {
                    Main.parser.sequencer.setTickPosition(0);
                }
                if (!parser.sequencer.isRunning()) continue;
                window.repaint();
            }
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
        JButton forkButton = new JButton("Fork me on GitHub!");
        forkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://github.com/marios8543/BongoMidi"));
                        } catch (Exception ex) {
                        Runtime runtime = Runtime.getRuntime();
                        try {
                            runtime.exec("xdg-open rhttps://github.com/marios8543/BongoMidi");
                            } catch (IOException ee) {
                            JOptionPane.showMessageDialog(window, ee.getMessage(), "Browser open error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
        pauseButton.setText("Restart");
        pauseButton.setActionCommand("restart");
        parser.sequencer.stop();
        bongos = new Renderer.Bongo[16];
        window.repaint();
        finishTrigger = false;
        Integer ok = JOptionPane.showOptionDialog(window, "BongoCat MIDI Player\nmarios8543\nDistributed under the MIT License\nhttps://github.com/marios8543/BongoMidi\n\nRestart?",
                "Thanks for playing!",JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, new FinishAnimation(), new Object[]{"Yes","No",forkButton},null);
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

    private static MidiParser init_player() {
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
        lenstr = String.format("%02d:%02d",
                TimeUnit.MICROSECONDS.toMinutes(private_parser.sequencer.getMicrosecondLength()),
                TimeUnit.MICROSECONDS.toSeconds(private_parser.sequencer.getMicrosecondLength()) % 60);
        seekslider.setMaximum((int) private_parser.sequencer.getMicrosecondLength());
        return private_parser;
    }

    public static void main(String[] args) {
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
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
            }
        });
        pauseButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JCheckBox loop = new JCheckBox("Loop");
        loop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox chk = (JCheckBox) e.getSource();
                parser.sequencer.setLoopCount(chk.isSelected() ?
                        Sequencer.LOOP_CONTINUOUSLY : 0);
            }
        });
        loop.setBackground(Color.WHITE);

        final JLabel speed = new JLabel("Speed:");
        speedSlider = new JSlider(33, 300);
        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                parser.sequencer.setTempoInBPM((float) source.getValue());
                speed.setText("Speed: (" + source.getValue() + " BPM)");
            }
        });
        speedSlider.setBackground(Color.WHITE);

        seekslider = new JSlider(0, (int) parser.sequencer.getMicrosecondLength());
        seekslider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (seekselftrigger) {
                    seekselftrigger = false;
                    return;
                }
                parser.sequencer.setMicrosecondPosition(source.getValue());
                parser.sequencer.setTempoInBPM(speedSlider.getValue());
            }
        });
        seekslider.setBackground(Color.WHITE);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                speedSlider.setValue((int) initialBpm);
            }
        });

        JButton openButton = new JButton("Open file");
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    MidiParser private_parser = init_player();
                    if (private_parser != null) {
                        parser.sequencer.stop();
                        bongos = new Renderer.Bongo[16];
                        parser = private_parser;

                        parser.sequencer.setLoopCount(loop.isSelected() ?
                                Sequencer.LOOP_CONTINUOUSLY : 0);

                        parser.sequencer.start();

                        speedSlider.setValue((int) parser.sequencer.getTempoInBPM());
                    }
                } catch (Exception ee) {
                    JOptionPane.showMessageDialog(window, ee.getMessage(), "File open error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        openButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        buttonPanel.add(openButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(pauseButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(loop);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(timeLabel);
        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        buttonPanel.add(seekslider);
        buttonPanel.add(Box.createRigidArea(new Dimension(30, 0)));
        buttonPanel.add(speed);
        buttonPanel.add(speedSlider);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(resetButton);


        Canvas c = new Canvas();
        c.setLocation(new Point(0, 0));

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setBounds(30, 30, screen.getDisplayMode().getWidth(), screen.getDisplayMode().getHeight());
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Renderer.build_coords(window.getWidth(),window.getHeight());
                super.componentResized(e);
            }
        });
        window.getContentPane().setLayout(new BoxLayout(window.getContentPane(), BoxLayout.PAGE_AXIS));
        window.getContentPane().add(c);
        window.getContentPane().add(buttonPanel, BorderLayout.CENTER);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0, 5))); // top padding to button
        window.getContentPane().setBackground(Color.WHITE);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0, 5))); // bottom padding
        window.setVisible(true);
        Renderer.build_coords(window.getWidth(),window.getHeight());
        renderThread.start();
        parser.sequencer.start();

        initialBpm = parser.sequencer.getTempoInBPM();
        speedSlider.setValue((int) initialBpm);
    }

}

