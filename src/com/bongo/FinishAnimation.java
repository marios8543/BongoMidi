package com.bongo;

import javax.swing.*;
import java.awt.*;

class FinishAnimation implements Icon {
    private Boolean lhand = true;
    private Boolean rhand = false;

    FinishAnimation() {
        final Thread renderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(Main.window, e.getMessage(), "Rendering thread error", JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                    if (component != null) component.repaint();
                }
            }
        });
        renderThread.start();
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (FinishAnimation.component == null) FinishAnimation.component = c;
        g.clearRect(0,0, c.getWidth(), c.getHeight());
        g.drawImage(Renderer.Instr_Categ.Bongo.getAsset(), x, y, c);
        g.drawImage(Renderer.get_lhand(lhand), x, y, c);
        g.drawImage(Renderer.get_rhand(rhand), x, y, c);
        lhand=!lhand;
        rhand=!rhand;
    }

    private static Component component;

    public int getIconWidth() {
        return 380;
    }

    public int getIconHeight() {
        return 264;
    }
}