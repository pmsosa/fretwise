package app.guitarlele.tuner;

/** A guitar tuning as MIDI notes, low string first. The guitarlele is a guitar with a capo. */
final class Tuning {
    private static final String[] NAMES = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};

    final String name;
    final int[] midi;

    Tuning(String name, int... midi) {
        this.name = name;
        this.midi = midi;
    }

    static final Tuning[] PRESETS = {
        new Tuning("Standard",       40, 45, 50, 55, 59, 64),
        new Tuning("Drop D",         38, 45, 50, 55, 59, 64),
        new Tuning("Half step down", 39, 44, 49, 54, 58, 63),
        new Tuning("DADGAD",         38, 45, 50, 55, 57, 62),
        new Tuning("Open G",         38, 43, 50, 55, 59, 62),
    };

    int[] withCapo(int capo) {
        int[] out = new int[midi.length];
        for (int i = 0; i < out.length; i++) out[i] = midi[i] + capo;
        return out;
    }

    static String noteName(int midi) { return NAMES[((midi % 12) + 12) % 12]; }
    static int octave(int midi) { return Math.floorDiv(midi, 12) - 1; }
    static double freq(int midi) { return 440.0 * Math.pow(2, (midi - 69) / 12.0); }
}
