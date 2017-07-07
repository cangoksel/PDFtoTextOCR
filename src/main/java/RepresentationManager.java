import org.antlr.v4.runtime.Token;
import zemberek.morphology.apps.TurkishMorphParser;
import zemberek.morphology.parser.MorphParse;
import zemberek.tokenizer.SentenceBoundaryDetector;
import zemberek.tokenizer.SimpleSentenceBoundaryDetector;
import zemberek.tokenizer.ZemberekLexer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RepresentationManager {
    private static RepresentationManager instance;
    private final ZemberekLexer lexer;
    private final TurkishMorphParser parser;
    private final HashMap<String, Integer> idfMap;

    public ArrayList<String> detectSentences(String input) {
        SentenceBoundaryDetector detector = new SimpleSentenceBoundaryDetector();
        ArrayList<String> sentences = (ArrayList<String>) detector.getSentences(input);
        return sentences;
    }

    private RepresentationManager() throws IOException {
        parser = TurkishMorphParser.createWithDefaults();
        lexer = new ZemberekLexer();
        idfMap = new HashMap<>();
    }

    public static RepresentationManager instance() throws IOException {
        if (instance == null)
            instance = new RepresentationManager();
        return instance;
    }

    public DocumentRepresentation createRepresentation(String contentText) {
        ArrayList<String> sentences = detectSentences(contentText);
        DocumentRepresentation rVal = new DocumentRepresentation();
        sentences.stream().map((s) -> lexer.getTokenIterator(s)).forEach((Iterator<Token> tokenIterator) -> {
            while (tokenIterator.hasNext()) {
                Token token = tokenIterator.next();
                //type 7 is a known word.
                if (token.getType() == 7) {
                    String t = token.getText();
                    t = t.toLowerCase();
                    rVal.setTotalWords(rVal.getTotalWords() + 1);
                    rVal.getWordCounts().put(t, rVal.getWordCounts().getOrDefault(t, 0) + 1);
                    List<MorphParse> parses = parser.parse(t);
                    if (parses.size() > 0) {
                        t = parses.get(0).getLemma();
                        rVal.getLemmaCounts().put(t, rVal.getLemmaCounts().getOrDefault(t, 0) + 1);
                        idfMap.put(t, idfMap.getOrDefault(t, 0) + 1);
                    }
                }
            }
        });

        return rVal;
    }

    public void incrementIDFs(HashMap<String, Integer> vals) {

        vals.forEach((k, v) -> {
            idfMap.put(k, idfMap.getOrDefault(k, 0) + 1);
        });

    }

    public void decrementIDFs(HashMap<String, Integer> vals) {
        vals.forEach((k, v) -> {
            idfMap.put(k, idfMap.getOrDefault(k, 1) - 1);
            if (idfMap.get(k) <= 0)
                idfMap.remove(k);
        });
    }

    public DocumentRepresentation createRepresentationFromFile(String fileName) throws IOException {
        File f = new File(fileName);
        String text = new String(Files.readAllBytes(Paths.get(f.getPath())), StandardCharsets.UTF_8);
        return createRepresentation(text);
    }


    public HashMap<String, Integer> getIdfMap() {
        return idfMap;
    }

    public int getIDF(String key) {
        return idfMap.getOrDefault(key, 0);
    }

    public void setIDF(String key, int val) {
        idfMap.put(key, val);
    }

    public HashMap<String, Double> getVector(DocumentRepresentation d) {
        Iterator iterator = idfMap.entrySet().iterator();
        HashMap<String, Double> vector = new HashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> me = (Map.Entry) iterator.next();
            int m = (d.getLemmaCounts().getOrDefault(me.getKey(), 0));
            int n = d.getLemmaCounts().size();
            int val = me.getValue();
            double weight = (double) m / (((double) n) * (double) val);
            vector.put(me.getKey(), weight);
        }
        return vector;
    }
}
