package org.game;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.plaf.metal.MetalSliderUI;
import java.awt.*;
import java.io.File;

public class MainMenu extends JFrame {
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private JPanel buttonPanel;
    private JPanel optionsPanel;
    private boolean gameStarted = false; // 🔒 zabezpieczenie

    public MainMenu() {
        setTitle("Drift");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension res = OptionsMenu.getResolution();
        setSize(res);
        setLocationRelativeTo(null);

        AnimatedGifPanel backgroundPanel = new AnimatedGifPanel("src/resources/images/background.gif");
        setContentPane(backgroundPanel);
        backgroundPanel.setLayout(null);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        int panelHeight = 350;
        leftPanel.setBounds(50, (res.height - panelHeight) / 2, 250, panelHeight);

        ImageIcon logoIcon = new ImageIcon("src/resources/images/drift_logo.png");
        Image scaledLogo = logoIcon.getImage().getScaledInstance(300, 150, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new Thread(() -> playSoundOnce("src/resources/sounds/burnout.wav")).start();
            }
        });

        leftPanel.add(logoLabel);
        leftPanel.add(Box.createVerticalStrut(30));

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);

        JButton startButton = createStyledButton("START");
        JButton optionsButton = createStyledButton("OPCJE");
        JButton exitButton = createStyledButton("WYJDŹ");

        buttonPanel.add(startButton);
        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(optionsButton);
        buttonPanel.add(Box.createVerticalStrut(20));
        buttonPanel.add(exitButton);

        leftPanel.add(buttonPanel);
        backgroundPanel.add(leftPanel);

        optionsPanel = new JPanel();
        optionsPanel.setOpaque(false);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBounds(350, 100, 300, 150);
        optionsPanel.setVisible(false);

        JLabel volumeLabel = new JLabel("Głośność: " + (int)(OptionsMenu.getVolume() * 100) + "%");
        volumeLabel.setForeground(Color.WHITE);

        JSlider volumeSlider = new JSlider(0, 100, (int)(OptionsMenu.getVolume() * 100));
        volumeSlider.addChangeListener(e -> {
            float newVolume = volumeSlider.getValue() / 100f;
            volumeLabel.setText("Głośność: " + volumeSlider.getValue() + "%");
            OptionsMenu.setVolume(newVolume);
            audioPlayer.setVolume(newVolume);
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
        backgroundPanel.add(optionsPanel);

        audioPlayer.playLoop("src/resources/sounds/menu_music.wav");

        startButton.addActionListener(e -> {
            if (!gameStarted) { //  zabezpieczenie
                gameStarted = true;
                audioPlayer.stop();
                showLoadingScreen();
            }
        });

        optionsButton.addActionListener(e -> optionsPanel.setVisible(!optionsPanel.isVisible()));
        exitButton.addActionListener(e -> {
            audioPlayer.stop();
            System.exit(0);
        });

        animateButtonsIn();
        setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setBackground(new Color(30, 30, 30));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setMaximumSize(new Dimension(150, 40));
        return button;
    }

    private void animateButtonsIn() {
        Point start = new Point(-250, buttonPanel.getY());
        Point end = buttonPanel.getLocation();
        buttonPanel.setLocation(start);

        Timer timer = new Timer(10, null);
        timer.addActionListener(e -> {
            Point current = buttonPanel.getLocation();
            int dx = (end.x - current.x) / 5;
            if (Math.abs(current.x - end.x) <= 2) {
                buttonPanel.setLocation(end);
                timer.stop();
            } else {
                buttonPanel.setLocation(current.x + dx, current.y);
            }
        });
        timer.start();
    }

    private void playSoundOnce(String filePath) {
        try {
            File soundFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float volume = OptionsMenu.getVolume();
            float dB = (float) (Math.log10(Math.max(volume, 0.0001)) * 20.0);
            gain.setValue(dB);
            clip.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showLoadingScreen() {
        getContentPane().removeAll();
        repaint();

        JLabel loadingLabel = new JLabel("Ładowanie...");
        loadingLabel.setFont(new Font("Arial", Font.BOLD, 36));
        loadingLabel.setForeground(Color.WHITE);
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        loadingLabel.setBounds(0, 0, getWidth(), getHeight());

        JPanel loadingPanel = new JPanel(null);
        loadingPanel.setBackground(Color.BLACK);
        loadingPanel.add(loadingLabel);
        setContentPane(loadingPanel);

        revalidate();
        repaint();

        // ✔ Timer z auto-stopem
        new Timer(2000, e -> {
            ((Timer) e.getSource()).stop(); // 🛑 zatrzymaj, by uniknąć wielokrotnych wywołań
            loadGameView();
        }).start();
    }

    private void loadGameView() {
        setTitle("Drift");
        getContentPane().removeAll();
        GamePanel gamePanel = new GamePanel();
        setContentPane(gamePanel);
        SwingUtilities.invokeLater(gamePanel::requestFocusInWindow);
        revalidate();
        repaint();
    }
}
