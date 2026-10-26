package app.guitarlele.tuner;

/** A guitar tuning as MIDI notes, low string first. The guitarlele is a guitar with a capo. */
final class Tuning {
    private static final String[] FLATS = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};
    private static final String[] SHARPS = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    final String name;
    final int[] midi;
    final boolean sharps;

    Tuning(String name, int... midi) { this(name, false, midi); }

    Tuning(String name, boolean sharps, int... midi) {
        this.name = name;
        this.sharps = sharps;
        this.midi = midi;
    }

    String noteName(int midi) { return noteName(midi, sharps); }

    int[] withCapo(int capo) {
        int[] out = new int[midi.length];
        for (int i = 0; i < out.length; i++) out[i] = midi[i] + capo;
        return out;
    }

    static String noteName(int midi, boolean sharps) { return (sharps ? SHARPS : FLATS)[((midi % 12) + 12) % 12]; }
    static int octave(int midi) { return Math.floorDiv(midi, 12) - 1; }
    static double freq(int midi) { return 440.0 * Math.pow(2, (midi - 69) / 12.0); }
}
