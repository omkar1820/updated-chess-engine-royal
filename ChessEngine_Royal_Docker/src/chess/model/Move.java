package chess.model;

public class Move {
    private final Position from;
    private final Position to;
    private final MoveType type;
    private final PieceType promotionPiece; // only for PROMOTION

    public enum MoveType {
        NORMAL, CAPTURE, EN_PASSANT, CASTLING_KINGSIDE, CASTLING_QUEENSIDE, PROMOTION
    }

    public Move(Position from, Position to) {
        this(from, to, MoveType.NORMAL, null);
    }

    public Move(Position from, Position to, MoveType type) {
        this(from, to, type, null);
    }

    public Move(Position from, Position to, MoveType type, PieceType promotionPiece) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.promotionPiece = promotionPiece;
    }

    public Position getFrom()               { return from; }
    public Position getTo()                 { return to; }
    public MoveType getType()               { return type; }
    public PieceType getPromotionPiece()    { return promotionPiece; }

    public boolean isCapture() {
        return type == MoveType.CAPTURE || type == MoveType.EN_PASSANT;
    }

    /** Parse user input like "e2e4" or "e7e8q" */
    public static Move fromString(String s) {
        if (s == null || s.length() < 4) throw new IllegalArgumentException("Invalid move: " + s);
        Position from = Position.fromAlgebraic(s.substring(0, 2));
        Position to   = Position.fromAlgebraic(s.substring(2, 4));
        PieceType promo = null;
        if (s.length() == 5) {
            promo = switch (s.charAt(4)) {
                case 'q' -> PieceType.QUEEN;
                case 'r' -> PieceType.ROOK;
                case 'b' -> PieceType.BISHOP;
                case 'n' -> PieceType.KNIGHT;
                default  -> throw new IllegalArgumentException("Invalid promotion: " + s.charAt(4));
            };
        }
        return new Move(from, to, promo != null ? MoveType.PROMOTION : MoveType.NORMAL, promo);
    }

    @Override
    public String toString() {
        String base = from.toAlgebraic() + to.toAlgebraic();
        if (promotionPiece != null) {
            base += switch (promotionPiece) {
                case QUEEN  -> "q";
                case ROOK   -> "r";
                case BISHOP -> "b";
                case KNIGHT -> "n";
                default     -> "";
            };
        }
        return base;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Move m)) return false;
        return from.equals(m.from) && to.equals(m.to) && type == m.type;
    }

    @Override
    public int hashCode() {
        return from.hashCode() * 64 + to.hashCode();
    }
}
