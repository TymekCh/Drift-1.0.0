package org.game;

import javax.swing.*;
import java.awt.*;

public class AnimatedGifPanel extends JPanel {
    private final ImageIcon gif;

    public AnimatedGifPanel(String gifPath) {
        this.gif = new ImageIcon(gifPath);
        setLayout(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Image img = gif.getImage();
        // Rysuje gif rozciągnięty na cały panel
        g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
    }
}

