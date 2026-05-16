package chess.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the full chess board and game state.
 * Handles placing/moving pieces, en passant target, castling rights.
 */
public class Board {
    private final Piece[][] grid = new Piece[8][8];
    private Color currentTurn = Color.WHITE;
    private Position enPassantTarget = null; // square a pawn can capture "into"

    // Castling rights
    private boolean whiteKingsideCastle  = true;
    private boolean whiteQueensideCastle = true;
    private boolean blackKingsideCastle  = true;
    private boolean blackQueensideCastle = true;

    private int halfMoveClock  = 0; // for 50-move rule
    private int fullMoveNumber = 1;

    // ── Construction ────────────────────────────────────────────────────────────

    public Board() {
        setupInitialPosition();
    }

    /** Deep-copy constructor */
    public Board(Board other) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (other.grid[r][c] != null)
                    this.grid[r][c] = new Piece(other.grid[r][c]);

        this.currentTurn          = other.currentTurn;
        this.enPassantTarget      = other.enPassantTarget;
        this.whiteKingsideCastle  = other.whiteKingsideCastle;
        this.whiteQueensideCastle = other.whiteQueensideCastle;
        this.blackKingsideCastle  = other.blackKingsideCastle;
        this.blackQueensideCastle = other.blackQueensideCastle;
        this.halfMoveClock        = other.halfMoveClock;
        this.fullMoveNumber       = other.fullMoveNumber;
    }

    private void setupInitialPosition() {
        // Black back rank
        PieceType[] backRank = {
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };
        for (int c = 0; c < 8; c++) {
            grid[0][c] = new Piece(backRank[c], Color.BLACK);
            grid[1][c] = new Piece(PieceType.PAWN, Color.BLACK);
            grid[6][c] = new Piece(PieceType.PAWN, Color.WHITE);
            grid[7][c] = new Piece(backRank[c], Color.WHITE);
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────────

    public Piece getPiece(Position pos)           { return grid[pos.getRow()][pos.getCol()]; }
    public Piece getPiece(int row, int col)        { return grid[row][col]; }
    public void  setPiece(Position pos, Piece p)  { grid[pos.getRow()][pos.getCol()] = p; }
    public Color getCurrentTurn()                 { return currentTurn; }
    public Position getEnPassantTarget()          { return enPassantTarget; }
    public int getHalfMoveClock()                 { return halfMoveClock; }
    public int getFullMoveNumber()                { return fullMoveNumber; }

    public boolean canCastle(Color color, boolean kingside) {
        if (color == Color.WHITE) return kingside ? whiteKingsideCastle : whiteQueensideCastle;
        else                      return kingside ? blackKingsideCastle  : blackQueensideCastle;
    }

    // ── Move Application ─────────────────────────────────────────────────────────

    /**
     * Apply a move to the board. Caller must pass a fully-typed Move
     * (MoveType is set by MoveGenerator before this is called).
     */
    public void applyMove(Move move) {
        Position from = move.getFrom();
        Position to   = move.getTo();
        Piece piece   = getPiece(from);
        Piece captured = getPiece(to);

        // 50-move rule tracking
        if (piece.getType() == PieceType.PAWN || captured != null) halfMoveClock = 0;
        else halfMoveClock++;

        // Clear en passant; will set below if double pawn push
        enPassantTarget = null;

        switch (move.getType()) {
            case NORMAL, CAPTURE -> {
                movePiece(from, to, piece);
            }
            case EN_PASSANT -> {
                movePiece(from, to, piece);
                int captureRow = piece.getColor() == Color.WHITE ? to.getRow() + 1 : to.getRow() - 1;
                grid[captureRow][to.getCol()] = null; // remove captured pawn
            }
            case CASTLING_KINGSIDE -> {
                int row = from.getRow();
                movePiece(from, to, piece);
                movePiece(new Position(row, 7), new Position(row, 5),
                          getPiece(new Position(row, 7)));
            }
            case CASTLING_QUEENSIDE -> {
                int row = from.getRow();
                movePiece(from, to, piece);
                movePiece(new Position(row, 0), new Position(row, 3),
                          getPiece(new Position(row, 0)));
            }
            case PROMOTION -> {
                setPiece(from, null);
                PieceType promo = move.getPromotionPiece() != null
                        ? move.getPromotionPiece() : PieceType.QUEEN;
                Piece promoted = new Piece(promo, piece.getColor());
                promoted.setMoved(true);
                setPiece(to, promoted);
            }
        }

        // En passant target after double pawn push
        if (piece.getType() == PieceType.PAWN) {
            int rowDiff = to.getRow() - from.getRow();
            if (Math.abs(rowDiff) == 2) {
                enPassantTarget = new Position((from.getRow() + to.getRow()) / 2, from.getCol());
            }
        }

        // Update castling rights
        updateCastlingRights(piece, from);

        if (currentTurn == Color.BLACK) fullMoveNumber++;
        currentTurn = currentTurn.opposite();
    }

    private void movePiece(Position from, Position to, Piece piece) {
        if (piece != null) piece.setMoved(true);
        setPiece(from, null);
        setPiece(to, piece);
    }

    private void updateCastlingRights(Piece piece, Position from) {
        if (piece.getType() == PieceType.KING) {
            if (piece.getColor() == Color.WHITE) { whiteKingsideCastle = false; whiteQueensideCastle = false; }
            else                                  { blackKingsideCastle = false; blackQueensideCastle = false; }
        }
        if (piece.getType() == PieceType.ROOK) {
            if (from.equals(new Position(7, 0))) whiteQueensideCastle = false;
            if (from.equals(new Position(7, 7))) whiteKingsideCastle  = false;
            if (from.equals(new Position(0, 0))) blackQueensideCastle = false;
            if (from.equals(new Position(0, 7))) blackKingsideCastle  = false;
        }
    }

    // ── King Location ────────────────────────────────────────────────────────────

    public Position findKing(Color color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color)
                    return new Position(r, c);
            }
        return null;
    }

    // ── Piece Lists ───────────────────────────────────────────────────────────────

    public List<Position> getPiecePositions(Color color) {
        List<Position> list = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p != null && p.getColor() == color)
                    list.add(new Position(r, c));
            }
        return list;
    }

    // ── Display ───────────────────────────────────────────────────────────────────

    public String toDisplay(boolean unicode) {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int r = 0; r < 8; r++) {
            sb.append(8 - r).append(" ");
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p == null) {
                    sb.append(((r + c) % 2 == 0) ? "." : "_").append(" ");
                } else {
                    sb.append(unicode ? getUnicode(p) : p.getSymbol()).append(" ");
                }
            }
            sb.append(8 - r).append("\n");
        }
        sb.append("  a b c d e f g h\n");
        return sb.toString();
    }

    private String getUnicode(Piece p) {
        return switch (p.getType()) {
            case KING   -> p.getColor() == Color.WHITE ? "♔" : "♚";
            case QUEEN  -> p.getColor() == Color.WHITE ? "♕" : "♛";
            case ROOK   -> p.getColor() == Color.WHITE ? "♖" : "♜";
            case BISHOP -> p.getColor() == Color.WHITE ? "♗" : "♝";
            case KNIGHT -> p.getColor() == Color.WHITE ? "♘" : "♞";
            case PAWN   -> p.getColor() == Color.WHITE ? "♙" : "♟";
        };
    }

    // Extra setters for FENParser and testing
    public static Board empty() {
        Board b = new Board();
        for (int r=0;r<8;r++) for(int c=0;c<8;c++) b.grid[r][c]=null;
        return b;
    }
    public void setCurrentTurn(Color t)      { this.currentTurn=t; }
    public void setEnPassantTarget(Position p){ this.enPassantTarget=p; }
    public void setHalfMoveClock(int v)      { this.halfMoveClock=v; }
    public void setFullMoveNumber(int v)     { this.fullMoveNumber=v; }
    public void setCastling(Color color, boolean kingside, boolean allowed) {
        if (color==Color.WHITE) { if(kingside) whiteKingsideCastle=allowed; else whiteQueensideCastle=allowed; }
        else                    { if(kingside) blackKingsideCastle=allowed;  else blackQueensideCastle=allowed; }
    }
}
