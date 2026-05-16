package chess.model;

public enum PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN;

    public int getValue() {
        return switch (this) {
            case PAWN   -> 100;
            case KNIGHT -> 320;
            case BISHOP -> 330;
            case ROOK   -> 500;
            case QUEEN  -> 900;
            case KING   -> 20000;
        };
    }

    public String getSymbol(Color color) {
        String s = switch (this) {
            case KING   -> "K";
            case QUEEN  -> "Q";
            case ROOK   -> "R";
            case BISHOP -> "B";
            case KNIGHT -> "N";
            case PAWN   -> "P";
        };
        return color == Color.WHITE ? s : s.toLowerCase();
    }
}
