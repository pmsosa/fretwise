package app.guitarlele.tuner;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

/** Records from the mic and reports the fundamental frequency (YIN) on the main thread. */
final class PitchEngine {
    interface Listener { void onPitch(float hz); } // hz < 0: nothing pitched heard

    private static final int RATE = 44100;
    private static final int WINDOW = 4096;
    private static final int HOP = 1024;
    private static final float MIN_HZ = 60f, MAX_HZ = 800f;
    private static final float SILENCE_RMS = 0.006f;
    private static final float YIN_THRESHOLD = 0.15f;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private volatile boolean running;
    private Thread thread;

    PitchEngine(Listener l) { listener = l; }

    @SuppressWarnings("MissingPermission")
    void start() {
        if (running) return;
        running = true;
        thread = new Thread(() -> {
            int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord rec = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, WINDOW * 4));
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) { running = false; return; }
            rec.startRecording();
            short[] hop = new short[HOP];
            float[] win = new float[WINDOW];
            while (running) {
                int got = 0;
                while (got < HOP && running) {
                    int n = rec.read(hop, got, HOP - got);
                    if (n <= 0) break;
                    got += n;
                }
                if (got < HOP) continue;
                System.arraycopy(win, HOP, win, 0, WINDOW - HOP);
                for (int i = 0; i < HOP; i++) win[WINDOW - HOP + i] = hop[i] / 32768f;
                final float hz = detect(win);
                main.post(() -> { if (running) listener.onPitch(hz); });
            }
            rec.stop();
            rec.release();
        }, "pitch-engine");
        thread.start();
    }

    void stop() {
        running = false;
        main.removeCallbacksAndMessages(null);
    }

    static float detect(float[] x) {
        int w = x.length / 2;
        double energy = 0;
        for (int i = 0; i < x.length; i++) energy += x[i] * x[i];
        if (Math.sqrt(energy / x.length) < SILENCE_RMS) return -1f;

        int tauMin = (int) (RATE / MAX_HZ);
        int tauMax = Math.min(w - 1, (int) (RATE / MIN_HZ));
        float[] d = new float[tauMax + 1];
        for (int tau = 1; tau <= tauMax; tau++) {
            float s = 0;
            for (int j = 0; j < w; j++) { float diff = x[j] - x[j + tau]; s += diff * diff; }
            d[tau] = s;
        }
        // cumulative mean normalised difference
        float run = 0;
        d[0] = 1;
        for (int tau = 1; tau <= tauMax; tau++) {
            run += d[tau];
            d[tau] = run == 0 ? 1 : d[tau] * tau / run;
        }
        int best = -1;
        for (int tau = tauMin; tau < tauMax; tau++) {
            if (d[tau] < YIN_THRESHOLD) {
                while (tau + 1 < tauMax && d[tau + 1] < d[tau]) tau++;
                best = tau;
                break;
            }
        }
        if (best < 0) return -1f;
        float a = d[best - 1], b = d[best], c = d[best + 1];
        float denom = a - 2 * b + c;
        float shift = denom == 0 ? 0 : 0.5f * (a - c) / denom;
        return RATE / (best + shift);
    }
}
