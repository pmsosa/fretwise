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

    /**
     * Frets for each string (-1 = muted), in neck order. open holds the open-string MIDI notes.
     * Six strings use the usual guitar shapes; shorter instruments get the easiest fingering found.
     */
    int[] frets(int[] open) {
        return open.length == 6 ? guitarShape() : searchShape(open);
    }

    private int[] guitarShape() {
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

    private static final int MAX_FRET = 9, MAX_SPAN = 3;

    /** Every string plays a chord tone, all three tones are heard, the hand spans few frets and sits low. */
    private int[] searchShape(int[] open) {
        int[] tones = {root, (root + (minor ? 3 : 4)) % 12, (root + 7) % 12};
        int[] cur = new int[open.length];
        int[][] best = {null};
        int[] bestScore = {Integer.MAX_VALUE};
        search(open, tones, 0, cur, best, bestScore);
        if (best[0] != null) return best[0];
        int[] fallback = new int[open.length];
        java.util.Arrays.fill(fallback, -1);
        return fallback;
    }

    private void search(int[] open, int[] tones, int i, int[] cur, int[][] best, int[] bestScore) {
        if (i == open.length) {
            boolean[] heard = new boolean[3];
            int sum = 0, lo = Integer.MAX_VALUE, hi = 0;
            for (int s = 0; s < open.length; s++) {
                int pc = (open[s] + cur[s]) % 12;
                for (int t = 0; t < 3; t++) if (tones[t] == pc) heard[t] = true;
                sum += cur[s];
                if (cur[s] > 0) { lo = Math.min(lo, cur[s]); hi = Math.max(hi, cur[s]); }
            }
            if (!(heard[0] && heard[1] && heard[2])) return;
            int span = hi == 0 ? 0 : hi - lo;
            if (span > MAX_SPAN) return;
            int score = sum + 2 * span;
            if (score < bestScore[0]) { bestScore[0] = score; best[0] = cur.clone(); }
            return;
        }
        for (int f = 0; f <= MAX_FRET; f++) {
            int pc = (open[i] + f) % 12;
            if (pc != tones[0] && pc != tones[1] && pc != tones[2]) continue;
            cur[i] = f;
            search(open, tones, i + 1, cur, best, bestScore);
        }
    }

    /** MIDI notes of the strummed strings (-1 = not played) once the capo is on. */
    int[] midiNotes(int[] open, int capo) {
        int[] f = frets(open), out = new int[open.length];
        for (int i = 0; i < out.length; i++) out[i] = f[i] < 0 ? -1 : open[i] + capo + f[i];
        return out;
    }

    String shapeText(int[] open) {
        StringBuilder sb = new StringBuilder();
        for (int f : frets(open)) sb.append(f < 0 ? "x" : String.valueOf(f)).append(' ');
        return sb.toString().trim();
    }
}
