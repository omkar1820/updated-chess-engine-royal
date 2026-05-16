package chess.ui;
import chess.engine.*;
import chess.model.*;
import java.util.Scanner;

public class ConsoleUI {
    private final GameEngine game=new GameEngine();
    private ChessAI ai=null;
    private Color   aiColor=null;

    public void run() {
        Scanner sc=new Scanner(System.in);
        System.out.println("╔════════════════════════════════╗");
        System.out.println("║    Java Chess Engine v1.0      ║");
        System.out.println("╠════════════════════════════════╣");
        System.out.println("║  1) Human vs Human             ║");
        System.out.println("║  2) Human vs AI                ║");
        System.out.println("╚════════════════════════════════╝");
        System.out.print("Mode: ");
        String mode=sc.nextLine().trim();
        if (mode.equals("2")) {
            System.out.print("Play as (W/B): ");
            aiColor=sc.nextLine().trim().equalsIgnoreCase("W")?Color.BLACK:Color.WHITE;
            System.out.print("AI depth (1-5, default 3): ");
            String ds=sc.nextLine().trim();
            int depth=ds.isEmpty()?3:Integer.parseInt(ds);
            ai=new ChessAI(depth);
            System.out.println("AI is " + aiColor + " at depth " + depth);
        }
        System.out.println("\nCommands: <move>  resign  moves  fen  save  help\n");
        printBoard();
        while (!game.isOver()) {
            Color turn=game.getCurrentTurn();
            System.out.println("  "+game.getStatus());
            if (ai!=null&&turn==aiColor) {
                System.out.println("  AI thinking...");
                Move aiMove=ai.findBestMove(game.getBoard());
                if (aiMove==null) break;
                game.makeMove(aiMove); System.out.println("  AI: "+aiMove); printBoard(); continue;
            }
            System.out.print(turn+" > ");
            String input=sc.nextLine().trim().toLowerCase();
            switch (input) {
                case "resign" -> { System.out.println(turn+" resigns!"); return; }
                case "moves"  -> { game.getLegalMoves().forEach(m->System.out.print(m+" ")); System.out.println(); }
                case "fen"    -> System.out.println("  FEN: "+game.toFEN());
                case "save"   -> {
                    try { String p="game_"+System.currentTimeMillis()+".pgn"; game.savePGN(p); System.out.println("  Saved: "+p); }
                    catch(Exception e) { System.out.println("  Error: "+e.getMessage()); }
                }
                case "help"   -> System.out.println("  Move: e2e4 | Promotion: e7e8q | resign | moves | fen | save");
                default -> { if(!game.makeMove(input)) System.out.println("  Illegal move."); else printBoard(); }
            }
        }
        System.out.println("\n"+game.getStatus());
        System.out.print("History: "); game.getHistory().forEach(m->System.out.print(m+" ")); System.out.println();
    }

    private void printBoard() { System.out.println(game.getBoard().toDisplay()); }
}
