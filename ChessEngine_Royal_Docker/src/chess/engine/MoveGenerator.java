package chess.engine;

import chess.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates all pseudo-legal and legal moves for a given board state.
 * Legal move = pseudo-legal move that does not leave own king in check.
 */
public class MoveGenerator {

    // ── Public API ────────────────────────────────────────────────────────────────

    /** All legal moves for the side whose turn it is. */
    public List<Move> generateLegalMoves(Board board) {
        List<Move> pseudoLegal = generatePseudoLegalMoves(board, board.getCurrentTurn());
        List<Move> legal = new ArrayList<>();
        for (Move m : pseudoLegal) {
            Board copy = new Board(board);
            copy.applyMove(enrichMoveType(copy, m));
            // After the move, the mover's king must not be in check
            Color mover = board.getCurrentTurn();
            if (!isInCheck(copy, mover)) {
                legal.add(enrichMoveType(board, m));
            }
        }
        return legal;
    }

    public boolean isInCheck(Board board, Color color) {
        Position king = board.findKing(color);
        if (king == null) return false;
        return isAttackedBy(board, king, color.opposite());
    }

    public boolean isCheckmate(Board board) {
        return isInCheck(board, board.getCurrentTurn())
                && generateLegalMoves(board).isEmpty();
    }

    public boolean isStalemate(Board board) {
        return !isInCheck(board, board.getCurrentTurn())
                && generateLegalMoves(board).isEmpty();
    }

    public boolean isDraw50Move(Board board) {
        return board.getHalfMoveClock() >= 100;
    }

    // ── Pseudo-Legal Generation ───────────────────────────────────────────────────

    public List<Move> generatePseudoLegalMoves(Board board, Color color) {
        List<Move> moves = new ArrayList<>();
        for (Position pos : board.getPiecePositions(color)) {
            Piece piece = board.getPiece(pos);
            moves.addAll(switch (piece.getType()) {
                case PAWN   -> generatePawnMoves(board, pos, color);
                case KNIGHT -> generateKnightMoves(board, pos, color);
                case BISHOP -> generateSlidingMoves(board, pos, color, false, true);
                case ROOK   -> generateSlidingMoves(board, pos, color, true, false);
                case QUEEN  -> generateSlidingMoves(board, pos, color, true, true);
                case KING   -> generateKingMoves(board, pos, color);
            });
        }
        return moves;
    }

    // ── Piece Move Generators ─────────────────────────────────────────────────────

    private List<Move> generatePawnMoves(Board board, Position pos, Color color) {
        List<Move> moves = new ArrayList<>();
        int dir       = color == Color.WHITE ? -1 : 1;
        int startRow  = color == Color.WHITE ? 6 : 1;
        int promoRow  = color == Color.WHITE ? 0 : 7;

        // Single push
        Position one = pos.offset(dir, 0);
        if (one.isValid() && board.getPiece(one) == null) {
            addPawnMove(moves, pos, one, promoRow);
            // Double push from starting rank
            if (pos.getRow() == startRow) {
                Position two = pos.offset(2 * dir, 0);
                if (board.getPiece(two) == null) {
                    moves.add(new Move(pos, two, Move.MoveType.NORMAL));
                }
            }
        }

        // Captures (diagonal)
        for (int dc : new int[]{-1, 1}) {
            Position cap = pos.offset(dir, dc);
            if (!cap.isValid()) continue;
            Piece target = board.getPiece(cap);
            if (target != null && target.getColor() != color) {
                addPawnMove(moves, pos, cap, promoRow);
            }
            // En passant
            Position ep = board.getEnPassantTarget();
            if (ep != null && ep.equals(cap)) {
                moves.add(new Move(pos, cap, Move.MoveType.EN_PASSANT));
            }
        }
        return moves;
    }

    private void addPawnMove(List<Move> moves, Position from, Position to, int promoRow) {
        if (to.getRow() == promoRow) {
            for (PieceType pt : new PieceType[]{PieceType.QUEEN, PieceType.ROOK,
                                                PieceType.BISHOP, PieceType.KNIGHT}) {
                moves.add(new Move(from, to, Move.MoveType.PROMOTION, pt));
            }
        } else {
            moves.add(new Move(from, to, Move.MoveType.NORMAL));
        }
    }

