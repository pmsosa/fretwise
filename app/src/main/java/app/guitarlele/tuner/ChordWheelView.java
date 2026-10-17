package app.guitarlele.tuner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/** The chord you're on in the middle, and the chords that usually come next around it. */
final class ChordWheelView extends View {
    interface Listener {
        void onCentreTapped();
        void onMoveTapped(Chord.Move move);
    }

    private static final int[] KIND_COLORS = {Palette.AMBER, Palette.MINT, Palette.CORAL};

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp = getResources().getDisplayMetrics().density;
    private Chord centre = new Chord(0, false);
    private Chord.Move[] moves = centre.moves();
    private float grow = 1f;
    private Listener listener;

    ChordWheelView(Context c) {
        super(c);
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        p.setTextAlign(Paint.Align.CENTER);
    }

    void setListener(Listener l) { listener = l; }

    void setCentre(Chord chord) {
        centre = chord;
        moves = chord.moves();
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(450);
        a.setInterpolator(new OvershootInterpolator(1.4f));
        a.addUpdateListener(v -> { grow = (float) v.getAnimatedValue(); invalidate(); });
        a.start();
    }

    private float ringR() { return Math.min(getWidth(), getHeight()) * 0.355f; }
    private float satR() { return Math.min(getWidth(), getHeight()) * 0.115f; }
    private float centreR() { return Math.min(getWidth(), getHeight()) * 0.175f; }

    private float satX(int i) {
        return getWidth() / 2f + (float) Math.cos(Math.toRadians(-90 + i * 60)) * ringR() * grow;
    }

    private float satY(int i) {
        return getHeight() / 2f + (float) Math.sin(Math.toRadians(-90 + i * 60)) * ringR() * grow;
    }

    @Override protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float sr = satR(), cr = centreR();
        float scale = Math.max(0f, grow);

        // soft glow
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx, cy, ringR() + sr,
                Palette.alpha(Palette.AMBER, 0.14f), 0, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, ringR() + sr, p);
        p.setShader(null);

        // spokes
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2 * dp);
        for (int i = 0; i < moves.length; i++) {
            p.setColor(Palette.alpha(KIND_COLORS[moves[i].kind], 0.35f));
            cv.drawLine(cx, cy, satX(i), satY(i), p);
        }

        // satellites
        for (int i = 0; i < moves.length; i++) {
            float x = satX(i), y = satY(i), r = sr * Math.min(1f, scale);
            int col = KIND_COLORS[moves[i].kind];
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF241848);
            cv.drawCircle(x, y, r, p);
            p.setColor(Palette.alpha(col, 0.22f));
            cv.drawCircle(x, y, r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f * dp);
            p.setColor(col);
            cv.drawCircle(x, y, r, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Palette.TEXT);
            p.setTextSize(r * 0.66f);
            cv.drawText(moves[i].chord.name(), x, y + r * 0.12f, p);
            p.setTextSize(r * 0.34f);
            p.setColor(col);
            cv.drawText(moves[i].label, x, y + r * 0.6f, p);
        }

        // centre
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(cx, cy - cr * 0.3f, cr * 1.4f, Palette.AMBER, 0xFFFF8A4C, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, cr, p);
        p.setShader(null);
        p.setColor(0xFF1B1038);
        p.setTextSize(cr * 0.85f);
        cv.drawText(centre.name(), cx, cy + cr * 0.28f, p);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (e.getAction() != MotionEvent.ACTION_UP || listener == null) return true;
        float x = e.getX(), y = e.getY();
        for (int i = 0; i < moves.length; i++) {
            if (Math.hypot(x - satX(i), y - satY(i)) <= satR() * 1.15f) {
                listener.onMoveTapped(moves[i]);
                performClick();
                return true;
            }
        }
        if (Math.hypot(x - getWidth() / 2f, y - getHeight() / 2f) <= centreR() * 1.2f) {
            listener.onCentreTapped();
            performClick();
        }
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }
}
