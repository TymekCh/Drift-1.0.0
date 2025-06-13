package org.game;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioPlayer {
    private Clip clip;

    public void playLoop(String filePath) {
        try {
            File soundFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);

            clip = AudioSystem.getClip();
            clip.open(audioStream);

            setVolume(OptionsMenu.getVolume()); // Ustawia głośność z opcji
            clip.loop(Clip.LOOP_CONTINUOUSLY); // Nieskończona pętla
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void setVolume(float volume) {
        if (clip == null) return;
        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        // Przekształca [0.0–1.0] na wartość dB
        float min = gainControl.getMinimum();
        float max = gainControl.getMaximum();
        float dB = (float) (Math.log10(Math.max(volume, 0.0001)) * 20.0);
        dB = Math.max(min, Math.min(dB, max)); // Ogranicza w zakresie
        gainControl.setValue(dB);
    }

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }
}
