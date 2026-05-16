package chess.engine;

import chess.model.*;
import java.util.HashMap;

public class TranspositionTable {
    public enum EntryType { EXACT, LOWER_BOUND, UPPER_BOUND }
    public record Entry(int score, int depth, EntryType type, Move bestMove) {}

    private static final long[][][] ZOBRIST_PIECES  = new long[6][2][64];
    private static final long        ZOBRIST_BLACK;
    private static final long[]      ZOBRIST_CASTLE = new long[4];
    private static final long[]      ZOBRIST_EP     = new long[8];

    static {
        java.util.Random rng = new java.util.Random(0xDEADBEEFL);
        for (int pt=0;pt<6;pt++) for(int c=0;c<2;c++) for(int sq=0;sq<64;sq++)
            ZOBRIST_PIECES[pt][c][sq] = rng.nextLong();
        ZOBRIST_BLACK = rng.nextLong();
        for (int i=0;i<4;i++) ZOBRIST_CASTLE[i] = rng.nextLong();
        for (int i=0;i<8;i++) ZOBRIST_EP[i]     = rng.nextLong();
    }

    private final HashMap<Long,Entry> table = new HashMap<>(1<<20);

    public void put(long hash, int score, int depth, EntryType type, Move best) {
        Entry e = table.get(hash);
        if (e == null || depth >= e.depth()) table.put(hash, new Entry(score,depth,type,best));
    }
    public Entry get(long hash) { return table.get(hash); }
    public void clear() { table.clear(); }

    public static long computeHash(Board board) {
        long hash = 0L;
        for (int r=0;r<8;r++) for(int c=0;c<8;c++) {
            Piece p = board.getPiece(r,c);
            if (p != null) hash ^= ZOBRIST_PIECES[p.getType().ordinal()][p.getColor().ordinal()][r*8+c];
        }
        if (board.getCurrentTurn()==Color.BLACK) hash ^= ZOBRIST_BLACK;
        if (board.canCastle(Color.WHITE,true))   hash ^= ZOBRIST_CASTLE[0];
        if (board.canCastle(Color.WHITE,false))  hash ^= ZOBRIST_CASTLE[1];
        if (board.canCastle(Color.BLACK,true))   hash ^= ZOBRIST_CASTLE[2];
        if (board.canCastle(Color.BLACK,false))  hash ^= ZOBRIST_CASTLE[3];
        Position ep = board.getEnPassantTarget();
        if (ep != null) hash ^= ZOBRIST_EP[ep.getCol()];
        return hash;
    }
}
