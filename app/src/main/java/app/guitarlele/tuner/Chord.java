package app.guitarlele.tuner;

/** A major or minor chord, named by the guitar shape you play (the capo only changes the sound). */
final class Chord {
    private static final String[] NAMES = {"C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"};
    static final int[] STANDARD = {40, 45, 50, 55, 59, 64};

    /** A chord you could go to next, with its role in the old chord's key. kind: 0 home, 1 away, 2 tension. */
    static final class Move {
        final Chord chord;
        final String label;
        final int kind;
        Move(Chord chord, String label, int kind) { this.chord = chord; this.label = label; this.kind = kind; }
    }

    final int root;
    final boolean minor;

    Chord(int root, boolean minor) {
        this.root = ((root % 12) + 12) % 12;
        this.minor = minor;
    }

    static String rootName(int root) { return NAMES[((root % 12) + 12) % 12]; }

    String name() { return NAMES[root] + (minor ? "m" : ""); }

    String soundsAs(int capo) { return NAMES[(root + capo) % 12] + (minor ? "m" : ""); }

    Move[] moves() {
        if (minor) {
            return new Move[]{
                move(5, true, "iv", 1), move(7, false, "V", 2), move(8, false, "♭VI", 1),
                move(10, false, "♭VII", 2), move(3, false, "♭III", 0), move(7, true, "v", 0)};
        }
        return new Move[]{
            move(5, false, "IV", 1), move(7, false, "V", 2), move(9, true, "vi", 0),
            move(2, true, "ii", 1), move(4, true, "iii", 0), move(10, false, "♭VII", 2)};
    }

    private Move move(int semis, boolean minor, String label, int kind) {
        return new Move(new Chord(root + semis, minor), label, kind);
    }

    /** Frets for the six strings, low to high; -1 is muted. */
    int[] frets() {
        if (!minor && root == 0) return new int[]{-1, 3, 2, 0, 1, 0};
        if (!minor && root == 7) return new int[]{3, 2, 0, 0, 0, 3};
        if (!minor && root == 2) return new int[]{-1, -1, 0, 2, 3, 2};
        if (minor && root == 2) return new int[]{-1, -1, 0, 2, 3, 1};
        int[] eForm = minor ? new int[]{0, 2, 2, 0, 0, 0} : new int[]{0, 2, 2, 1, 0, 0};
        int[] aForm = minor ? new int[]{-1, 0, 2, 2, 1, 0} : new int[]{-1, 0, 2, 2, 2, 0};
        int fe = (root - 4 + 12) % 12, fa = (root - 9 + 12) % 12;
        int[] form = fe <= fa ? eForm : aForm;
        int shift = Math.min(fe, fa);
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) out[i] = form[i] < 0 ? -1 : form[i] + shift;
        return out;
    }

    /** MIDI notes of the strummed strings (-1 = not played) once the capo is on. */
    int[] midiNotes(int capo) {
        int[] f = frets(), out = new int[6];
        for (int i = 0; i < 6; i++) out[i] = f[i] < 0 ? -1 : STANDARD[i] + capo + f[i];
        return out;
    }

    String shapeText() {
        StringBuilder sb = new StringBuilder();
        for (int f : frets()) sb.append(f < 0 ? "x" : String.valueOf(f)).append(' ');
        return sb.toString().trim();
    }
}
