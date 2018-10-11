package com.bongo;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

class Canvas extends JComponent {
    public void paintComponent(Graphics g){
        g.clearRect(0, 0, Main.window.getWidth(), Main.window.getHeight());
        for(int i=0;i<Main.bongos.length;i++){
            Renderer.Bongo bongo = Main.bongos[i];
            if(bongo!=null){
                if (Main.seconds - 1 > bongo.lastSecondValue) {
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
    static JFrame window = new JFrame();
    private static boolean playing = true;
    private static MidiParser parser;
    private static JButton pauseButton;
    private static JLabel timeLabel;
    static int seconds = 0;
    private static Thread timeThread;

    private static void togglePlayPause() {
        if (playing) {
            parser.sequencer.stop();
            pauseButton.setText("PLAY");
            timeThread.interrupt();
        } else {
            parser.sequencer.start();
            pauseButton.setText("PAUSE");
            timeThread = generateTimeThread();
            timeThread.start();
        }
        playing = !playing;
    }

    private static void incrementSeconds() {
        seconds++;
        timeLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        if (!parser.sequencer.isRunning()) {
            finish();
        }
    }

    private static void finish() {
        pauseButton.setText("RESTART");
        pauseButton.setActionCommand("restart");
        parser.sequencer.stop();
        playing = false;
        bongos = new Renderer.Bongo[16];
        window.repaint();
        timeThread.interrupt();
    }

    private static void restart() {
        parser.sequencer.setTickPosition(0);
        seconds = 0;
        togglePlayPause();
        pauseButton.setActionCommand("toggle");
    }

    private static Thread generateTimeThread() {
        return new Thread(() -> {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                            incrementSeconds();
                        } catch (Exception e) {
                            break;
                        }
                    }
                });
    }

    public static void main(String[] args) throws Exception {
        Renderer.build_coords();
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MIDI Files", "mid", "midi");
        chooser.setFileFilter(filter);
        File file = null;
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        } else {
            System.exit(0);
        }
        parser = new MidiParser(file);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBackground(Color.WHITE);

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

        timeLabel = new JLabel("00:00");

        buttonPanel.add(pauseButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
        buttonPanel.add(timeLabel);

        Canvas c = new Canvas();
        c.setLocation(new Point(0, 0));

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setBounds(30, 30, 1350, 800);
        window.setName("Bongo Cat MIDI Player - " + file.getName());
        window.setTitle("Bongo Cat MIDI Player - " + file.getName());
        window.getContentPane().setLayout(new BoxLayout(window.getContentPane(), BoxLayout.PAGE_AXIS));
        window.getContentPane().add(c);
        window.getContentPane().add(buttonPanel, BorderLayout.CENTER);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0,5))); // top padding to button
        window.getContentPane().setBackground(Color.WHITE);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0,5))); // bottom padding
        window.setVisible(true);
        timeThread = generateTimeThread();
        timeThread.start();
        parser.sequencer.start();
    }
}
