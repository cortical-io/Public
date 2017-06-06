package io.cortical.iris.ui.custom.richtext;


import static org.fxmisc.richtext.TwoDimensional.Bias.Backward;
import static org.fxmisc.richtext.TwoDimensional.Bias.Forward;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyleSpans;
import org.reactfx.SuspendableNo;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;


/**
 * Special One-off Rich Text Editor for displaying text with controls for
 * user-editable styling.
 * 
 * @author Thomas Mikula
 * @author cogmission
 */
public class RichTextArea extends VBox {
    
    protected Button applyBtn;
    protected ColorPicker textColorPicker;
    protected ComboBox<String> familyCombo;
    protected ComboBox<Integer> sizeCombo;
    
    protected boolean isPromptMode;
    protected String promptString;
    
    
    protected StringProperty readyProperty = new SimpleStringProperty();
    protected StringProperty textProperty = new SimpleStringProperty();
    protected BooleanProperty promptState = new SimpleBooleanProperty();
    
    protected StyledTextArea<ParStyle, TextStyle> area;
    
    protected final SuspendableNo updatingToolbar = new SuspendableNo();
    
    protected ChangeListener<String> textListener;
    protected Integer savedFontSize;
    protected Color savedFontColor;
     
    
    
    public RichTextArea(boolean includeControls) {
        super(5);
        
        this.area = createTextArea(includeControls);
        
        VirtualizedScrollPane<StyledTextArea<ParStyle, TextStyle>> vsPane = new VirtualizedScrollPane<>(area);
        VBox.setVgrow(vsPane, Priority.ALWAYS);
        
        if(includeControls) {
            lengthProperty().addListener((v,o,n) -> {
                setSendEnabled(n > 0 && !promptState.get());
            });
            
            getStyleClass().addAll("input-text-area");
            getChildren().addAll(createPanel1(), vsPane);
        }else{
            getStyleClass().addAll("input-text-no-controls-area");
            getChildren().addAll(vsPane);
        }
    }
    
    public StyledTextArea<ParStyle, TextStyle> getStyledArea() {
        return this.area;
    }
    
    public StyledTextArea<ParStyle, TextStyle> createTextArea(boolean includeControls) {
        StyledTextArea<ParStyle, TextStyle> area = new StyledTextArea<ParStyle, TextStyle>(
            ParStyle.EMPTY, ( paragraph, style) -> paragraph.setStyle(style.toCss()),
                TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Serif").updateTextColor(Color.BLACK),
                    ( text, style) -> text.setStyle(style.toCss())) {
        };
        area.setWrapText(true);
        area.setStyleCodecs(ParStyle.CODEC, TextStyle.CODEC);
        area.getStyleClass().addAll(includeControls ? "rich-text-area" : "rich-text-area-no-controls");
        return area;
    }
    
    /**
     * Enables or Disables the send button, altering its CSS to display
     * either look.
     * @param b
     */
    public void setSendEnabled(boolean b) {
        if(b) {
            applyBtn.getStyleClass().removeAll("apply-unset");
            applyBtn.getStyleClass().addAll("apply-set");
            applyBtn.setDisable(false);
        }else{
            applyBtn.getStyleClass().removeAll("apply-set");
            applyBtn.getStyleClass().addAll("apply-unset");
            applyBtn.setDisable(true);
        }
    }
    
    /**
     * Sets the string to be used as a prompt string.
     * @param usePrompt
     */
    public void setPromptString(String prompt) {
        this.isPromptMode = prompt != null && prompt.length() > 5;
        
        this.promptString = prompt;
        
        if(textListener != null) {
            textProperty.removeListener(textListener);
        } else {
            promptState.addListener((v,o,n) -> {
                if(n) {
                    setText(promptString);
                    area.selectAll();
                    updateTextColor(Color.rgb(170, 170, 170));
                    updateFontSize(10);
                    area.selectRange(0, 0);
                } else {
                    clear();
                }
            });
        }
        
        if(!isPromptMode) {
            return;
        }
        
        if(area.getText() == null || area.getText().isEmpty()) {
            promptState.set(true);
        }
    }
    
    /**
     * Returns the button used to invoke "apply" action(s).
     * @return
     */
    public Button getApplyButton() {
        return applyBtn;
    }
    
    public void setText(String text) {
        replace(0, getLength(), text);
    }
    
