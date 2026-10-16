package app.guitarlele.tuner;

final class Palette {
    static final int TEXT = 0xFFF4EEFF;
    static final int DIM = 0xFF9C93B8;
    static final int AMBER = 0xFFFFB547;
    static final int CORAL = 0xFFFF6B6B;
    static final int MINT = 0xFF5CF2B0;
    static final int CARD = 0x22FFFFFF;

    static int alpha(int color, float a) {
        return (color & 0x00FFFFFF) | ((int) (a * 255) << 24);
    }
}
