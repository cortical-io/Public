package io.cortical.iris.view.input.text;

import io.cortical.iris.view.BackPanel;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class TextBackPanel extends BackPanel {
    
    /**
     * Constructs a new {@code TextBackPanel}
     * @param spacing   vertical spacing between child nodes
     */
    public TextBackPanel(double spacing) {
        super(spacing);
        
        Text title = new Text("TextDisplay Usage");
        title.setFont(Font.font(title.getFont().getFamily(), FontWeight.BOLD, 18));
        
        Text instr1 = new Text("\n\u2022 \tText may be entered either by typing, then clicking ");
        instr1.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv = new ImageView("validate.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        Text instr2 = new Text(" or by pasting text in the \"Input Text Area\" by typing <CTRL> + <V> or " +
            "by clicking the \"Paste\" button ");
        
        ImageView iv2 = new ImageView("paste_button.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf = new TextFlow(instr1, iv, instr2, iv2);
        
        //--
        
        Text title2 = new Text("Language Detection");
        title2.setFont(Font.font(title.getFont().getFamily(), FontWeight.BOLD, 18));
        
        Text instr3 = new Text("The automatic language detection feature requires at least 10 words or 40-50 characters " +
            "in order to process the inserted text and choose the appropriate language. For text less than that, you will " +
            "have to select the language from the drop-down choice box. Due to cultural inter-mixing, some language Retinas " +
            "will accept words from different languages, however some will not and you may receive an error message.\n\n");
        
        Text instr7 = new Text("NOTE:");
        instr7.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr4 = new Text(" In order to see similar terms in the \"Fingerprint\" tab of a given ");
        instr4.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr5 = new Text("connected");
        instr5.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 12));
        
        Text instr6 = new Text(" OutputWindow, you will have to have that language selected or automatically detected.");
        instr6.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf2 = new TextFlow(instr3, instr7, instr4, instr5, instr6);
        
        getChildren().addAll(title, tf, title2, tf2);
    }
}