    public void setEditable(boolean b) {
        area.setEditable(b);
    }
    
    public Color getBackgroundColor() {
        return (Color)area.getBackground().getFills().get(0).getFill();
    }
    
    public int getLength() {
        return area.getLength();
    }
    
    public void replace(int start, int end, String text) {
        area.replaceText(start, end, text);
        textProperty.set(area.getText());
    }
    
    public void clear() {
        area.clear();
        area.clearStyle(0, area.getText().length());
        textProperty.set(null);
        readyProperty.set("");
    }
    
    public void apply() {
        readyProperty.set(area.getText());
        setSendEnabled(false);
    }
    
    public StringProperty readyProperty() {
        return readyProperty;
    }
    
    public StringProperty textProperty() {
        return textProperty;
    }
    
    public ObservableValue<Integer> lengthProperty() {
        return area.lengthProperty();
    }
    
    /**
     * Returns a list containing all occurrences of the search word.
     * (i.e. contains the start and end indexes of the word
     * within this {@code RichTextArea}'s text).
     * @param text  the string to search for
     * @return  a list containing all occurrences of the search word.
     */
    public List<Word> extractWords(String searchWord) {
        List<Word> matches = new ArrayList<>();
        
        boolean isQuoteSurrounded = searchWord.indexOf("\"") == 0 && searchWord.lastIndexOf("\"") == searchWord.length() -1;
        
        String text = area.getText().toLowerCase();
        
        Locale currentLocale = Locale.getDefault();
        BreakIterator wordIterator = 
           BreakIterator.getWordInstance(currentLocale);
        wordIterator.setText(text);
        int start = wordIterator.first();
        int end = wordIterator.next();

        while (end != BreakIterator.DONE) {
            String word = text.substring(start,end);
            if(isQuoteSurrounded) {
                if(text.substring(Math.max(0, start - 1), Math.min(text.length(), end + 1)).equals(searchWord.toLowerCase())) {
                    matches.add(new Word(searchWord, Math.max(0, start - 1), Math.min(text.length(), end + 1)));
                }
            } else if (word.equals(searchWord.toLowerCase())) {
                matches.add(new Word(searchWord, start, end));
            }
            start = end;
            end = wordIterator.next();
        }
        
        return matches;
    }
    
    /**
     * Returns a list containing all occurrences of the search character.
     * (i.e. contains the start and end indexes of the word
     * within this {@code RichTextArea}'s text).
     * @param text  the string to search for
     * @return  a list containing all occurrences of the search word.
     */
    public List<Word> extractChars(String searchChar) {
        List<Word> matches = new ArrayList<>();
        
        String text = area.getText().toLowerCase();
        
        Locale currentLocale = Locale.getDefault();
        BreakIterator charIterator = 
           BreakIterator.getCharacterInstance(currentLocale);
        charIterator.setText(text);
        int boundary = charIterator.first();
        
        while (boundary != BreakIterator.DONE) {
            String word = text.substring(boundary, boundary + 1);
            if (word.equals(searchChar.toLowerCase())) {
                matches.add(new Word(searchChar, boundary, boundary + 1));
            }
            boundary = charIterator.next();
            if(boundary == text.length()) break;
            
        }
        
        return matches;
    }
    
    /**
     * Sets style for the given character range.
     */
    public void setStyle(int from, int to, Color foreground, Color background) {
        TextStyle ts = new TextStyle().updateTextColor(foreground).updateBackgroundColor(background);
        area.setStyle(from, to, ts);
    }
    
    /**
     * Sets style for the given character range.
     */
    public void setStyle(String word, Color foreground, Color background, boolean isBold) {
        TextStyle ts = new TextStyle().updateTextColor(foreground).updateBold(isBold).updateBackgroundColor(background);
        List<Word> matches = extractWords(word);
        matches.stream().forEach(match -> area.setStyle(match.getStart(), match.getEnd(), ts));
    }
    
    /**
     * Sets style for the given character range.
     */
    public void setStyle(String word, Color foreground, Color background) {
        TextStyle ts = new TextStyle().updateTextColor(foreground).updateBackgroundColor(background);
        List<Word> matches = extractWords(word);
        matches.stream().forEach(match -> area.setStyle(match.getStart(), match.getEnd(), ts));
    }
    
