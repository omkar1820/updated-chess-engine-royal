package chess.engine;

import chess.model.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChessAI {
    private static final int INF        = Integer.MAX_VALUE/2;
    private static final int MATE_SCORE = 100_000;

    private final MoveGenerator      generator = new MoveGenerator();
    private final Evaluator          evaluator = new Evaluator();
    private final TranspositionTable tt        = new TranspositionTable();
    private final int                maxDepth;
    private final AtomicBoolean      stopFlag  = new AtomicBoolean(false);
    private int nodesSearched, ttHits;

    public ChessAI(int depth) { this.maxDepth = depth; }

    public Move findBestMove(Board board) {
        stopFlag.set(false); tt.clear();
        Move bestMove = null;
        for (int depth=1; depth<=maxDepth && !stopFlag.get(); depth++) {
            nodesSearched=0; ttHits=0;
            Move candidate = searchRoot(board, depth);
            if (!stopFlag.get() && candidate!=null) bestMove = candidate;
            System.out.printf("[AI] depth=%d nodes=%d ttHits=%d best=%s%n",depth,nodesSearched,ttHits,bestMove);
        }
        return bestMove;
    }

    private Move searchRoot(Board board, int depth) {
        boolean max = board.getCurrentTurn()==Color.WHITE;
        List<Move> moves = generator.generateLegalMoves(board);
        if (moves.isEmpty()) return null;
        moves.sort((a,b)->Boolean.compare(!a.isCapture(),!b.isCapture()));
        Move best=moves.get(0); int bestScore=max?-INF:INF, alpha=-INF, beta=INF;
        for (Move m : moves) {
            if (stopFlag.get()) break;
            Board c=new Board(board); c.applyMove(m);
            int score=alphaBeta(c,depth-1,alpha,beta,!max);
            if (max&&score>bestScore){bestScore=score;best=m;alpha=Math.max(alpha,score);}
            if (!max&&score<bestScore){bestScore=score;best=m;beta=Math.min(beta,score);}
        }
        return best;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean max) {
        nodesSearched++;
        if (stopFlag.get()) return 0;
        long hash = TranspositionTable.computeHash(board);
        TranspositionTable.Entry e = tt.get(hash);
        if (e!=null && e.depth()>=depth) {
            ttHits++;
            switch(e.type()) {
                case EXACT: return e.score();
                case LOWER_BOUND: alpha=Math.max(alpha,e.score()); if(alpha>=beta) return beta; break;
                case UPPER_BOUND: beta=Math.min(beta,e.score());   if(alpha>=beta) return alpha; break;
            }
        }
        if (generator.isCheckmate(board)) return max?-(MATE_SCORE+depth):(MATE_SCORE+depth);
        if (generator.isStalemate(board)||generator.isDraw50Move(board)) return 0;
        if (depth==0) return quiescence(board,alpha,beta,max);

        List<Move> moves=generator.generateLegalMoves(board);
        moves.sort((a,b)->Boolean.compare(!a.isCapture(),!b.isCapture()));
        int origAlpha=alpha; Move bestMove=null;

        if (max) {
            int maxEval=-INF;
            for(Move m:moves){Board c=new Board(board);c.applyMove(m);int eval=alphaBeta(c,depth-1,alpha,beta,false);
                if(eval>maxEval){maxEval=eval;bestMove=m;}alpha=Math.max(alpha,eval);if(beta<=alpha)break;}
            storeInTT(hash,maxEval,depth,origAlpha,beta,bestMove); return maxEval;
        } else {
            int minEval=INF;
            for(Move m:moves){Board c=new Board(board);c.applyMove(m);int eval=alphaBeta(c,depth-1,alpha,beta,true);
                if(eval<minEval){minEval=eval;bestMove=m;}beta=Math.min(beta,eval);if(beta<=alpha)break;}
            storeInTT(hash,minEval,depth,origAlpha,beta,bestMove); return minEval;
        }
    }

    private int quiescence(Board board, int alpha, int beta, boolean max) {
        nodesSearched++;
        int standPat=evaluator.evaluate(board);
        if (max) { if(standPat>=beta) return beta; alpha=Math.max(alpha,standPat); }
        else     { if(standPat<=alpha) return alpha; beta=Math.min(beta,standPat); }
        List<Move> caps=generator.generateLegalMoves(board).stream().filter(Move::isCapture).toList();
        if (max) {
            for(Move m:caps){Board c=new Board(board);c.applyMove(m);alpha=Math.max(alpha,quiescence(c,alpha,beta,false));if(beta<=alpha)break;}
            return alpha;
        } else {
            for(Move m:caps){Board c=new Board(board);c.applyMove(m);beta=Math.min(beta,quiescence(c,alpha,beta,true));if(beta<=alpha)break;}
            return beta;
        }
    }

    private void storeInTT(long hash,int score,int depth,int origAlpha,int beta,Move best){
        TranspositionTable.EntryType type;
        if(score<=origAlpha)      type=TranspositionTable.EntryType.UPPER_BOUND;
        else if(score>=beta)      type=TranspositionTable.EntryType.LOWER_BOUND;
        else                      type=TranspositionTable.EntryType.EXACT;
        tt.put(hash,score,depth,type,best);
    }

    public void stop() { stopFlag.set(true); }
    public int getNodesSearched() { return nodesSearched; }
    public int getTTHits()        { return ttHits; }
}
