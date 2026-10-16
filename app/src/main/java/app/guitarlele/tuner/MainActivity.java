package app.guitarlele.tuner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements PitchEngine.Listener {
    private static final int REQ_MIC = 1;
    private static final long IDLE_AFTER_MS = 700;

    private SharedPreferences prefs;
    private float dp;
    private int tuningIndex, capo;
    private int[] strings;
    private int locked = -1;

    private PitchEngine engine;
    private TunerView tunerView;
    private StringsView stringsView;
    private TextView tuningChip, notesLine, lockHint;
    private final TextView[] capoChips = new TextView[8];

    private long lastVoiced;
    private int lastTarget = -1;
    private float smoothCents;
    private int tunedFrames;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        prefs = getSharedPreferences("tuner", MODE_PRIVATE);
        tuningIndex = Math.min(prefs.getInt("tuning", 0), Tuning.PRESETS.length - 1);
        capo = prefs.getInt("capo", 3); // guitar with capo 3 = the user's guitarlele

        buildUi();
        applyTuning();
        engine = new PitchEngine(this);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{getColor(R.color.bg_top), getColor(R.color.bg_bottom)});
        root.setBackground(bg);
        root.setPadding(px(20), px(36), px(20), px(20));

        TextView title = text("Guitarlele Tuner", 26, Palette.TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, lp(-1, -2, 0));

        notesLine = text("", 15, Palette.AMBER, false);
        notesLine.setGravity(Gravity.CENTER);
        notesLine.setLetterSpacing(0.12f);
        root.addView(notesLine, lp(-1, -2, 0, 4, 0));

        // tuning picker
        tuningChip = chip("", true);
        tuningChip.setOnClickListener(v -> pickTuning());
        LinearLayout.LayoutParams chipLp = lp(-2, -2, 0, 18, 0);
        chipLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(tuningChip, chipLp);

        // capo chips
        TextView capoLabel = text("CAPO", 12, Palette.DIM, true);
        capoLabel.setLetterSpacing(0.2f);
        capoLabel.setGravity(Gravity.CENTER);
        root.addView(capoLabel, lp(-1, -2, 0, 14, 0));
        LinearLayout capoRow = new LinearLayout(this);
        capoRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < capoChips.length; i++) {
            final int c = i;
            TextView t = chip(String.valueOf(i), false);
            t.setOnClickListener(v -> { capo = c; applyTuning(); });
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, px(40), 1);
            l.setMargins(px(3), 0, px(3), 0);
            capoRow.addView(t, l);
            capoChips[i] = t;
        }
        root.addView(capoRow, lp(-1, -2, 0, 6, 0));

        tunerView = new TunerView(this);
        root.addView(tunerView, lp(-1, 0, 1, 8, 0));

        stringsView = new StringsView(this);
        stringsView.setListener(i -> {
            locked = locked == i ? -1 : i;
            stringsView.setLocked(locked);
            lastTarget = -1;
            updateLockHint();
        });
        root.addView(stringsView, lp(-1, 92, 0));

        lockHint = text("", 13, Palette.DIM, false);
        lockHint.setGravity(Gravity.CENTER);
        root.addView(lockHint, lp(-1, -2, 0, 4, 0));
        updateLockHint();

        setContentView(root);
    }

    private void applyTuning() {
        Tuning t = Tuning.PRESETS[tuningIndex];
        strings = t.withCapo(capo);
        locked = -1;
        lastTarget = -1;
        prefs.edit().putInt("tuning", tuningIndex).putInt("capo", capo).apply();

        StringBuilder sb = new StringBuilder();
        for (int m : strings) sb.append(sb.length() > 0 ? "  " : "").append(Tuning.noteName(m));
        notesLine.setText(sb);
        tuningChip.setText(t.name + "  ▾");
        for (int i = 0; i < capoChips.length; i++) styleChip(capoChips[i], i == capo);
        stringsView.setStrings(strings);
        tunerView.setIdle();
        updateLockHint();
    }

    private void updateLockHint() {
        lockHint.setText(locked < 0 ? "Auto-detecting the string  ·  tap one to lock it"
                : "Locked to string " + (strings.length - locked) + "  ·  tap again for auto");
    }

    private void pickTuning() {
        String[] names = new String[Tuning.PRESETS.length];
        for (int i = 0; i < names.length; i++) names[i] = Tuning.PRESETS[i].name;
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Guitar tuning (before capo)")
                .setSingleChoiceItems(names, tuningIndex, (d, which) -> {
                    tuningIndex = which;
                    applyTuning();
                    d.dismiss();
                }).show();
    }

    // --- pitch handling -------------------------------------------------------------------

    @Override public void onPitch(float hz) {
        long now = SystemClock.uptimeMillis();
        if (hz < 0) {
            if (now - lastVoiced > IDLE_AFTER_MS) {
                tunerView.setIdle();
                stringsView.setCurrent(-1);
                lastTarget = -1;
            }
            return;
        }
        lastVoiced = now;

        int target = locked;
        float cents = 0;
        if (target < 0) {
            float best = Float.MAX_VALUE;
            for (int i = 0; i < strings.length; i++) {
                float c = centsOff(hz, strings[i]);
                if (Math.abs(c) < best) { best = Math.abs(c); target = i; cents = c; }
            }
        } else {
            cents = centsOff(hz, strings[target]);
        }

        smoothCents = target == lastTarget ? smoothCents * 0.5f + cents * 0.5f : cents;
        if (target != lastTarget) tunedFrames = 0;
        lastTarget = target;

        tunerView.setReading(Tuning.noteName(strings[target]), Tuning.octave(strings[target]),
                smoothCents, hz);
        stringsView.setCurrent(target);
        if (tunerView.isTuned()) {
            if (++tunedFrames >= 4) stringsView.markTuned(target);
        } else {
            tunedFrames = 0;
            if (Math.abs(smoothCents) > 15) stringsView.clearTuned(target);
        }
    }

    private static float centsOff(float hz, int midi) {
        return (float) (1200 * Math.log(hz / Tuning.freq(midi)) / Math.log(2));
    }

    // --- mic lifecycle --------------------------------------------------------------------

    @Override protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            engine.start();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    @Override protected void onPause() {
        engine.stop();
        super.onPause();
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code == REQ_MIC && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            engine.start();
        } else if (code == REQ_MIC) {
            lockHint.setText("Microphone permission is needed to hear your guitarlele");
        }
    }

    // --- view helpers ---------------------------------------------------------------------

    private int px(float v) { return Math.round(v * dp); }

    private LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return lp(w, h, weight, 0, 0);
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight, int top, int bottom) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(
                w > 0 ? px(w) : w, h > 0 ? px(h) : h, weight);
        l.topMargin = px(top);
        l.bottomMargin = px(bottom);
        return l;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return t;
    }

    private TextView chip(String s, boolean wide) {
        TextView t = text(s, 16, Palette.TEXT, true);
        t.setGravity(Gravity.CENTER);
        if (wide) t.setPadding(px(22), px(10), px(22), px(10));
        styleChip(t, wide);
        return t;
    }

    private void styleChip(TextView t, boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(px(20));
        g.setColor(selected ? Palette.AMBER : Palette.CARD);
        t.setBackground(g);
        t.setTextColor(selected ? 0xFF1B1038 : Palette.TEXT);
    }
}
