package chess.util;
import chess.model.Move;
import java.util.*;

public class OpeningBook {
    private static final Map<String,List<String>> BOOK=new HashMap<>();
    static {
        put("",           "e2e4","d2d4","c2c4","g1f3");
        put("e2e4",       "e7e5","c7c5","e7e6","c7c6","d7d5");
        put("e2e4,e7e5",  "g1f3","f2f4","d2d4","b1c3");
        put("e2e4,e7e5,g1f3","b8c6","g8f6","f8c5","d7d6");
        put("e2e4,c7c5",  "g1f3","b1c3","d2d4");
        put("e2e4,c7c5,g1f3","d7d6","b8c6","e7e6");
        put("d2d4",       "d7d5","g8f6","f7f5","e7e6");
        put("d2d4,d7d5",  "c2c4","b1c3","g1f3");
        put("d2d4,d7d5,c2c4","e7e6","c7c6","d5c4","g8f6");
        put("d2d4,g8f6",  "c2c4","g1f3","b1c3");
        put("d2d4,g8f6,c2c4","g7g6","e7e6","c7c5");
        put("d2d4,g8f6,c2c4,g7g6","b1c3","g2g3","g1f3");
    }
    private static void put(String key, String... r) { BOOK.put(key,Arrays.asList(r)); }

    public String lookup(List<Move> history) {
        List<String> opts=BOOK.get(buildKey(history));
        if (opts==null||opts.isEmpty()) return null;
        return opts.get(new Random().nextInt(opts.size()));
    }
    public boolean inBook(List<Move> history) { return BOOK.containsKey(buildKey(history)); }
    private String buildKey(List<Move> h) {
        if (h.isEmpty()) return "";
        StringBuilder sb=new StringBuilder();
        for (Move m:h) sb.append(m.toString()).append(',');
        return sb.substring(0,sb.length()-1);
    }
}
