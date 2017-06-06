package io.cortical.iris.ui.custom.richtext;

public class Word {
    private String word;
    private int start, end;
    public Word(String text, int start, int end) {
        this.word = text;
        this.start = start;
        this.end = end;
    }
    public String getWord() { return word; }
    public int getStart() { return start; }
    public int getEnd() { return end; }
}