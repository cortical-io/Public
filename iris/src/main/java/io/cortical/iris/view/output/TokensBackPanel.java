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

public class TokensBackPanel extends BackPanel {

    public TokensBackPanel(double spacing) {
        super(spacing);
        
        Text title = new Text("TokensDisplay Usage");
        title.setFont(Font.font("Questrial", FontWeight.BOLD, 18));
        
        /*
         Given an input text this method returns a list of sentences (each of which is a comma-separated list of tokens). 
         Part of speech tags (POStags) can be given in a comma-separated list. If the POStags parameter is left blank, terms of all types are retrieved, 
         otherwise this method will return only the terms corresponding to the requested parts of speech. 
         The tags are described in more detail in the cortical.io Documentation website.
         
         TextFlow desc = new TextFlow();
        desc.getChildren().add(new Text(
            "The /text/tokenize endpoint accepts POS tags from a universal POS tag set. As an aid to determining " +
            "which POS tag(s) you need, the following is a table showing the mapping from the universal set of POS tags to the"));
        pane.layoutBoundsProperty().addListener((v,o,n) -> {
            desc.setPrefWidth(n.getWidth() - 55);
        });
        
        Hyperlink link = new Hyperlink("PENN TREE (Gate style) POS tag set.");
        link.setOnAction(e -> {
            ApplicationService.getInstance().getHostServices().showDocument("https://gate.ac.uk/sale/tao/splitap7.html#x39-789000G");
            ((Hyperlink)e.getSource()).setVisited(false);
        });
        desc.getChildren().add(link);
         
         * 
         */
        
        Text instr = new Text("\u2022 \tThe \"TokensDisplay\" uses the submitted input text to display a list of sentences (each of which is a comma-separated list of tokens).");
        
        Text instr3 = new Text("\n\nGiven the following text:");
        instr3.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        TextFlow tf = new TextFlow(instr, instr3);
        
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
        
        ImageView iv = new ImageView("tokens_display_example.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf3 = new TextFlow(iv);
        tf3.setTextAlignment(TextAlignment.CENTER);
        
        Text instr6 = new Text("\nPOSTag Selector");
        instr6.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr7 = new Text("\u2022 \tMultiple Part-of-Speech tags (POSTags) can be specified using the \"POSTag Selector\". If no POSTags parameter is selected or the \"Check-all\" " +
            "checkbox (located at the top of the first column in the POSTags table), is checked, terms of all types are displayed; otherwise this screen will return only the terms corresponding " +
            "to the requested parts of speech.");
        
        TextFlow tf4 = new TextFlow(instr7);
        
        Text instr8 = new Text("\u2022 \tTo enable Part-of-Speech filtering, click the ");
        
        Text instr9 = new Text("\"Show POS Tags to filter tokens\"");
        instr9.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr10 = new Text(" button:");
        
        TextFlow tf5 = new TextFlow(instr8, instr9, instr10);
        
        ImageView iv2 = new ImageView("tokens_display_show_pos_button.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf6 = new TextFlow(iv2);
        tf6.setTextAlignment(TextAlignment.CENTER);
        
        Text spacer = new Text("...to reveal the available POSTags.");
        TextFlow spaceFlow = new TextFlow(spacer);
        spaceFlow.setTextAlignment(TextAlignment.CENTER);
        
        ImageView iv3 = new ImageView("tokens_display_hide_pos.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf7 = new TextFlow(iv3);
        tf7.setTextAlignment(TextAlignment.CENTER);
        
        Text instr11 = new Text("\n\n\u2022 \tSelect the \"Paging Dots\" to page through the available Part-of-Speech tags:");
        
        TextFlow tf9 = new TextFlow(instr11);
        
        ImageView iv4 = new ImageView("tokens_display_pos_tags_page2.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf8 = new TextFlow(iv4);
        tf8.setTextAlignment(TextAlignment.CENTER);
        
        
        
        
        getChildren().addAll(title, tf, tf2, instr5, tf3, instr6, tf4, tf5, tf6, spaceFlow, tf7, tf9, tf8);
    }
}
