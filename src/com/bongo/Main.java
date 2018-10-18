package com.bongo;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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
    private static Boolean seekselftrigger = false;
    private static String lenstr;
    private static JSlider seekslider = new JSlider(0,0);
    private static JLabel timeLabel;

    /** @noinspection InfiniteLoopStatement*/
    private static final Thread renderThread = new Thread(() -> {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice d = ge.getDefaultScreenDevice();
        int refreshRate = (d.getDisplayMode().getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) ?
                60 : d.getDisplayMode().getRefreshRate();
        long ms = (long) ((1. / ((double) refreshRate)) * 1000);
        while (true){
            try {
                if(Main.parser.sequencer.getMicrosecondPosition()==Main.parser.sequencer.getMicrosecondLength()){
                    Main.finish();
                }
                Main.timeLabel.setText(String.format("%02d:%02d/%s",
                        TimeUnit.MICROSECONDS.toMinutes(Main.parser.sequencer.getMicrosecondPosition()),
                        TimeUnit.MICROSECONDS.toSeconds(Main.parser.sequencer.getMicrosecondPosition()) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MICROSECONDS.toMinutes(Main.parser.sequencer.getMicrosecondPosition()))
                        ,Main.lenstr));
                Main.seekslider.setValue((int)Main.parser.sequencer.getMicrosecondPosition());
                Main.seekselftrigger=true;
                Thread.sleep(ms);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(window, e.getMessage(), "Rendering thread error", JOptionPane.ERROR_MESSAGE);
            }
            if (!parser.sequencer.isRunning()) continue;
            window.repaint();
        }
    });

    private static void togglePlayPause() {
        if (parser.sequencer.isRunning()) {
            parser.sequencer.stop();
            pauseButton.setText("PLAY");
        } else {
            parser.sequencer.start();
            pauseButton.setText("PAUSE");
        }
    }

    private static void finish() {
        pauseButton.setText("RESTART");
        pauseButton.setActionCommand("restart");
        parser.sequencer.stop();
        bongos = new Renderer.Bongo[16];
        window.repaint();
        Integer ok = JOptionPane.showOptionDialog(window,"BongoCat MIDI Player\nhttps://github.com/marios8543/BongoMidi","Thanks for playing!",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,new ImageIcon(Objects.requireNonNull(Renderer.load_asset("bongo.png"))),null,null);
        if(ok==JOptionPane.OK_OPTION){
            restart();
        }
        else {
            System.exit(0);
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
    }

    private static MidiParser init_player() throws Exception{
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MIDI Files", "mid", "midi");
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        } else {
            return null;
        }
        MidiParser private_parser = new MidiParser(file);
        lenstr = String.format("%02d:%02d",
                TimeUnit.MICROSECONDS.toMinutes(private_parser.sequencer.getMicrosecondLength()),
                TimeUnit.MICROSECONDS.toSeconds(private_parser.sequencer.getMicrosecondLength()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MICROSECONDS.toMinutes(private_parser.sequencer.getMicrosecondLength())));
        seekslider.setMaximum((int)private_parser.sequencer.getMicrosecondLength());
        return private_parser;
    }

    public static void main(String[] args) throws Exception {
        Renderer.build_coords();

        parser = init_player();
        if (parser == null) {
            System.exit(0);
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBackground(Color.WHITE);

        timeLabel = new JLabel("00:00");
        pauseButton = new JButton("PAUSE");
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

        JButton openButton = new JButton("Open file");
        openButton.addActionListener(e -> {
            Boolean isplaying = parser.sequencer.isRunning();
            if(isplaying){
                togglePlayPause();
            }
            try {
                MidiParser private_parser = init_player();
                if (private_parser == null) {
                    if(isplaying){
                        togglePlayPause();
                    }
                } else {
                    parser = private_parser;
                    restart();
                }
            } catch (Exception ee) {
                JOptionPane.showMessageDialog(window, ee.getMessage(), "File open error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JCheckBox loop = new JCheckBox("Loop");
        loop.addActionListener(e -> {
            JCheckBox chk = (JCheckBox) e.getSource();
            if (chk.isSelected()) {
                parser.sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            } else {
                parser.sequencer.setLoopCount(0);
            }
        });

        JLabel speed = new JLabel("Speed:");
        JSlider speedSlider = new JSlider(33, 300);
        speedSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            parser.sequencer.setTempoInBPM((float) source.getValue());
        });

        seekslider = new JSlider(0, (int) parser.sequencer.getMicrosecondLength());
        seekslider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            if (seekselftrigger) {
                seekselftrigger = false;
                return;
            }
            parser.sequencer.setMicrosecondPosition(source.getValue());
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


        Canvas c = new Canvas();
        c.setLocation(new Point(0, 0));

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setBounds(30, 30, 1350, 800);
        window.setName("Bongo Cat MIDI Player - " + file.getName());
        window.setTitle("Bongo Cat MIDI Player - " + file.getName());
        window.getContentPane().setLayout(new BoxLayout(window.getContentPane(), BoxLayout.PAGE_AXIS));
        window.getContentPane().add(c);
        window.getContentPane().add(buttonPanel, BorderLayout.CENTER);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0, 5))); // top padding to button
        window.getContentPane().setBackground(Color.WHITE);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0, 5))); // bottom padding
        window.setVisible(true);
        renderThread.start();
        parser.sequencer.start();
    }
}
