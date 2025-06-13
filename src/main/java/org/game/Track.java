package org.game;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Track {
    private String name;
    private BufferedImage trackImage;

    public Track(String name, String imagePath) {
        this.name = name;
        try {
            this.trackImage = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public Color getColorAt(double x, double y) {
//        int ix = (int) x;
//        int iy = (int) y;
//        if (ix >= 0 && ix < trackImage.getWidth() && iy >= 0 && iy < trackImage.getHeight()) {
//            return new Color(trackImage.getRGB(ix, iy));
//        }
//        return Color.BLACK;
//    }

    public void paintComponent(Graphics2D g2d) {
        if (trackImage != null) {
            g2d.drawImage(trackImage, 0, 0, trackImage.getWidth() * 2, trackImage.getHeight() * 2, null);
        }
    }

    public String getName() {
        return name;
    }
}