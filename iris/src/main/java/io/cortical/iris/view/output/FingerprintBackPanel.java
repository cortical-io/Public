package io.cortical.iris.view.output;

import io.cortical.iris.view.BackPanel;
import io.cortical.util.ImmutableTextFlow;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class FingerprintBackPanel extends BackPanel {
    
    public FingerprintBackPanel(double spacing) {
        super(spacing);
        
        Text title = new Text("FingerprintDisplay Usage");
        title.setFont(Font.font("Questrial", FontWeight.BOLD, 18));
        
        Text instr1 = new Text("\u2022 \tMouse over the positions in the display to\n" +
        "\tto bring up the tooltip for that position.");
        instr1.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv = new ImageView("fingerprint_popup_mouse_up.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf = new TextFlow(iv);
        tf.setTextAlignment(TextAlignment.CENTER);
        
        Text instr2 = new Text("\u2022 \tPress and hold "); 
        instr2.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr8 = new Text("the mouse button after the tooltip is displayed\n" +
        "\tto expand the description to include similar terms for\n\tthat position.");
        instr8.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf7 = new TextFlow(instr2, instr8);
        
        ImageView iv2 = new ImageView("fingerprint_popup_mouse_down.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf2 = new TextFlow(iv2);
        tf2.setTextAlignment(TextAlignment.CENTER);
        
        Text instr4 = new Text("\tWhen the mouse is held down, the tooltip shows the linear position\n\t \"[8501]\" as well " +
            "as the coordinate position - and the similar terms for this position \n\tare listed.");
        instr4.setFont(Font.font("Arial", FontWeight.NORMAL, FontPosture.ITALIC, 11));
        
        ImmutableTextFlow tf3 = new ImmutableTextFlow(instr4);
        tf3.setTextAlignment(TextAlignment.LEFT);
        tf3.setMaxWidth(400);
                
        Text instr3 = new Text("\u2022 \tHold down the mouse for as long as you would like\n\tto examine " +
        "the tooltip information.\n");
        instr3.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr5 = new Text("Colored ID Squares:");
        instr5.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr6 = new Text("\u2022 \tWhen terms are selected on the \"SimilarTerms\" tab...");
        
        ImageView iv3 = new ImageView("similar_term_selected.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf4 = new TextFlow(iv3);
        tf4.setTextAlignment(TextAlignment.CENTER);
        
        Text instr7 = new Text("\n\n...the popup shows the color of that term as an additional color ID Square indicating " +
            "additional terms are present in this position. This is to account for only the top-most color being displayed in " +
            "a given cell/position of the fingerprint.");
        instr6.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf5 = new TextFlow(instr7);
        tf5.setTextAlignment(TextAlignment.LEFT);
        
        ImageView iv4 = new ImageView("fingerprint_popup_with_similar.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf6 = new TextFlow(iv4);
        tf6.setTextAlignment(TextAlignment.CENTER);
        
        //--
        
        Text colorTitle = new Text("\nChanging the \"Color ID\" of an \"InputWindow\"");
        colorTitle.setFont(Font.font("Questrial", FontWeight.BOLD, 18));
        
        Text fpText = new Text("The \"FingerprintDisplay's\" position colors (cells or dots in the display), are also affected by the " +
           "\"Color ID\" of the primary connected \"InputWindow\". This is so that the positions of different inputs can be easily distinguished " +
           "inside the view, allowing the user to determine the inputs contributing to each position in the display.");
        
        TextFlow fpTextFlow = new TextFlow(fpText);
        
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
        
        Text textStep3 = new Text("Done!");
        textStep3.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text textStep4 = new Text("\n...the \"FingerprintDisplay's\" positions color's reflect the change.");
        
        ImageView imgTS3 = new ImageView("fp_id_color_change.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow flowStep3 = new TextFlow(imgTS3);
        flowStep3.setTextAlignment(TextAlignment.CENTER);
        
        getChildren().addAll(title, instr1, tf, tf7, tf2, tf3, instr3, instr5, instr6, tf4, tf5, tf6, colorTitle, fpTextFlow, textSteps, textStep1, flowStep, textStep2, flowStep2, textStep3, textStep4, flowStep3);
    }
}
