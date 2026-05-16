package chess.model;

public class Piece {
    private final PieceType type;
    private final Color color;
    private boolean hasMoved;

    public Piece(PieceType type, Color color) {
        this.type = type;
        this.color = color;
        this.hasMoved = false;
    }

    public Piece(Piece other) {
        this.type = other.type;
        this.color = other.color;
        this.hasMoved = other.hasMoved;
    }

    public PieceType getType()  { return type; }
    public Color getColor()     { return color; }
    public boolean hasMoved()   { return hasMoved; }
    public void setMoved(boolean moved) { this.hasMoved = moved; }

    public String getSymbol() {
        return type.getSymbol(color);
    }

    @Override
    public String toString() {
        return color + " " + type;
    }
}
