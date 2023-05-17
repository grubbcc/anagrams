package server;

public class Word {
    final String letters;
    final String suffix;
    final String definition;

    Word(String letters, String suffix, String definition) {
        this.letters = letters;
        this.suffix = suffix;
        this.definition = definition;
    }
}
