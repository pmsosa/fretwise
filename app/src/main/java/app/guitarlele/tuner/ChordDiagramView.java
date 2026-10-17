package app.guitarlele.tuner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** A guitar chord box: six strings, five frets, dots where the fingers go. */
final class ChordDiagramView extends View {
    private static final int ROWS = 5;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float dp = getResources().getDisplayMetrics().density;
    private int[] frets = new int[]{-1, -1, -1, -1, -1, -1};

    ChordDiagramView(Context c) {
        super(c);
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        p.setTextAlign(Paint.Align.CENTER);
    }

    void setChord(Chord chord) {
        frets = chord.frets();
        invalidate();
    }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float left = 34 * dp, right = w - 14 * dp, top = 30 * dp, bottom = h - 8 * dp;
        float sx = (right - left) / 5f, fy = (bottom - top) / ROWS;

        int minPos = Integer.MAX_VALUE, max = 0;
        for (int f : frets) if (f > 0) { minPos = Math.min(minPos, f); max = Math.max(max, f); }
        int start = max <= ROWS ? 1 : minPos;

        // frets and nut
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(Palette.alpha(Palette.TEXT, 0.28f));
        p.setStrokeWidth(1.5f * dp);
        for (int r = 1; r <= ROWS; r++) cv.drawLine(left, top + r * fy, right, top + r * fy, p);
        p.setColor(Palette.TEXT);
        p.setStrokeWidth(start == 1 ? 5 * dp : 1.5f * dp);
        cv.drawLine(left, top, right, top, p);
        // strings
        p.setStrokeWidth(1.5f * dp);
        p.setColor(Palette.alpha(Palette.TEXT, 0.6f));
        for (int s = 0; s < 6; s++) cv.drawLine(left + s * sx, top, left + s * sx, bottom, p);

        // barre
        if (minPos != Integer.MAX_VALUE) {
            int first = -1, last = -1, count = 0;
            for (int s = 0; s < 6; s++) if (frets[s] == minPos) { if (first < 0) first = s; last = s; count++; }
            if (count >= 2) {
                float y = top + (minPos - start + 0.5f) * fy;
                rect.set(left + first * sx - 9 * dp, y - 9 * dp, left + last * sx + 9 * dp, y + 9 * dp);
                p.setStyle(Paint.Style.FILL);
                p.setColor(Palette.AMBER);
                cv.drawRoundRect(rect, 9 * dp, 9 * dp, p);
            }
        }

        // dots, open rings and muted crosses
        for (int s = 0; s < 6; s++) {
            float x = left + s * sx;
            int f = frets[s];
            if (f > 0) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(Palette.AMBER);
                cv.drawCircle(x, top + (f - start + 0.5f) * fy, 9 * dp, p);
            } else if (f == 0) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * dp);
                p.setColor(Palette.MINT);
                cv.drawCircle(x, top - 13 * dp, 5 * dp, p);
            } else {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2 * dp);
                p.setColor(Palette.CORAL);
                float c = 4.5f * dp, y = top - 13 * dp;
                cv.drawLine(x - c, y - c, x + c, y + c, p);
                cv.drawLine(x - c, y + c, x + c, y - c, p);
            }
        }

        if (start > 1) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Palette.DIM);
            p.setTextSize(14 * dp);
            cv.drawText(start + "fr", 16 * dp, top + 0.5f * fy + 5 * dp, p);
        }
    }
}
