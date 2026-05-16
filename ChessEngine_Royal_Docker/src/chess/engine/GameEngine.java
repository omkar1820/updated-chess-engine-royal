package chess.engine;

import chess.model.*;
import java.util.*;

public class GameEngine {
    public enum GameResult { ONGOING, WHITE_WINS, BLACK_WINS, DRAW_STALEMATE, DRAW_50_MOVE }

    private final Board         board     = new Board();
    private final MoveGenerator generator = new MoveGenerator();
    private final List<Move>    history   = new ArrayList<>();
    private GameResult          result    = GameResult.ONGOING;

    public boolean makeMove(String s) {
        try { return makeMove(Move.fromString(s)); }
        catch (IllegalArgumentException e) { return false; }
    }

    public boolean makeMove(Move raw) {
        Move enriched = generator.enrichMoveType(board, raw);
        boolean legal = generator.generateLegalMoves(board).stream().anyMatch(m ->
            m.getFrom().equals(enriched.getFrom()) && m.getTo().equals(enriched.getTo()) &&
            (enriched.getPromotionPiece() == null || enriched.getPromotionPiece() == m.getPromotionPiece())
        );
        if (!legal) return false;
        board.applyMove(enriched);
        history.add(enriched);
        if (generator.isCheckmate(board))
            result = board.getCurrentTurn() == Color.WHITE ? GameResult.BLACK_WINS : GameResult.WHITE_WINS;
        else if (generator.isStalemate(board))  result = GameResult.DRAW_STALEMATE;
        else if (generator.isDraw50Move(board)) result = GameResult.DRAW_50_MOVE;
        return true;
    }

    public boolean isInCheck()         { return generator.isInCheck(board, board.getCurrentTurn()); }
    public List<Move> getLegalMoves()  { return generator.generateLegalMoves(board); }
    public Board getBoard()            { return board; }
    public Color getCurrentTurn()      { return board.getCurrentTurn(); }
    public GameResult getResult()      { return result; }
    public boolean isOver()            { return result != GameResult.ONGOING; }
    public List<Move> getHistory()     { return history; }

    public String toFEN() { return chess.util.FENParser.toFEN(board); }

    public void savePGN(String path) throws java.io.IOException {
        String res = switch (result) {
            case WHITE_WINS -> "1-0"; case BLACK_WINS -> "0-1";
            case DRAW_STALEMATE, DRAW_50_MOVE -> "1/2-1/2"; case ONGOING -> "*";
        };
        chess.util.PGNWriter.write(history, res, path);
    }

    public String getStatus() {
        return switch (result) {
            case ONGOING        -> (isInCheck() ? "CHECK! " : "") + board.getCurrentTurn() + "'s turn";
            case WHITE_WINS     -> "CHECKMATE - White wins!";
            case BLACK_WINS     -> "CHECKMATE - Black wins!";
            case DRAW_STALEMATE -> "DRAW - Stalemate";
            case DRAW_50_MOVE   -> "DRAW - 50-move rule";
        };
    }
}
