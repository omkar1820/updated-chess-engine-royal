package chess.ui;
import chess.network.*;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner sc=new Scanner(System.in);
        System.out.println("┌─────────────────────────────────┐");
        System.out.println("│   Chess Engine v1.0 - Startup   │");
        System.out.println("├─────────────────────────────────┤");
        System.out.println("│  1) Local game                  │");
        System.out.println("│  2) Start network server        │");
        System.out.println("│  3) Connect as network client   │");
        System.out.println("└─────────────────────────────────┘");
        System.out.print("Choose: ");
        switch(sc.nextLine().trim()) {
            case "1" -> new ConsoleUI().run();
            case "2" -> new ChessServer().start();
            case "3" -> new ChessClient().connect();
            default  -> System.out.println("Invalid choice.");
        }
    }
}
