package app.guitarlele.tuner;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** The chord wheel screen: pick a start chord, then follow the wheel to where it could go next. */
final class ChordsPanel extends LinearLayout {
    private final UiKit ui;
    private final ToneSynth synth;
    private final ChordWheelView wheel;
    private final ChordDiagramView diagram;
    private final TextView shapeText, soundsText;
    private final LinearLayout pathRow;
    private final HorizontalScrollView pathScroll;
    private final TextView[] rootChips = new TextView[12];
    private final TextView qualityChip;

    private final List<Chord> path = new ArrayList<>();
    private boolean startMinor;
    private int capo;

    ChordsPanel(Context ctx, UiKit ui, ToneSynth synth, View capoRow, int capo) {
        super(ctx);
        this.ui = ui;
        this.synth = synth;
        this.capo = capo;
        setOrientation(VERTICAL);

        addView(capoRow, ui.lp(-1, -2, 0, 4, 0));

        // start chord: major/minor + the twelve roots
        LinearLayout startRow = new LinearLayout(ctx);
        startRow.setGravity(Gravity.CENTER_VERTICAL);
        qualityChip = ui.chip("Major", false);
        qualityChip.setTextSize(14);
        qualityChip.setPadding(ui.px(14), ui.px(8), ui.px(14), ui.px(8));
        qualityChip.setOnClickListener(v -> {
            startMinor = !startMinor;
            qualityChip.setText(startMinor ? "Minor" : "Major");
            refreshRootLabels();
        });
        startRow.addView(qualityChip, ui.lp(-2, -2, 0));
        HorizontalScrollView rootScroll = new HorizontalScrollView(ctx);
        rootScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout roots = new LinearLayout(ctx);
        for (int i = 0; i < 12; i++) {
            final int root = i;
            TextView t = ui.chip("", false);
            t.setTextSize(14);
            t.setPadding(ui.px(13), ui.px(8), ui.px(13), ui.px(8));
            t.setOnClickListener(v -> startOn(new Chord(root, startMinor)));
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
            l.leftMargin = ui.px(6);
            roots.addView(t, l);
            rootChips[i] = t;
        }
        rootScroll.addView(roots);
        startRow.addView(rootScroll, ui.lp(0, -2, 1));
        addView(startRow, ui.lp(-1, -2, 0, 12, 0));
        refreshRootLabels();

        // the path taken so far
        pathScroll = new HorizontalScrollView(ctx);
        pathScroll.setHorizontalScrollBarEnabled(false);
        pathRow = new LinearLayout(ctx);
        pathRow.setGravity(Gravity.CENTER_VERTICAL);
        pathScroll.addView(pathRow);
        addView(pathScroll, ui.lp(-1, 40, 0, 10, 0));

        wheel = new ChordWheelView(ctx);
        wheel.setListener(new ChordWheelView.Listener() {
            @Override public void onCentreTapped() { sound(); }
            @Override public void onMoveTapped(Chord.Move m) { go(m.chord); }
        });
        addView(wheel, ui.lp(-1, 0, 1));

        // how to play it
        LinearLayout info = new LinearLayout(ctx);
        info.setGravity(Gravity.CENTER_VERTICAL);
        diagram = new ChordDiagramView(ctx);
        info.addView(diagram, ui.lp(170, 150, 0));
        LinearLayout side = new LinearLayout(ctx);
        side.setOrientation(VERTICAL);
        side.setPadding(ui.px(16), 0, 0, 0);
        side.addView(ui.text("HOW TO PLAY", 12, Palette.DIM, true));
        shapeText = ui.text("", 22, Palette.TEXT, true);
        shapeText.setLetterSpacing(0.1f);
        side.addView(shapeText, ui.lp(-2, -2, 0, 2, 0));
        soundsText = ui.text("", 14, Palette.AMBER, false);
        side.addView(soundsText, ui.lp(-2, -2, 0, 8, 0));
        side.addView(ui.text("Tap the middle to hear it again", 12, Palette.DIM, false), ui.lp(-2, -2, 0, 8, 0));
        info.addView(side, ui.lp(0, -2, 1));
        addView(info, ui.lp(-1, -2, 0, 0, 4));

        startOn(new Chord(7, false)); // G, a friendly place to begin
    }

    void setCapo(int capo) {
        this.capo = capo;
        refreshInfo();
    }

    void stopSound() { synth.release(); }

    private void refreshRootLabels() {
        for (int i = 0; i < 12; i++) rootChips[i].setText(Chord.rootName(i) + (startMinor ? "m" : ""));
    }

    private void startOn(Chord c) {
        path.clear();
        go(c);
    }

    private void go(Chord c) {
        path.add(c);
        wheel.setCentre(c);
        refreshPath();
        refreshInfo();
        sound();
    }

    private void jumpBack(int index) {
        while (path.size() > index + 1) path.remove(path.size() - 1);
        Chord c = path.get(index);
        wheel.setCentre(c);
        refreshPath();
        refreshInfo();
        sound();
    }

    private Chord current() { return path.get(path.size() - 1); }

    private void sound() { synth.strum(current().midiNotes(capo)); }

    private void refreshInfo() {
        Chord c = current();
        diagram.setChord(c);
        shapeText.setText(c.shapeText());
        soundsText.setText(capo == 0 ? c.name() + " chord"
                : c.name() + " shape  ·  capo " + capo + "\nsounds as " + c.soundsAs(capo));
    }

    private void refreshPath() {
        pathRow.removeAllViews();
        int from = Math.max(0, path.size() - 12);
        for (int i = from; i < path.size(); i++) {
            final int idx = i;
            boolean last = i == path.size() - 1;
            if (i > from) pathRow.addView(ui.text("›", 18, Palette.DIM, false), ui.lp(-2, -2, 0));
            TextView t = ui.chip(path.get(i).name(), last);
            t.setTextSize(14);
            t.setPadding(ui.px(12), ui.px(6), ui.px(12), ui.px(6));
            if (!last) t.setOnClickListener(v -> jumpBack(idx));
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
            l.leftMargin = ui.px(4);
            l.rightMargin = ui.px(4);
            pathRow.addView(t, l);
        }
        pathScroll.post(() -> pathScroll.fullScroll(View.FOCUS_RIGHT));
    }
}
