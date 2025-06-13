package org.game;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.plaf.metal.MetalSliderUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private Image carImage;
    private double x = 1800, y = 700;
    private double angle = 270;
    private double speed = 0;
    private double driftAngle = 0;
    private double lastSpeed = 0;

    private boolean turboExhaustActive = false;
    private long turboExhaustStart = 0;
    private final long exhaustDuration = 300; // ms
    private Image turboFire;

    private boolean turboActive = false;
    private long lastTurboTime = -20000;

    private final int carWidth = 120;
    private final int carHeight = 90;

    private boolean up, down, left, right, handbrake;
    private boolean engineStarted = false;
    private boolean paused = false;

    private final LinkedList<TireMark> tireMarks = new LinkedList<>();
    private final HashMap<String, Clip> activeSounds = new HashMap<>();
    private Clip backgroundMusicClip;

    private JPanel dimBackground;
    private JPanel pauseMenuPanel;
    private JPanel optionsPanel;

    private Image grassTexture;


    private Track currentTrack;
        //Wygląd, tekstury i kolory
    public GamePanel() {
        setBackground(new Color(34, 139, 34));
        setFocusable(true);
        addKeyListener(this);
        setLayout(null);
        grassTexture = new ImageIcon("src/resources/images/grass_tile.png").getImage();
        carImage = new ImageIcon("src/resources/images/car.png").getImage();
        SwingUtilities.invokeLater(this::initPauseMenu);

        // Przypisz tor
        currentTrack = new Track("Tor 1", "src/resources/tracks/track1.png");
        turboFire = new ImageIcon("src/resources/images/turbo_fire.png").getImage();

        timer = new Timer(16, this); //Timer pozwala grze na płynniejsze chodzenie okolo 60 FPS
        timer.start();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (dimBackground != null) {
            dimBackground.setBounds(0, 0, getWidth(), getHeight());
            pauseMenuPanel.setBounds((getWidth() - 300) / 2, (getHeight() - 300) / 2, 300, 300);
        }
    }
        //Menu pauzy
    private void initPauseMenu() {
        dimBackground = new JPanel();
        dimBackground.setBounds(0, 0, getWidth(), getHeight());
        dimBackground.setBackground(new Color(0, 0, 0, 150));
        dimBackground.setVisible(false);
        dimBackground.setLayout(null);
        add(dimBackground);

        pauseMenuPanel = new JPanel();
        pauseMenuPanel.setLayout(new BoxLayout(pauseMenuPanel, BoxLayout.Y_AXIS));
        pauseMenuPanel.setBounds((getWidth() - 300) / 2, (getHeight() - 300) / 2, 300, 300);
        pauseMenuPanel.setOpaque(false);
        pauseMenuPanel.setVisible(false);

        JButton resumeButton = createStyledButton("Kontynuuj");
        JButton optionsButton = createStyledButton("Opcje");
        JButton toMenuButton = createStyledButton("Wyjdź do menu");
        JButton exitButton = createStyledButton("Wyjdź z gry");

        resumeButton.addActionListener(e -> resumeGame());
        optionsButton.addActionListener(e -> optionsPanel.setVisible(!optionsPanel.isVisible()));
        toMenuButton.addActionListener(e -> {
            stopAllAudio();
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            topFrame.dispose();
            SwingUtilities.invokeLater(() -> new MainMenu());
        });
        exitButton.addActionListener(e -> System.exit(0));

        for (JButton b : new JButton[]{resumeButton, optionsButton, toMenuButton, exitButton}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(200, 40));
            pauseMenuPanel.add(Box.createVerticalStrut(10));
            pauseMenuPanel.add(b);
        }

        optionsPanel = new JPanel();
        optionsPanel.setOpaque(false);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setMaximumSize(new Dimension(250, 100));

        JLabel volumeLabel = new JLabel("Głośność: " + (int)(OptionsMenu.getVolume() * 100) + "%");
        volumeLabel.setForeground(Color.WHITE);
        volumeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider volumeSlider = new JSlider(0, 100, (int)(OptionsMenu.getVolume() * 100));
        volumeSlider.addChangeListener(e -> {
            float newVolume = volumeSlider.getValue() / 100f;
            volumeLabel.setText("Głośność: " + volumeSlider.getValue() + "%");
            OptionsMenu.setVolume(newVolume);
            setVolume(backgroundMusicClip, newVolume); // dodano
        });

        volumeSlider.setUI(new MetalSliderUI() {
            @Override
            public void paintThumb(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
            }

            @Override
            public void paintTrack(Graphics g) {
                g.setColor(new Color(30, 30, 30));
                g.fillRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height);
            }
        });

        optionsPanel.add(volumeLabel);
        optionsPanel.add(volumeSlider);
        optionsPanel.setVisible(false);
        pauseMenuPanel.add(optionsPanel);

        dimBackground.add(pauseMenuPanel);
    }


    private void pauseGame() {
        paused = true;
        timer.stop();
        dimBackground.setVisible(true);
        pauseMenuPanel.setVisible(true);
    }

    private void resumeGame() {
        paused = false;
        timer.start();
        pauseMenuPanel.setVisible(false);
        optionsPanel.setVisible(false);
        dimBackground.setVisible(false);
    }
        //Wygląd przycisków
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBackground(new Color(30, 30, 30));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setMaximumSize(new Dimension(200, 40));
        return button;
    }
    //Renderowanie gry
    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        int panelW = getWidth();
        int panelH = getHeight();

        int camX = (int) (x - panelW / 2);
        int camY = (int) (y - panelH / 2);

        for (int i = 0; i < getWidth(); i += grassTexture.getWidth(null)) {
            for (int j = 0; j < getHeight(); j += grassTexture.getHeight(null)) {
                g.drawImage(grassTexture, i, j, this);
            }
        }
        g2d.translate(-camX, -camY); // Przesunięcie kamery

        // Tor
        if (currentTrack != null) {
            currentTrack.paintComponent(g2d);
        }

        // Ślady opon
        g2d.setColor(Color.DARK_GRAY);
        for (TireMark mark : tireMarks) {
            g2d.fillOval((int) mark.x, (int) mark.y, 3, 3);
        }

        drawCar(g2d);

        // Wskaźnik turbo
        drawTurboIndicator(g2d, camX, camY);
    }

    private void drawCar(Graphics2D g2d) {
//        if (turboExhaustActive && System.currentTimeMillis() - turboExhaustStart < exhaustDuration) {
//            int flameWidth = 40;
//            int flameHeight = 20;
//            g2d.drawImage(turboFire, -carWidth / 2 - flameWidth + 5, -flameHeight / 2, flameWidth, flameHeight, null);
//        } else {
//            turboExhaustActive = false;
//        }
        g2d.translate(x, y);
        g2d.rotate(Math.toRadians(angle));

// Skaluje do carWidth i carHeight
        g2d.drawImage(carImage, -carWidth / 2, -carHeight / 2, carWidth, carHeight, null);

        g2d.rotate(-Math.toRadians(angle));
        g2d.translate(-x, -y);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused) {
            updatePhysics();
            repaint();
        }
    }
        //Cała fizyka gry, oblicza predkosc, przyspieszenie i turbo
    private void updatePhysics() {
        if (!engineStarted) {
            engineStarted = true;
            playSound("engine_start", false);
            playBackgroundMusic();
            return;
        }
        // Color pixelColor = currentTrack.getColorAt(x, y);

//        if (pixelColor.equals(Color.RED)) {
//            angle += 180;
//            speed = Math.abs(speed) * 0.7;
//        }
        boolean isIdle = Math.abs(speed) < 0.3;
        boolean isBraking = down && speed > 0.1;
        boolean isFast = Math.abs(speed) > 7.0;
        boolean isAccelerating = up && speed > lastSpeed;

        lastSpeed = speed;

        if (isIdle) {
            playExclusiveEngineSound("engine_idle");
        } else if (isBraking) {
            playExclusiveEngineSound("engine_brake");
        } else if (isFast) {
            playExclusiveEngineSound("engine_fast");
        } else if (isAccelerating) {
            playExclusiveEngineSound("engine_accelerating");
        }

        double acceleration = handbrake ? 0 : 0.4;
        double friction = handbrake ? 0.2 : 0.05;
        double baseSpeed = 12.5;
        double maxSpeed = turboActive ? baseSpeed * 1.5 : baseSpeed;
        double turnSpeed = handbrake ? 6.0 : 3.0;
        double driftFactor = handbrake ? 0.75 : 0.92;

        if (up) speed += acceleration;
        else if (down) speed -= acceleration;
        else speed *= (1 - friction);

        speed = Math.max(-maxSpeed, Math.min(maxSpeed, speed));

        if (left) angle -= turnSpeed * (speed / maxSpeed);
        if (right) angle += turnSpeed * (speed / maxSpeed);

        driftAngle = driftAngle * driftFactor + angle * (1 - driftFactor);

        double rad = Math.toRadians(driftAngle); //Efekt driftu
        x += speed * Math.cos(rad);
        y += speed * Math.sin(rad);

        if (Math.abs(angle - driftAngle) > 5 || handbrake) {
            tireMarks.add(new TireMark(x, y));
            if (tireMarks.size() > 300) {
                tireMarks.removeFirst();
            }
        }
        if (turboActive && System.currentTimeMillis() - lastTurboTime >= 7500) {
            turboActive = false;
        }
        if (!turboActive && System.currentTimeMillis() - lastTurboTime < 100) {
            turboExhaustActive = true;
            turboExhaustStart = System.currentTimeMillis();
        }
    }
    private void playExclusiveEngineSound(String name) {
        String[] engineSounds = {"engine_idle", "engine_accelerating", "engine_brake", "engine_fast"};

        // Stop all engine sounds except the one being requested
        for (String sound : engineSounds) {
            if (!sound.equals(name)) {
                stopSound(sound);
            }
        }

        // Only play if it's not already playing
        playSound(name, true);
    }

    private void stopSound(String name) {
        Clip clip = activeSounds.get(name);
        if (clip != null) {
            clip.stop();
            clip.close();
            activeSounds.remove(name);
        }
    }
    private void playSound(String name, boolean loop) {
        if (activeSounds.containsKey(name)) {
            Clip clip = activeSounds.get(name);
            if (clip != null && clip.isRunning()) return;
        }

        try {
            File file = new File("src/resources/sounds/" + name + ".wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.setFramePosition(0);
            clip.loop(loop ? Clip.LOOP_CONTINUOUSLY : 0);
            clip.start();
            activeSounds.put(name, clip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAllAudio() {
        activeSounds.values().forEach(clip -> {
            if (clip != null) {
                clip.stop();
                clip.close();
            }
        });
        activeSounds.clear();
        if (backgroundMusicClip != null) {
            backgroundMusicClip.stop();
            backgroundMusicClip.close();
        }
    }

    private void setVolume(Clip clip, float volume) {
        if (clip != null) {
            try {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log10(volume) * 20);
                gainControl.setValue(dB);
            } catch (Exception ignored) {}
        }
    }
        //Wskaznik turbo
    private void drawTurboIndicator(Graphics2D g2d, int camX, int camY) {
        long timeSinceLastTurbo = System.currentTimeMillis() - lastTurboTime;
        long cooldown = 20000;
        double progress = Math.min(1.0, timeSinceLastTurbo / (double) cooldown);

        int barWidth = 120;
        int barHeight = 15;
        int xPos = camX + getWidth() - barWidth - 20;
        int yPos = camY + getHeight() - barHeight - 20;

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(xPos, yPos, barWidth, barHeight);

        g2d.setColor(progress >= 1.0 ? Color.CYAN : Color.ORANGE);
        g2d.fillRect(xPos, yPos, (int)(barWidth * progress), barHeight);

        g2d.setColor(Color.WHITE);
        g2d.drawRect(xPos, yPos, barWidth, barHeight);
        g2d.drawString("Turbo", xPos + 5, yPos - 5);
    }

    private void playBackgroundMusic() {
        try {
            File file = new File("src/resources/sounds/background_music.wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            backgroundMusicClip = AudioSystem.getClip();
            backgroundMusicClip.open(audioStream);
            setVolume(backgroundMusicClip, OptionsMenu.getVolume()); // dodano
            backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusicClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class TireMark {
        double x, y;
        TireMark(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> up = true;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> down = true;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SPACE -> handbrake = true;
            case KeyEvent.VK_ESCAPE -> {
                if (!paused) pauseGame();
                else resumeGame();
            }
            case KeyEvent.VK_SHIFT -> {
                if (!turboActive && System.currentTimeMillis() - lastTurboTime >= 15000) {
                    turboActive = true;
                    lastTurboTime = System.currentTimeMillis();
                    playSound("burnout", false); // optional turbo sound
                }
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> up = false;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> down = false;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> right = false;
            case KeyEvent.VK_SPACE -> handbrake = false;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
}