    /**
     * Sets style for the given character range.
     */
    public void setCharStyle(String word, Color foreground, Color background) {
        TextStyle ts = new TextStyle().updateTextColor(foreground).updateBold(true).updateBackgroundColor(background);
        List<Word> matches = extractChars(word);
        matches.stream().forEach(match -> area.setStyle(match.getStart(), match.getEnd(), ts));
    }
    
    /**
     * Sets the bold style for the given word.
     * @param word
     * @param isBold
     */
    public void setBoldStyle(String word, boolean isBold) {
        TextStyle ts = new TextStyle().updateBold(isBold);
        List<Word> matches = extractWords(word);
        matches.stream().forEach(match -> area.setStyle(match.getStart(), match.getEnd(), ts));
    }
    
    public double getVirtualHeight() {
        return area.totalHeightEstimateProperty().getOrElse(0.0);
    }
    
    protected HBox createPanel1() {
        double min = 26;
        area.wrapTextProperty().set(true);
        Button undoBtn = createButton("undo", area::undo);
        undoBtn.setMinWidth(min);
        Button redoBtn = createButton("redo", area::redo);
        redoBtn.setMinWidth(min);
        Button cutBtn = createButton("cut", area::cut);
        cutBtn.setMinWidth(min);
        Button copyBtn = createButton("copy", area::copy);
        copyBtn.setMinWidth(min);
        Button pasteBtn = createButton("paste", area::paste);
        pasteBtn.setMinWidth(min);
        applyBtn = createButton("apply-unset", this::apply);
        applyBtn.setDisable(true);
        applyBtn.setMinWidth(30);
        
        sizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 6, 7, 8, 9, 10, 11, 12, 13, 
            14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72));
        sizeCombo.getSelectionModel().select(Integer.valueOf(12));
        sizeCombo.setMinWidth(65);
        sizeCombo.setValue(12);
        sizeCombo.setDisable(false);
        sizeCombo.setOnAction(evt -> {
            updateFontSize(sizeCombo.getValue());
        });
        familyCombo = new ComboBox<>(FXCollections.observableList(Font.getFamilies()));
        familyCombo.getSelectionModel().select("Serif");
        familyCombo.setOnAction(evt -> updateFontFamily(familyCombo.getValue()));
        
        textColorPicker = new ColorPicker(Color.BLACK);
        textColorPicker.setMinWidth(120);
        textColorPicker.setTooltip(new Tooltip("Text color"));
        textColorPicker.valueProperty().addListener((o, old, color) -> updateTextColor(color));
        
        undoBtn.disableProperty().bind(Bindings.not(area.undoAvailableProperty()));
        redoBtn.disableProperty().bind(Bindings.not(area.redoAvailableProperty()));

        BooleanBinding selectionEmpty = new BooleanBinding() {
            { bind(area.selectionProperty()); }

            @Override
            protected boolean computeValue() {
                return area.getSelection().getLength() == 0;
            }
        };
        
        cutBtn.disableProperty().bind(selectionEmpty);
        copyBtn.disableProperty().bind(selectionEmpty);
        
        area.beingUpdatedProperty().addListener(createUpdatePropertyListener());
        
        HBox panel1 = new HBox(3.0);
        panel1.getChildren().addAll(
            applyBtn, undoBtn, redoBtn, cutBtn, copyBtn, pasteBtn,
            sizeCombo, familyCombo, textColorPicker);
                    
        return panel1;
    }
    
    protected Button createButton(String styleClass, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add(styleClass);
        button.setOnAction(evt -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(20);
        button.setPrefHeight(20);
        return button;
    }

    protected ToggleButton createToggleButton(ToggleGroup grp, String styleClass, Runnable action) {
        ToggleButton button = new ToggleButton();
        button.setToggleGroup(grp);
        button.getStyleClass().add(styleClass);
        button.setOnAction(evt -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(20);
        button.setPrefHeight(20);
        return button;
    }
    
    protected void toggleBold() {
        updateStyleInSelection(spans -> TextStyle.bold(!spans.styleStream().allMatch(style -> style.bold.orElse(false))));
    }

    protected void toggleItalic() {
        updateStyleInSelection(spans -> TextStyle.italic(!spans.styleStream().allMatch(style -> style.italic.orElse(false))));
    }

    protected void toggleUnderline() {
        updateStyleInSelection(spans -> TextStyle.underline(!spans.styleStream().allMatch(style -> style.underline.orElse(false))));
    }

    protected void toggleStrikethrough() {
        updateStyleInSelection(spans -> TextStyle.strikethrough(!spans.styleStream().allMatch(style -> style.strikethrough.orElse(false))));
    }

    protected void alignLeft() {
        updateParagraphStyleInSelection(ParStyle.alignLeft());
    }

    protected void alignCenter() {
        updateParagraphStyleInSelection(ParStyle.alignCenter());
    }

    protected void alignRight() {
        updateParagraphStyleInSelection(ParStyle.alignRight());
    }

    protected void alignJustify() {
        updateParagraphStyleInSelection(ParStyle.alignJustify());
    }

    protected void updateStyleInSelection(Function<StyleSpans<TextStyle>, TextStyle> mixinGetter) {
        IndexRange selection = area.getSelection();
        if(selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
            TextStyle mixin = mixinGetter.apply(styles);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    protected void updateStyleInSelection(TextStyle mixin) {
        IndexRange selection = area.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    protected void updateParagraphStyleInSelection(Function<ParStyle, ParStyle> updater) {
        IndexRange selection = area.getSelection();
        int startPar = area.offsetToPosition(selection.getStart(), Forward).getMajor();
        int endPar = area.offsetToPosition(selection.getEnd(), Backward).getMajor();
        for(int i = startPar; i <= endPar; ++i) {
            Paragraph<ParStyle, TextStyle> paragraph = area.getParagraph(i);
            area.setParagraphStyle(i, updater.apply(paragraph.getParagraphStyle()));
        }
    }

    protected void updateParagraphStyleInSelection(ParStyle mixin) {
        updateParagraphStyleInSelection(style -> style.updateWith(mixin));
    }

    protected void updateFontSize(Integer size) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.fontSize(size));
        }
    }

    protected void updateFontFamily(String family) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.fontFamily(family));
        }
    }

    protected void updateTextColor(Color color) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.textColor(color));
        }
    }

    protected void updateBackgroundColor(Color color) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.backgroundColor(color));
        }
    }

    protected void updateParagraphBackground(Color color) {
        if(!updatingToolbar.get()) {
            updateParagraphStyleInSelection(ParStyle.backgroundColor(color));
        }
    }
    
    @SuppressWarnings("unused")
    protected ChangeListener<Boolean> createUpdatePropertyListener() {
        return (o, old, beingUpdated) -> {
            if(!beingUpdated) {
                Integer fontSize;
                String fontFamily;
                Color textColor;
                Color backgroundColor;

                IndexRange selection = area.getSelection();
                if(selection.getLength() != 0) {
                    StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
                    int[] sizes = styles.styleStream().mapToInt(s -> s.fontSize.orElse(-1)).distinct().toArray();
                    fontSize = sizes.length == 1 ? sizes[0] : -1;
                    String[] families = styles.styleStream().map(s -> s.fontFamily.orElse(null)).distinct().toArray(String[]::new);
                    fontFamily = families.length == 1 ? families[0] : null;
                    Color[] colors = styles.styleStream().map(s -> s.textColor.orElse(null)).distinct().toArray(Color[]::new);
                    textColor = colors.length == 1 ? colors[0] : null;
                    Color[] backgrounds = styles.styleStream().map(s -> s.backgroundColor.orElse(null)).distinct().toArray(i -> new Color[i]);
                    backgroundColor = backgrounds.length == 1 ? backgrounds[0] : null;
                } else {
                    int p = area.getCurrentParagraph();
                    int col = area.getCaretColumn();
                    TextStyle style = area.getStyleAtPosition(p, col);
                    fontSize = style.fontSize.orElse(-1);
                    fontFamily = style.fontFamily.orElse(null);
                    textColor = style.textColor.orElse(null);
                    backgroundColor = style.backgroundColor.orElse(null);
                }

                updatingToolbar.suspendWhile(() -> {
                    if(fontSize != -1) {
                        sizeCombo.getSelectionModel().select(fontSize);
                    } else {
                        sizeCombo.getSelectionModel().clearSelection();
                    }

                    if(familyCombo != null) {
                        if(fontFamily != null) {
                            familyCombo.getSelectionModel().select(fontFamily);
                        } else {
                            familyCombo.getSelectionModel().clearSelection();
                        }
                    }

                    if(textColor != null && textColorPicker != null) {
                        textColorPicker.setValue(textColor);
                    }

                });
            }
        };
    }
    
}
