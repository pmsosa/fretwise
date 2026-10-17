package app.guitarlele.tuner;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Plays a plucked, strummed chord from a few decaying harmonics per string. */
final class ToneSynth {
    private static final int RATE = 44100;
    private static final float SECONDS = 2.2f;
    private static final float STRUM_GAP = 0.03f;
    private static final int HARMONICS = 8;

    private AudioTrack track;
    private int generation;

    void strum(int[] midi) {
        final int gen;
        synchronized (this) { gen = ++generation; }
        new Thread(() -> {
            short[] pcm = render(midi);
            synchronized (ToneSynth.this) {
                if (gen != generation) return; // a newer chord already took over
                releaseTrack();
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(pcm.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
                track.write(pcm, 0, pcm.length);
                track.play();
            }
        }, "synth").start();
    }

    synchronized void release() {
        generation++;
        releaseTrack();
    }

    private void releaseTrack() {
        if (track != null) {
            track.stop();
            track.release();
            track = null;
        }
    }

    private static short[] render(int[] midi) {
        int n = (int) (RATE * SECONDS);
        float[] mix = new float[n];
        int k = 0;
        for (int m : midi) {
            if (m < 0) continue;
            int start = (int) (k++ * STRUM_GAP * RATE);
            double f0 = Tuning.freq(m);
            for (int h = 1; h <= HARMONICS; h++) {
                double f = f0 * h;
                if (f > 9000) break;
                float amp = (float) (0.32 / Math.pow(h, 1.25));
                double decay = 1.3 + 0.85 * h;
                double w = 2 * Math.PI * f / RATE;
                for (int i = start; i < n; i++) {
                    double t = (i - start) / (double) RATE;
                    float attack = Math.min(1f, (i - start) / (0.004f * RATE));
                    mix[i] += (float) (amp * attack * Math.exp(-decay * t) * Math.sin(w * (i - start)));
                }
            }
        }
        float peak = 0.001f;
        for (float v : mix) peak = Math.max(peak, Math.abs(v));
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            float fade = Math.min(1f, (n - i) / (0.05f * RATE));
            out[i] = (short) (mix[i] / peak * 0.85f * fade * 32767);
        }
        return out;
    }
}
