package app.guitarlele.tuner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** One round button per string. Highlights the one being heard; tap to lock onto a string. */
final class StringsView extends View {
    interface Listener { void onStringTapped(int index); }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp = getResources().getDisplayMetrics().density;
    private int[] midi = new int[0];
    private boolean[] tuned = new boolean[0];
    private int current = -1, locked = -1;
    private Listener listener;

    StringsView(Context c) {
        super(c);
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        p.setTextAlign(Paint.Align.CENTER);
    }

    void setListener(Listener l) { listener = l; }

    void setStrings(int[] midi) {
        this.midi = midi;
        tuned = new boolean[midi.length];
        current = locked = -1;
        invalidate();
    }

    void setCurrent(int i) { if (i != current) { current = i; invalidate(); } }
    void setLocked(int i) { locked = i; invalidate(); }
    void markTuned(int i) { if (i >= 0 && i < tuned.length && !tuned[i]) { tuned[i] = true; invalidate(); } }
    void clearTuned(int i) { if (i >= 0 && i < tuned.length && tuned[i]) { tuned[i] = false; invalidate(); } }

    @Override protected void onDraw(Canvas cv) {
        int n = midi.length;
        if (n == 0) return;
        float w = getWidth(), cell = w / n;
        float r = Math.min(cell * 0.42f, 30 * dp), cy = getHeight() / 2f - 8 * dp;
        for (int i = 0; i < n; i++) {
            float cx = cell * (i + 0.5f);
            boolean on = i == current || i == locked;
            p.setStyle(Paint.Style.FILL);
            p.setColor(on ? Palette.AMBER : Palette.CARD);
            cv.drawCircle(cx, cy, r, p);
            if (i == locked) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2.5f * dp);
                p.setColor(Palette.TEXT);
                cv.drawCircle(cx, cy, r + 4 * dp, p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(on ? 0xFF1B1038 : Palette.TEXT);
            p.setTextSize(r * 0.78f);
            cv.drawText(Tuning.noteName(midi[i]), cx, cy + r * 0.27f, p);
            p.setTextSize(11 * dp);
            p.setColor(Palette.DIM);
            cv.drawText(String.valueOf(n - i), cx, cy + r + 18 * dp, p);
            if (tuned[i]) {
                p.setColor(Palette.MINT);
                cv.drawCircle(cx + r * 0.72f, cy - r * 0.72f, 6 * dp, p);
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP && midi.length > 0 && listener != null) {
            int i = (int) (e.getX() / (getWidth() / (float) midi.length));
            listener.onStringTapped(Math.max(0, Math.min(midi.length - 1, i)));
            performClick();
        }
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }
}
