package chess.util;
import chess.model.*;
import java.io.*;
import java.time.LocalDate;
import java.util.List;

public class PGNWriter {
    public static void write(List<Move> moves, String result, String path) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println("[Event \"Local Chess Game\"]");
            pw.println("[Site \"ChessEngine v1.0\"]");
            pw.println("[Date \"" + LocalDate.now() + "\"]");
            pw.println("[Round \"1\"]");
            pw.println("[White \"Player1\"]");
            pw.println("[Black \"Player2\"]");
            pw.println("[Result \"" + result + "\"]");
            pw.println();
            Board board = new Board();
            StringBuilder line = new StringBuilder();
            int moveNum=1;
            for (int i=0;i<moves.size();i++) {
                Move m=moves.get(i);
                String san=toSAN(board,m);
                board.applyMove(m);
                if (i%2==0) line.append(moveNum++).append(". ");
                line.append(san).append(" ");
                if (line.length()>72) { pw.println(line.toString().trim()); line=new StringBuilder(); }
            }
            if (!line.isEmpty()) pw.print(line.toString().trim()+" ");
            pw.println(result);
        }
        System.out.println("[PGN] Saved to: " + path);
    }

    public static String toSAN(Board board, Move move) {
        if (move.getType()==Move.MoveType.CASTLING_KINGSIDE)  return "O-O";
        if (move.getType()==Move.MoveType.CASTLING_QUEENSIDE) return "O-O-O";
        Piece piece=board.getPiece(move.getFrom()); if(piece==null) return move.toString();
        StringBuilder san=new StringBuilder();
        if (piece.getType()!=PieceType.PAWN)
            san.append(Character.toUpperCase(piece.getType().getSymbol(Color.WHITE).charAt(0)));
        if (piece.getType()!=PieceType.PAWN) san.append(disambiguate(board,move,piece));
        if (move.isCapture()) {
            if (piece.getType()==PieceType.PAWN) san.append(move.getFrom().toAlgebraic().charAt(0));
            san.append('x');
        }
        san.append(move.getTo().toAlgebraic());
        if (move.getType()==Move.MoveType.PROMOTION && move.getPromotionPiece()!=null)
            san.append('=').append(Character.toUpperCase(move.getPromotionPiece().getSymbol(Color.WHITE).charAt(0)));
        Board after=new Board(board); after.applyMove(move);
        chess.engine.MoveGenerator gen=new chess.engine.MoveGenerator();
        if (gen.isCheckmate(after)) san.append('#');
        else if (gen.isInCheck(after,after.getCurrentTurn())) san.append('+');
        return san.toString();
    }

    private static String disambiguate(Board board, Move move, Piece piece) {
        chess.engine.MoveGenerator gen=new chess.engine.MoveGenerator();
        boolean sameFile=false, sameRank=false, conflict=false;
        for (Move m : gen.generateLegalMoves(board)) {
            if (m.equals(move)) continue;
            Piece o=board.getPiece(m.getFrom());
            if (o==null||o.getType()!=piece.getType()||!m.getTo().equals(move.getTo())) continue;
            conflict=true;
            if (m.getFrom().getCol()==move.getFrom().getCol()) sameFile=true;
            if (m.getFrom().getRow()==move.getFrom().getRow()) sameRank=true;
        }
        if (!conflict) return "";
        if (!sameFile) return String.valueOf((char)('a'+move.getFrom().getCol()));
        if (!sameRank) return String.valueOf(8-move.getFrom().getRow());
        return move.getFrom().toAlgebraic();
    }
}
