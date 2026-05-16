package chess.util;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class PGNReader {
    private final Map<String,String> tags  = new LinkedHashMap<>();
    private final List<String>       moves = new ArrayList<>();

    public PGNReader(String path) throws IOException {
        StringBuilder moveText=new StringBuilder();
        try (BufferedReader br=new BufferedReader(new FileReader(path))) {
            String line;
            while ((line=br.readLine())!=null) {
                line=line.trim();
                if (line.startsWith("[")) {
                    Matcher m=Pattern.compile("\\[(\\w+)\\s+\"([^\"]+)\"]").matcher(line);
                    if (m.matches()) tags.put(m.group(1),m.group(2));
                } else if (!line.isEmpty()) moveText.append(line).append(' ');
            }
        }
        String clean=moveText.toString()
            .replaceAll("\\{[^}]*}","").replaceAll("\\$\\d+","")
            .replaceAll("1-0|0-1|1/2-1/2|\\*","").trim();
        for (String token : clean.split("\\s+")) {
            if (token.isEmpty()||token.matches("\\d+\\.+")) continue;
            moves.add(token);
        }
    }

    public Map<String,String> getTags()  { return Collections.unmodifiableMap(tags); }
    public List<String>       getMoves() { return Collections.unmodifiableList(moves); }
    public String getTag(String key)     { return tags.getOrDefault(key,"?"); }
    @Override public String toString()   { return "PGN["+getTag("White")+" vs "+getTag("Black")+", "+getTag("Result")+", moves="+moves.size()+"]"; }
}
