package app.guitarlele.tuner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Gauge with a swinging needle, the detected note and how many cents off it is. */
final class TunerView extends View {
    private static final float RANGE = 50f;       // cents shown each side
    private static final float SWEEP = 62f;       // degrees each side of straight up
    static final float IN_TUNE = 5f;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private final float dp = getResources().getDisplayMetrics().density;

    private boolean active;
    private String note = "–", octave = "", hint = "Play a string";
    private float freq, targetCents, shownCents, glow;

    TunerView(Context c) {
        super(c);
        p.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    }

    void setIdle() {
        active = false;
        note = "–"; octave = "";
        hint = "Play a string";
        targetCents = 0;
        postInvalidateOnAnimation();
    }

    void setReading(String note, int octave, float cents, float hz) {
        active = true;
        this.note = note;
        this.octave = String.valueOf(octave);
        this.freq = hz;
        targetCents = Math.max(-RANGE, Math.min(RANGE, cents));
        float abs = Math.abs(cents);
        hint = abs <= IN_TUNE ? "In tune" : cents < 0 ? "Too low  ·  tune up" : "Too high  ·  tune down";
        postInvalidateOnAnimation();
    }

    boolean isTuned() { return active && Math.abs(targetCents) <= IN_TUNE; }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float r = Math.min(w * 0.44f, (h - 24 * dp) / 1.7f);
        float cx = w / 2, cy = 24 * dp + r;

        // ease needle + glow toward their targets
        shownCents += (targetCents - shownCents) * 0.18f;
        float glowTarget = isTuned() ? 1f : 0f;
        glow += (glowTarget - glow) * 0.12f;
        boolean settling = Math.abs(targetCents - shownCents) > 0.05f || Math.abs(glowTarget - glow) > 0.01f;

        // glow behind gauge
        if (glow > 0.01f) {
            p.setStyle(Paint.Style.FILL);
            p.setShader(new RadialGradient(cx, cy, r * 1.25f,
                    Palette.alpha(Palette.MINT, 0.35f * glow), 0, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, r * 1.25f, p);
            p.setShader(null);
        }

        // ticks
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStyle(Paint.Style.STROKE);
        for (int c = -50; c <= 50; c += 5) {
            boolean major = c % 25 == 0;
            float ang = (float) Math.toRadians(-90 + c / RANGE * SWEEP);
            float len = (major ? 22 : 12) * dp;
            int col = Math.abs(c) <= IN_TUNE ? Palette.MINT : Math.abs(c) <= 20 ? Palette.AMBER : Palette.CORAL;
            p.setColor(Palette.alpha(col, major || c == 0 ? 1f : 0.55f));
            p.setStrokeWidth((major ? 4 : 3) * dp);
            cv.drawLine(cx + (float) Math.cos(ang) * (r - len), cy + (float) Math.sin(ang) * (r - len),
                    cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r, p);
        }

        // flat / sharp labels
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(22 * dp);
        p.setColor(Palette.DIM);
        for (int s = -1; s <= 1; s += 2) {
            float ang = (float) Math.toRadians(-90 + s * SWEEP);
            cv.drawText(s < 0 ? "♭" : "♯", cx + (float) Math.cos(ang) * (r + 4 * dp) + s * 6 * dp,
                    cy + (float) Math.sin(ang) * (r + 4 * dp) + 8 * dp, p);
        }

        // needle
        int needle = !active ? Palette.DIM : isTuned() ? Palette.MINT : Math.abs(shownCents) <= 20 ? Palette.AMBER : Palette.CORAL;
        float na = (float) Math.toRadians(-90 + shownCents / RANGE * SWEEP);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5 * dp);
        p.setColor(Palette.alpha(needle, active ? 1f : 0.5f));
        cv.drawLine(cx, cy, cx + (float) Math.cos(na) * (r - 34 * dp), cy + (float) Math.sin(na) * (r - 34 * dp), p);
        p.setStyle(Paint.Style.FILL);
        cv.drawCircle(cx, cy, 11 * dp, p);
        p.setColor(0xFF1B1038);
        cv.drawCircle(cx, cy, 4.5f * dp, p);

        // note name
        p.setColor(active ? (isTuned() ? Palette.MINT : Palette.TEXT) : Palette.alpha(Palette.TEXT, 0.35f));
        p.setTextSize(r * 0.62f);
        p.setTextAlign(Paint.Align.CENTER);
        float baseline = cy + r * 0.78f;
        cv.drawText(note, cx - (octave.isEmpty() ? 0 : 8 * dp), baseline, p);
        if (!octave.isEmpty()) {
            float nw = p.measureText(note);
            p.setTextSize(r * 0.22f);
            p.setTextAlign(Paint.Align.LEFT);
            cv.drawText(octave, cx - 8 * dp + nw / 2 + 2 * dp, baseline, p);
        }

        // hint + cents/frequency
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(17 * dp);
        p.setColor(active ? needle : Palette.DIM);
        cv.drawText(hint, cx, baseline + 30 * dp, p);
        if (active) {
            p.setTextSize(14 * dp);
            p.setColor(Palette.DIM);
            cv.drawText(String.format("%+.0f cents  ·  %.1f Hz", targetCents, freq), cx, baseline + 54 * dp, p);
        }

        if (settling) postInvalidateOnAnimation();
    }
}
