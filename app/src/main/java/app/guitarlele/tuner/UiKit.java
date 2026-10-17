package app.guitarlele.tuner;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small view helpers shared by the tuner and chord screens. */
final class UiKit {
    private final Context ctx;
    final float dp;

    UiKit(Context ctx) {
        this.ctx = ctx;
        dp = ctx.getResources().getDisplayMetrics().density;
    }

    int px(float v) { return Math.round(v * dp); }

    LinearLayout.LayoutParams lp(int w, int h, float weight, int top, int bottom) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(
                w > 0 ? px(w) : w, h > 0 ? px(h) : h, weight);
        l.topMargin = px(top);
        l.bottomMargin = px(bottom);
        return l;
    }

    LinearLayout.LayoutParams lp(int w, int h, float weight) { return lp(w, h, weight, 0, 0); }

    TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return t;
    }

    TextView chip(String s, boolean selected) {
        TextView t = text(s, 16, Palette.TEXT, true);
        t.setGravity(Gravity.CENTER);
        styleChip(t, selected);
        return t;
    }

    void styleChip(TextView t, boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(px(20));
        g.setColor(selected ? Palette.AMBER : Palette.CARD);
        t.setBackground(g);
        t.setTextColor(selected ? 0xFF1B1038 : Palette.TEXT);
    }
}
