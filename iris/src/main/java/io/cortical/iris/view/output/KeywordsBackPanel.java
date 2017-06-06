package io.cortical.iris.view.output;

import io.cortical.iris.view.BackPanel;
import io.cortical.util.ImmutableTextFlow;
import javafx.geometry.Insets;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class KeywordsBackPanel extends BackPanel {
    
    public KeywordsBackPanel(double spacing) {
        super(spacing);
        
        Text title = new Text("KeywordsDisplay Usage");
        title.setFont(Font.font("Questrial", FontWeight.BOLD, 18));
        
        Text instr2 = new Text("\u2022 \tThe \"KeywordsDisplay\" features 2 display areas. The top area displays a \"bubble list\" of keywords from the " +
            "entered text; and the bottom area displays the original text, with the detected keywords highlighted.");
        
        TextFlow tf = new TextFlow(instr2);
        
        Text instr3 = new Text("Given the following text:");
        instr3.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        
        Text instr4 = new Text("\"Hierarchical Temporal Memory implementation in Java - an official Community-Driven Java port of the Numenta Platform for Intelligent Computing (NuPIC).\"");
        instr4.setFont(Font.font("Arial", FontWeight.NORMAL, FontPosture.ITALIC, 11));
        
        TextFlow tf2 = new ImmutableTextFlow(instr4);
        tf2.setMaxWidth(400);
        tf2.setTextAlignment(TextAlignment.LEFT);
        layoutBoundsProperty().addListener((v,o,n) -> {
            double w = (n.getWidth() / 6) * 5;
            tf2.setPadding(new Insets(0, 0, 0, (n.getWidth() / 2) - (w / 2)));
            tf2.setMaxWidth(w);
        });
        
        Text instr5 = new Text("...you will see the following displayed:");
        
        ImageView iv = new ImageView("keywords_example_text.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf3 = new TextFlow(iv);
        tf3.setTextAlignment(TextAlignment.CENTER);
        
        
        Text instr6 = new Text("\n\u2022 \tThe color of the highlighted text, reflects the ");
        
        Text instr7 = new Text("\"Color ID\"");
        instr7.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr8 = new Text(" of the connected \"InputWindow\".");
        
        TextFlow tf4 = new TextFlow(instr6, instr7, instr8);
        
        //--
        
        Text colorTitle = new Text("\nChanging the \"Color ID\" of an \"InputWindow\"");
        colorTitle.setFont(Font.font("Questrial", FontWeight.BOLD, 18));
        
        Text textSteps = new Text("Change the \"Color ID\" in 2 steps:");
        textSteps.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text textStep1 = new Text("\t1. Locate the connected \"InputWindow's\"  \"Color ID\"  Marker.");
        
        ImageView imgTS1 = new ImageView("input_window_id_tab.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow flowStep = new TextFlow(imgTS1);
        flowStep.setTextAlignment(TextAlignment.CENTER);
        
        Text textStep2 = new Text("\t2. Simply select your desired color");
        
        ImageView imgTS2 = new ImageView("input_window_id_tab_changing.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow flowStep2 = new TextFlow(imgTS2);
        flowStep2.setTextAlignment(TextAlignment.CENTER);
        
        Text instr9 = new Text("\n...again, you will see the following displayed:");
        
        ImageView iv3 = new ImageView("keywords_display_highlight_color_change.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf5 = new TextFlow(iv3);
        tf5.setTextAlignment(TextAlignment.CENTER);
        
        
        getChildren().addAll(title, tf, instr3, tf2, instr5, tf3, tf4, colorTitle, textSteps, textStep1, flowStep, textStep2, flowStep2, instr9, tf5);
    }
}
