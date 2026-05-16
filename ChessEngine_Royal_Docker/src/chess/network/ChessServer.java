package chess.network;

import chess.engine.GameEngine;
import chess.model.Color;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChessServer {
    private static final int PORT = 5555;
    private final GameEngine game = new GameEngine();
    private ObjectOutputStream out1, out2;
    private ObjectInputStream  in1,  in2;

    public void start() throws IOException {
        System.out.println("[Server] Listening on port " + PORT + " ...");
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("[Server] Waiting for Player 1 (WHITE) ...");
        Socket s1 = server.accept();
        out1 = new ObjectOutputStream(s1.getOutputStream());
        in1  = new ObjectInputStream(s1.getInputStream());
        out1.writeObject(new ChessMessage(ChessMessage.Type.ASSIGN_COLOR,"WHITE")); out1.flush();
        System.out.println("[Server] P1 connected. Waiting for Player 2 (BLACK) ...");
        Socket s2 = server.accept();
        out2 = new ObjectOutputStream(s2.getOutputStream());
        in2  = new ObjectInputStream(s2.getInputStream());
        out2.writeObject(new ChessMessage(ChessMessage.Type.ASSIGN_COLOR,"BLACK")); out2.flush();
        System.out.println("[Server] Both players connected. Game starts!");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> handlePlayer(in1, out2, Color.WHITE, "P1"));
        pool.submit(() -> handlePlayer(in2, out1, Color.BLACK, "P2"));
    }

    private void handlePlayer(ObjectInputStream in, ObjectOutputStream relay, Color color, String name) {
        try {
            while (!game.isOver()) {
                ChessMessage msg = (ChessMessage) in.readObject();
                System.out.printf("[Server] %s: %s%n", name, msg);
                switch (msg.getType()) {
                    case MOVE -> {
                        if (game.getCurrentTurn() != color) continue;
                        if (game.makeMove(msg.getPayload())) {
                            relay.writeObject(msg); relay.flush();
                            if (game.isOver()) {
                                String r = game.getStatus();
                                out1.writeObject(new ChessMessage(ChessMessage.Type.GAME_OVER,r)); out1.flush();
                                out2.writeObject(new ChessMessage(ChessMessage.Type.GAME_OVER,r)); out2.flush();
                            }
                        }
                    }
                    case CHAT, REQUEST_DRAW, ACCEPT_DRAW, RESIGN -> { relay.writeObject(msg); relay.flush(); }
                    default -> {}
                }
            }
        } catch (Exception e) { System.out.println("[Server] "+name+" disconnected: "+e.getMessage()); }
    }

    public static void main(String[] args) throws IOException { new ChessServer().start(); }
}
