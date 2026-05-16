package chess.network;
import java.io.Serializable;
public class ChessMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Type { MOVE, CHAT, ASSIGN_COLOR, GAME_OVER, REQUEST_DRAW, ACCEPT_DRAW, RESIGN, PING }
    private final Type type; private final String payload;
    public ChessMessage(Type t, String p) { this.type=t; this.payload=p; }
    public Type   getType()    { return type; }
    public String getPayload() { return payload; }
    @Override public String toString() { return type+":"+payload; }
}
