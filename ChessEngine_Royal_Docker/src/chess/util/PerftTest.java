package chess.util;
import chess.engine.MoveGenerator;
import chess.model.*;

public class PerftTest {
    private final MoveGenerator gen=new MoveGenerator();

    public long perft(Board board, int depth) {
        if (depth==0) return 1L;
        long nodes=0L;
        for (Move m:gen.generateLegalMoves(board)) {
            Board c=new Board(board); c.applyMove(m); nodes+=perft(c,depth-1);
        }
        return nodes;
    }

    public void divide(Board board, int depth) {
        long total=0;
        for (Move m:gen.generateLegalMoves(board)) {
            Board c=new Board(board); c.applyMove(m);
            long count=perft(c,depth-1);
            System.out.printf("  %-8s  %,d%n",m,count); total+=count;
        }
        System.out.println("  Total: "+total);
    }

    public static void main(String[] args) {
        PerftTest t=new PerftTest(); Board b=new Board();
        System.out.println("=== Perft Test - Starting Position ===");
        System.out.println("Expected: depth1=20  depth2=400  depth3=8902  depth4=197281");
        System.out.println();
        long[] expected={20L,400L,8902L,197281L};
        for (int d=1;d<=4;d++) {
            long start=System.currentTimeMillis(), result=t.perft(b,d), ms=System.currentTimeMillis()-start;
            System.out.printf("  depth %d -> %,7d  (%3d ms)  %s%n",d,result,ms,result==expected[d-1]?"PASS":"FAIL (expected "+expected[d-1]+")");
        }
    }
}
