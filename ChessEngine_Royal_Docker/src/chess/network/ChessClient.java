package chess.network;

import chess.engine.GameEngine;
import chess.model.Color;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.*;

public class ChessClient {
    private static final String HOST = "localhost";
    private static final int    PORT = 5555;
    private final GameEngine   game  = new GameEngine();
    private Color               myColor;
    private ObjectOutputStream  out;
    private ObjectInputStream   in;
    private volatile boolean    gameOver = false;

    public void connect() throws IOException {
        Socket socket = new Socket(HOST, PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
        System.out.println("[Client] Connected to " + HOST + ":" + PORT);
        try {
            ChessMessage assign = (ChessMessage) in.readObject();
            myColor = Color.valueOf(assign.getPayload());
            System.out.println("[Client] You are: " + myColor);
        } catch (ClassNotFoundException e) { throw new IOException("Protocol error",e); }

        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.submit(this::listenForMessages);

        Scanner sc = new Scanner(System.in);
        System.out.println("Commands: <move e.g. e2e4>  chat <msg>  resign  draw");
        System.out.println(game.getBoard().toDisplay());

        while (!gameOver) {
            if (game.getCurrentTurn() != myColor) { try{Thread.sleep(300);}catch(InterruptedException ignored){} continue; }
            System.out.print(myColor + " > ");
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("chat "))      { send(new ChessMessage(ChessMessage.Type.CHAT, line.substring(5))); }
            else if (line.equals("resign"))    { send(new ChessMessage(ChessMessage.Type.RESIGN, myColor.toString())); gameOver=true; }
            else if (line.equals("draw"))      { send(new ChessMessage(ChessMessage.Type.REQUEST_DRAW, "")); }
            else {
                if (!game.makeMove(line)) { System.out.println("  Illegal move."); continue; }
                send(new ChessMessage(ChessMessage.Type.MOVE, line));
                System.out.println(game.getBoard().toDisplay());
                System.out.println("  " + game.getStatus());
                if (game.isOver()) gameOver=true;
            }
        }
        pool.shutdownNow();
    }

    private void listenForMessages() {
        try {
            while (!gameOver) {
                ChessMessage msg = (ChessMessage) in.readObject();
                switch (msg.getType()) {
                    case MOVE -> {
                        game.makeMove(msg.getPayload());
                        System.out.println("\n[Opponent played: " + msg.getPayload() + "]");
                        System.out.println(game.getBoard().toDisplay());
                        System.out.println("  " + game.getStatus());
                        if (game.isOver()) gameOver=true;
                    }
                    case CHAT     -> System.out.println("\n[Chat]: " + msg.getPayload());
                    case GAME_OVER -> { System.out.println("\n*** GAME OVER: "+msg.getPayload()+" ***"); gameOver=true; }
                    case REQUEST_DRAW -> System.out.println("\n[Opponent offers draw. Type 'draw' to accept.]");
                    case ACCEPT_DRAW  -> { System.out.println("\n[Draw accepted.]"); gameOver=true; }
                    case RESIGN       -> { System.out.println("\n[Opponent resigned. You win!]"); gameOver=true; }
                    default -> {}
                }
            }
        } catch (Exception e) { if(!gameOver) System.out.println("[Client] Disconnected: "+e.getMessage()); }
    }

    private void send(ChessMessage msg) {
        try { out.writeObject(msg); out.flush(); }
        catch (IOException e) { System.out.println("[Client] Send failed: "+e.getMessage()); }
    }

    public static void main(String[] args) throws IOException { new ChessClient().connect(); }
}
