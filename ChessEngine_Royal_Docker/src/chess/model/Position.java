package chess.model;

public class Position {
    private final int row; // 0-7, 0 = rank 8 (top)
    private final int col; // 0-7, 0 = file a (left)

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public boolean isValid() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    public Position offset(int dRow, int dCol) {
        return new Position(row + dRow, col + dCol);
    }

    /** e.g. "e2" -> Position(6,4) */
    public static Position fromAlgebraic(String s) {
        if (s == null || s.length() < 2) throw new IllegalArgumentException("Invalid: " + s);
        int col = s.charAt(0) - 'a';
        int row = 8 - Character.getNumericValue(s.charAt(1));
        return new Position(row, col);
    }

    public String toAlgebraic() {
        return "" + (char)('a' + col) + (8 - row);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position p)) return false;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() {
        return row * 8 + col;
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
