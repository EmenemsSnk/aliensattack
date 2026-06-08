package com.emenems.games.aliens.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

public class ArcadeSoundPlayer {
    private static final float SAMPLE_RATE = 8_000f;
    private static final int[] BACKGROUND_NOTES = {147, 220, 294, 247, 196, 262, 330, 294};
    private Clip backgroundClip;

    public void playShoot() {
        playTone(740, 45, 0.16);
    }

    public void playExplosion() {
        playTone(155, 95, 0.22);
    }

    public void playLifeLoss() {
        try {
            playSamples(createLifeLossSamples());
        } catch (Exception ignored) {
            // Audio devices are not guaranteed in CI/headless environments; gameplay must continue silently.
        }
    }

    public synchronized void startBackgroundMusic() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            return;
        }

        try {
            byte[] samples = createBackgroundSamples();
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(format, samples, 0, samples.length);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception ignored) {
            closeBackgroundClip();
        }
    }

    public synchronized void stopBackgroundMusic() {
        closeBackgroundClip();
    }

    private void playTone(int frequencyHz, int durationMs, double volume) {
        try {
            byte[] samples = createSamples(frequencyHz, durationMs, volume);
            playSamples(samples);
        } catch (Exception ignored) {
            // Audio devices are not guaranteed in CI/headless environments; gameplay must continue silently.
        }
    }

    private byte[] createSamples(int frequencyHz, int durationMs, double volume) {
        int sampleCount = Math.max(1, Math.round(SAMPLE_RATE * durationMs / 1000f));
        byte[] samples = new byte[sampleCount];
        for (int index = 0; index < sampleCount; index++) {
            double angle = 2.0 * Math.PI * frequencyHz * index / SAMPLE_RATE;
            samples[index] = (byte) (Math.sin(angle) * Byte.MAX_VALUE * volume);
        }
        return samples;
    }

    private byte[] createBackgroundSamples() {
        int noteDurationMs = 360;
        int noteSamples = Math.round(SAMPLE_RATE * noteDurationMs / 1000f);
        byte[] samples = new byte[noteSamples * BACKGROUND_NOTES.length];
        int offset = 0;
        for (int note : BACKGROUND_NOTES) {
            for (int index = 0; index < noteSamples; index++) {
                double fade = Math.min(1.0, Math.min(index / 120.0, (noteSamples - index) / 160.0));
                double primary = Math.sin(2.0 * Math.PI * note * index / SAMPLE_RATE);
                double overtone = Math.sin(2.0 * Math.PI * note * 2 * index / SAMPLE_RATE) * 0.28;
                samples[offset + index] = (byte) ((primary + overtone) * Byte.MAX_VALUE * 0.08 * fade);
            }
            offset += noteSamples;
        }
        return samples;
    }

    private byte[] createLifeLossSamples() {
        int firstDurationMs = 70;
        int secondDurationMs = 110;
        int firstSamples = Math.round(SAMPLE_RATE * firstDurationMs / 1000f);
        int secondSamples = Math.round(SAMPLE_RATE * secondDurationMs / 1000f);
        byte[] samples = new byte[firstSamples + secondSamples];

        for (int index = 0; index < firstSamples; index++) {
            double progress = (double) index / Math.max(1, firstSamples - 1);
            double fade = 1.0 - progress * 0.35;
            double primary = Math.sin(2.0 * Math.PI * 220 * index / SAMPLE_RATE);
            double overtone = Math.sin(2.0 * Math.PI * 330 * index / SAMPLE_RATE) * 0.18;
            samples[index] = (byte) ((primary + overtone) * Byte.MAX_VALUE * 0.13 * fade);
        }

        for (int index = 0; index < secondSamples; index++) {
            double progress = (double) index / Math.max(1, secondSamples - 1);
            double fade = 1.0 - progress;
            double primary = Math.sin(2.0 * Math.PI * 165 * index / SAMPLE_RATE);
            double overtone = Math.sin(2.0 * Math.PI * 247 * index / SAMPLE_RATE) * 0.14;
            samples[firstSamples + index] = (byte) ((primary + overtone) * Byte.MAX_VALUE * 0.11 * fade);
        }

        return samples;
    }

    private void playSamples(byte[] samples) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
        Clip clip = AudioSystem.getClip();
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                event.getLine().close();
            }
        });
        clip.open(format, samples, 0, samples.length);
        clip.start();
    }

    private void closeBackgroundClip() {
        if (backgroundClip != null) {
            backgroundClip.stop();
            backgroundClip.close();
            backgroundClip = null;
        }
    }
}
