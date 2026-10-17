package app.guitarlele.tuner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements PitchEngine.Listener {
    private static final int REQ_MIC = 1;
    private static final long IDLE_AFTER_MS = 700;

    private SharedPreferences prefs;
    private UiKit ui;
    private int tuningIndex, capo, tab;
    private int[] strings;
    private int locked = -1;

    private PitchEngine engine;
    private ToneSynth synth;
    private TunerView tunerView;
    private StringsView stringsView;
    private ChordsPanel chordsPanel;
    private LinearLayout tunerPage;
    private TextView tuningChip, notesLine, lockHint;
    private final TextView[] tabChips = new TextView[2];
    private final List<TextView[]> capoRows = new ArrayList<>();

    private long lastVoiced;
    private int lastTarget = -1;
    private float smoothCents;
    private int tunedFrames;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ui = new UiKit(this);
        prefs = getSharedPreferences("tuner", MODE_PRIVATE);
        tuningIndex = Math.min(prefs.getInt("tuning", 0), Tuning.PRESETS.length - 1);
        capo = prefs.getInt("capo", 3); // guitar with capo 3 = the user's guitarlele
        tab = prefs.getInt("tab", 0);

        engine = new PitchEngine(this);
        synth = new ToneSynth();
        buildUi();
        applyTuning();
        showTab(tab);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{getColor(R.color.bg_top), getColor(R.color.bg_bottom)}));
        // targetSdk 35 draws edge to edge, so keep clear of the status bar, cutout and gesture bar
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(i.left + ui.px(20), i.top + ui.px(12), i.right + ui.px(20), i.bottom + ui.px(12));
            return WindowInsets.CONSUMED;
        });

        TextView title = ui.text("Guitarlele", 26, Palette.TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, ui.lp(-1, -2, 0));

        LinearLayout tabs = new LinearLayout(this);
        String[] names = {"Tuner", "Chords"};
        for (int i = 0; i < 2; i++) {
            final int t = i;
            tabChips[i] = ui.chip(names[i], false);
            tabChips[i].setOnClickListener(v -> showTab(t));
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, ui.px(42), 1);
            l.setMargins(ui.px(4), 0, ui.px(4), 0);
            tabs.addView(tabChips[i], l);
        }
        root.addView(tabs, ui.lp(-1, -2, 0, 12, 0));

        FrameLayout content = new FrameLayout(this);
        tunerPage = buildTunerPage();
        chordsPanel = new ChordsPanel(this, ui, synth, capoRow(), capo);
        content.addView(tunerPage, new FrameLayout.LayoutParams(-1, -1));
        content.addView(chordsPanel, new FrameLayout.LayoutParams(-1, -1));
        root.addView(content, ui.lp(-1, 0, 1, 4, 0));

        setContentView(root);
    }

    private LinearLayout buildTunerPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        notesLine = ui.text("", 15, Palette.AMBER, false);
        notesLine.setGravity(Gravity.CENTER);
        notesLine.setLetterSpacing(0.12f);
        page.addView(notesLine, ui.lp(-1, -2, 0, 8, 0));

        tuningChip = ui.chip("", false);
        tuningChip.setPadding(ui.px(22), ui.px(10), ui.px(22), ui.px(10));
        tuningChip.setOnClickListener(v -> pickTuning());
        LinearLayout.LayoutParams chipLp = ui.lp(-2, -2, 0, 14, 0);
        chipLp.gravity = Gravity.CENTER_HORIZONTAL;
        page.addView(tuningChip, chipLp);

        page.addView(capoRow(), ui.lp(-1, -2, 0, 4, 0));

        tunerView = new TunerView(this);
        page.addView(tunerView, ui.lp(-1, 0, 1, 8, 0));

        stringsView = new StringsView(this);
        stringsView.setListener(i -> {
            locked = locked == i ? -1 : i;
            stringsView.setLocked(locked);
            lastTarget = -1;
            updateLockHint();
        });
        page.addView(stringsView, ui.lp(-1, 92, 0));

        lockHint = ui.text("", 13, Palette.DIM, false);
        lockHint.setGravity(Gravity.CENTER);
        page.addView(lockHint, ui.lp(-1, -2, 0, 4, 0));
        return page;
    }

    /** "CAPO 0 1 2 …" chips. Each screen has its own row; they all follow the same capo. */
    private View capoRow() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView label = ui.text("CAPO", 12, Palette.DIM, true);
        label.setLetterSpacing(0.2f);
        label.setGravity(Gravity.CENTER);
        box.addView(label, ui.lp(-1, -2, 0, 10, 0));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        TextView[] chips = new TextView[8];
        for (int i = 0; i < chips.length; i++) {
            final int c = i;
            chips[i] = ui.chip(String.valueOf(i), false);
            chips[i].setOnClickListener(v -> { capo = c; applyTuning(); });
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, ui.px(40), 1);
            l.setMargins(ui.px(3), 0, ui.px(3), 0);
            row.addView(chips[i], l);
        }
        box.addView(row, ui.lp(-1, -2, 0, 6, 0));
        capoRows.add(chips);
        return box;
    }

    private void showTab(int t) {
        tab = t;
        prefs.edit().putInt("tab", t).apply();
        tunerPage.setVisibility(t == 0 ? View.VISIBLE : View.GONE);
        chordsPanel.setVisibility(t == 1 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < tabChips.length; i++) ui.styleChip(tabChips[i], i == t);
        if (t == 0) {
            chordsPanel.stopSound();
            startMic();
        } else {
            engine.stop();
        }
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
        for (TextView[] chips : capoRows)
            for (int i = 0; i < chips.length; i++) ui.styleChip(chips[i], i == capo);
        stringsView.setStrings(strings);
        tunerView.setIdle();
        chordsPanel.setCapo(capo);
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

    private void startMic() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            engine.start();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (tab == 0) startMic();
    }

    @Override protected void onPause() {
        engine.stop();
        chordsPanel.stopSound();
        super.onPause();
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code != REQ_MIC) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (tab == 0) engine.start();
        } else {
            lockHint.setText("Microphone permission is needed to hear your guitarlele");
        }
    }
}
