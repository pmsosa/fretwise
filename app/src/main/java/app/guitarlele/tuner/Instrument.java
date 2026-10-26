package app.guitarlele.tuner;

/** An instrument and the tunings it is commonly strung in. Strings are listed in the order they sit on the neck. */
final class Instrument {
    final String name;
    final String tuningTitle;
    final int defaultCapo;
    final Tuning[] tunings;

    private Instrument(String name, String tuningTitle, int defaultCapo, Tuning... tunings) {
        this.name = name;
        this.tuningTitle = tuningTitle;
        this.defaultCapo = defaultCapo;
        this.tunings = tunings;
    }

    static final int GUITARLELE = 0;

    static final Instrument[] ALL = {
        // a guitarlele is a guitar with a capo, so these are guitar tunings
        new Instrument("Guitarlele", "Guitar tuning (before capo)", 3,
            new Tuning("Standard",       40, 45, 50, 55, 59, 64),
            new Tuning("Drop D",         38, 45, 50, 55, 59, 64),
            new Tuning("Half step down", 39, 44, 49, 54, 58, 63),
            new Tuning("DADGAD",         38, 45, 50, 55, 57, 62),
            new Tuning("Open G",         38, 43, 50, 55, 59, 62)),
        new Instrument("Ukulele", "Ukulele tuning", 0,
            new Tuning("Standard GCEA",  67, 60, 64, 69),
            new Tuning("Low G",          55, 60, 64, 69),
            new Tuning("Baritone DGBE",  50, 55, 59, 64),
            new Tuning("D tuning ADF#B", true, 69, 62, 66, 71)),
        // strings 1-4 are A3 D4 F#4 B3; listed in neck order (4th to 1st) that is B3 F#4 D4 A3
        new Instrument("Cuatro", "Cuatro venezolano tuning", 0,
            new Tuning("Standard (A D F# B)", true, 59, 66, 62, 57)),
    };
}
