package com.bongo;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

class MyCanvas extends JComponent {
    public void paintComponent(Graphics g){
        g.clearRect(0, 0, Main.window.getWidth(), Main.window.getHeight());
        for(int i=0;i<Main.bongos.length;i++){
            Renderer.Bongo bongo = Main.bongos[i];
            if(bongo!=null){
                g.drawImage(bongo.note.cpatch.getAsset(),bongo.x,bongo.y,this);
                g.drawImage(Renderer.get_lhand(bongo.l_hand),bongo.x,bongo.y,this);
                g.drawImage(Renderer.get_rhand(bongo.r_hand),bongo.x,bongo.y,this);
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

    private static void togglePlayPause() {
        if (playing) {
            parser.sequencer.stop();
            pauseButton.setText("PLAY");
        } else {
            parser.sequencer.start();
            pauseButton.setText("PAUSE");
        }
        playing = !playing;
    }

    public static void main(String[] args) throws Exception {
        Renderer.build_coords();
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MIDI Files", "mid", "midi");
        chooser.setFileFilter(filter);
        File file = null;
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        }
        parser = new MidiParser(file);

        pauseButton = new JButton("PAUSE");
        pauseButton.setActionCommand("toggle");
        pauseButton.setMnemonic(KeyEvent.VK_P);
        pauseButton.addActionListener((ActionEvent e) -> {
            switch (e.getActionCommand()) {
                case "toggle": {
                    togglePlayPause();
                    break;
                }
                default: {
                    System.err.println("Invalid action: " + e.getActionCommand());
                    break;
                }
            }
        });
        pauseButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setBounds(30, 30, 1350, 800);
        window.setName("Bongo Cat MIDI Player");
        window.setTitle("Bongo Cat MIDI Player");
        window.getContentPane().setLayout(new BoxLayout(window.getContentPane(), BoxLayout.PAGE_AXIS));
        window.getContentPane().add(new MyCanvas());
        window.getContentPane().add(pauseButton, BorderLayout.CENTER);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0,5))); // top padding to button
        window.getContentPane().setBackground(Color.WHITE);
        window.getContentPane().add(Box.createRigidArea(new Dimension(0,5))); // bottom padding
        window.setVisible(true);
        Thread renderThread = new Thread(() -> {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice d = ge.getDefaultScreenDevice();
            int refreshRate = (d.getDisplayMode().getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) ?
                    60 : d.getDisplayMode().getRefreshRate();
            long ms = (long) ((1. / ((double) refreshRate)) * 1000);
            while (true){
                try {
                    Thread.sleep(ms);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(window, "Renderer thread interrupted", "Error", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (!playing) continue;
                window.repaint();
            }
        });
        renderThread.start();
        parser.sequencer.start();
    }
}
