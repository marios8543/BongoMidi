package com.bongo;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

class Canvas extends JComponent {
    public void paintComponent(Graphics g){
        g.clearRect(0, 0, Main.window.getWidth(), Main.window.getHeight());
        for(int i=0;i<Main.bongos.length;i++){
            Renderer.Bongo bongo = Main.bongos[i];
            if(bongo!=null){
                if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - 1 > bongo.getLastSecondValue()) {
                    Main.bongos[i] = null;
                } else {
                    g.drawImage(bongo.getNote().getCpatch().getAsset(), bongo.getX(), bongo.getY(), this);
                    g.drawImage(Renderer.get_lhand(bongo.isL_hand()), bongo.getX(), bongo.getY(), this);
                    g.drawImage(Renderer.get_rhand(bongo.isR_hand()), bongo.getX(), bongo.getY(), this);
                }
            }
        }
    }
}