    private List<Move> generateKnightMoves(Board board, Position pos, Color color) {
        List<Move> moves = new ArrayList<>();
        int[][] offsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] o : offsets) {
            Position to = pos.offset(o[0], o[1]);
            if (!to.isValid()) continue;
            Piece target = board.getPiece(to);
            if (target == null || target.getColor() != color) {
                moves.add(new Move(pos, to, target != null
                        ? Move.MoveType.CAPTURE : Move.MoveType.NORMAL));
            }
        }
        return moves;
    }

    private List<Move> generateSlidingMoves(Board board, Position pos, Color color,
                                             boolean rook, boolean bishop) {
        List<Move> moves = new ArrayList<>();
        int[][] dirs = {};
        if (rook && bishop)  dirs = new int[][]{{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
        else if (rook)       dirs = new int[][]{{0,1},{0,-1},{1,0},{-1,0}};
        else                 dirs = new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}};

        for (int[] d : dirs) {
            Position cur = pos.offset(d[0], d[1]);
            while (cur.isValid()) {
                Piece target = board.getPiece(cur);
                if (target == null) {
                    moves.add(new Move(pos, cur, Move.MoveType.NORMAL));
                } else {
                    if (target.getColor() != color)
                        moves.add(new Move(pos, cur, Move.MoveType.CAPTURE));
                    break; // blocked
                }
                cur = cur.offset(d[0], d[1]);
            }
        }
        return moves;
    }

    private List<Move> generateKingMoves(Board board, Position pos, Color color) {
        List<Move> moves = new ArrayList<>();
        int[][] dirs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        for (int[] d : dirs) {
            Position to = pos.offset(d[0], d[1]);
            if (!to.isValid()) continue;
            Piece target = board.getPiece(to);
            if (target == null || target.getColor() != color) {
                moves.add(new Move(pos, to, target != null
                        ? Move.MoveType.CAPTURE : Move.MoveType.NORMAL));
            }
        }
        // Castling
        Piece king = board.getPiece(pos);
        if (king != null && !king.hasMoved()) {
            int row = pos.getRow();
            // Kingside
            if (board.canCastle(color, true)
                    && board.getPiece(row, 5) == null
                    && board.getPiece(row, 6) == null
                    && !isAttackedBy(board, new Position(row, 4), color.opposite())
                    && !isAttackedBy(board, new Position(row, 5), color.opposite())
                    && !isAttackedBy(board, new Position(row, 6), color.opposite())) {
                moves.add(new Move(pos, new Position(row, 6), Move.MoveType.CASTLING_KINGSIDE));
            }
            // Queenside
            if (board.canCastle(color, false)
                    && board.getPiece(row, 1) == null
                    && board.getPiece(row, 2) == null
                    && board.getPiece(row, 3) == null
                    && !isAttackedBy(board, new Position(row, 4), color.opposite())
                    && !isAttackedBy(board, new Position(row, 3), color.opposite())
                    && !isAttackedBy(board, new Position(row, 2), color.opposite())) {
                moves.add(new Move(pos, new Position(row, 2), Move.MoveType.CASTLING_QUEENSIDE));
            }
        }
        return moves;
    }

    // ── Attack Detection ──────────────────────────────────────────────────────────

    public boolean isAttackedBy(Board board, Position target, Color attacker) {
        for (Position pos : board.getPiecePositions(attacker)) {
            Piece p = board.getPiece(pos);
            if (canAttack(board, p, pos, target)) return true;
        }
        return false;
    }

    private boolean canAttack(Board board, Piece piece, Position from, Position target) {
        int dr = target.getRow() - from.getRow();
        int dc = target.getCol() - from.getCol();
        return switch (piece.getType()) {
            case PAWN -> {
                int dir = piece.getColor() == Color.WHITE ? -1 : 1;
                yield dr == dir && Math.abs(dc) == 1;
            }
            case KNIGHT -> {
                int adr = Math.abs(dr), adc = Math.abs(dc);
                yield (adr == 2 && adc == 1) || (adr == 1 && adc == 2);
            }
            case BISHOP -> Math.abs(dr) == Math.abs(dc) && isClearDiagonal(board, from, target);
            case ROOK   -> (dr == 0 || dc == 0) && isClearLine(board, from, target);
            case QUEEN  -> (Math.abs(dr) == Math.abs(dc) && isClearDiagonal(board, from, target))
                        || ((dr == 0 || dc == 0) && isClearLine(board, from, target));
            case KING   -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1;
        };
    }

    private boolean isClearLine(Board board, Position from, Position to) {
        int dr = Integer.signum(to.getRow() - from.getRow());
        int dc = Integer.signum(to.getCol() - from.getCol());
        Position cur = from.offset(dr, dc);
        while (!cur.equals(to)) {
            if (board.getPiece(cur) != null) return false;
            cur = cur.offset(dr, dc);
        }
        return true;
    }

    private boolean isClearDiagonal(Board board, Position from, Position to) {
        return isClearLine(board, from, to);
    }

    // ── Move Type Enrichment ──────────────────────────────────────────────────────

    /**
     * Given a minimal move (from/to only), determine its actual MoveType.
     * This is needed because Move.fromString() gives us NORMAL by default.
     */
    public Move enrichMoveType(Board board, Move raw) {
        Position from = raw.getFrom();
        Position to   = raw.getTo();
        Piece piece   = board.getPiece(from);
        if (piece == null) return raw;

        // Already typed (castling/en passant/promotion set explicitly)
        if (raw.getType() != Move.MoveType.NORMAL && raw.getType() != Move.MoveType.CAPTURE)
            return raw;

        // Promotion
        int promoRow = piece.getColor() == Color.WHITE ? 0 : 7;
        if (piece.getType() == PieceType.PAWN && to.getRow() == promoRow) {
            PieceType promo = raw.getPromotionPiece() != null ? raw.getPromotionPiece() : PieceType.QUEEN;
            return new Move(from, to, Move.MoveType.PROMOTION, promo);
        }

        // En passant
        Position ep = board.getEnPassantTarget();
        if (piece.getType() == PieceType.PAWN && ep != null && ep.equals(to)) {
            return new Move(from, to, Move.MoveType.EN_PASSANT);
        }

        // Castling
        if (piece.getType() == PieceType.KING) {
            if (to.getCol() - from.getCol() == 2)
                return new Move(from, to, Move.MoveType.CASTLING_KINGSIDE);
            if (from.getCol() - to.getCol() == 2)
                return new Move(from, to, Move.MoveType.CASTLING_QUEENSIDE);
        }

        // Capture
        if (board.getPiece(to) != null)
            return new Move(from, to, Move.MoveType.CAPTURE);

        return raw;
    }
}
