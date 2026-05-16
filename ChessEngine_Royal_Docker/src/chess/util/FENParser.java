package chess.util;
import chess.model.*;

public class FENParser {
    public static Board fromFEN(String fen) {
        Board board = Board.empty();
        String[] parts = fen.trim().split("\\s+");
        String[] ranks = parts[0].split("/");
        for (int r=0;r<8;r++) {
            int c=0;
            for (char ch : ranks[r].toCharArray()) {
                if (Character.isDigit(ch)) c+=Character.getNumericValue(ch);
                else { board.setPiece(new Position(r,c),charToPiece(ch)); c++; }
            }
        }
        if (parts.length>1 && parts[1].equals("b")) board.setCurrentTurn(Color.BLACK);
        if (parts.length>2) {
            String cr=parts[2];
            board.setCastling(Color.WHITE,true, cr.contains("K"));
            board.setCastling(Color.WHITE,false,cr.contains("Q"));
            board.setCastling(Color.BLACK,true, cr.contains("k"));
            board.setCastling(Color.BLACK,false,cr.contains("q"));
        }
        if (parts.length>3 && !parts[3].equals("-"))
            board.setEnPassantTarget(Position.fromAlgebraic(parts[3]));
        if (parts.length>4) board.setHalfMoveClock(Integer.parseInt(parts[4]));
        if (parts.length>5) board.setFullMoveNumber(Integer.parseInt(parts[5]));
        return board;
    }

    public static String toFEN(Board board) {
        StringBuilder sb = new StringBuilder();
        for (int r=0;r<8;r++) {
            int empty=0;
            for (int c=0;c<8;c++) {
                Piece p=board.getPiece(r,c);
                if (p==null) empty++;
                else { if(empty>0){sb.append(empty);empty=0;} sb.append(pieceToChar(p)); }
            }
            if (empty>0) sb.append(empty);
            if (r<7) sb.append('/');
        }
        sb.append(' ').append(board.getCurrentTurn()==Color.WHITE?'w':'b');
        String cr="";
        if(board.canCastle(Color.WHITE,true)) cr+="K"; if(board.canCastle(Color.WHITE,false)) cr+="Q";
        if(board.canCastle(Color.BLACK,true)) cr+="k"; if(board.canCastle(Color.BLACK,false)) cr+="q";
        sb.append(' ').append(cr.isEmpty()?"-":cr);
        Position ep=board.getEnPassantTarget();
        sb.append(' ').append(ep!=null?ep.toAlgebraic():"-");
        sb.append(' ').append(board.getHalfMoveClock()).append(' ').append(board.getFullMoveNumber());
        return sb.toString();
    }

    private static Piece charToPiece(char c) {
        Color color = Character.isUpperCase(c)?Color.WHITE:Color.BLACK;
        PieceType type = switch(Character.toUpperCase(c)) {
            case 'K'->PieceType.KING; case 'Q'->PieceType.QUEEN; case 'R'->PieceType.ROOK;
            case 'B'->PieceType.BISHOP; case 'N'->PieceType.KNIGHT; case 'P'->PieceType.PAWN;
            default->throw new IllegalArgumentException("Unknown: "+c);
        };
        return new Piece(type,color);
    }

    private static char pieceToChar(Piece p) {
        char c = switch(p.getType()) {
            case KING->'K'; case QUEEN->'Q'; case ROOK->'R';
            case BISHOP->'B'; case KNIGHT->'N'; case PAWN->'P';
        };
        return p.getColor()==Color.WHITE?c:Character.toLowerCase(c);
    }
}
