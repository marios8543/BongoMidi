package com.bongo;
import java.awt.*;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

class MyCanvas extends JComponent {
    public void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D) g;
        for(Integer i=0;i<Main.bongos.length;i++){
            Renderer.Bongo bongo = Main.bongos[i];
            if(bongo!=null){
                g.drawImage(bongo.note.cpatch.getAsset(),bongo.x,bongo.y,this);
                g.drawImage(Renderer.get_lhand(bongo.l_hand),bongo.x,bongo.y,this);
                g.drawImage(Renderer.get_rhand(bongo.r_hand),bongo.x,bongo.y,this);
            }
        }
    }
}
class Main {

    public static Renderer.Bongo[] bongos = new Renderer.Bongo[16];
    public static JFrame window = new JFrame();
    static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws Exception {
        Renderer.build_coords();
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MIDI Files", "mid", "midi");
        chooser.setFileFilter(filter);
        File file = null;
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        }
        MidiParser parser = new MidiParser(file);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setBounds(30, 30, 1350, 800);
        window.setName("Bongo Cat MIDI Player");
        window.setVisible(true);
        window.getContentPane().add(new MyCanvas());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (true){
                    window.invalidate();
                    window.validate();
                    window.repaint();
                }
            }
        });
        parser.sequencer.start();
    }
}
