package io.cortical.iris.view.output;

import io.cortical.iris.view.BackPanel;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class ContextBackPanel extends BackPanel {
    public ContextBackPanel(double spacing) {
        super(spacing);
        
        Text title = new Text("ContextDisplay Usage");
        title.setFont(Font.font("Questrial", FontWeight.BOLD, 18));
        
        Text instr1 = new Text("\u2022 \tThe \"ContextDisplay's\" TreeTable, allows selection or narrowing of contexts for "+
            "which similar terms (as seen on the \"SimilarTerms\" tab and the tooltips of the \"FingerprintDisplay\"), are displayed.");
        
        TextFlow tf1 = new TextFlow(instr1);
        
        Text instr2 = new Text("\u2022 \tClicking on any one of the listed contexts unselects all of the contexts, because only one context " +
           "can be selected at any time - thus requiring another click (2 - clicks), to select one specific context.");
        
        TextFlow tf2 = new TextFlow(instr2);
        
        Text instr3 = new Text("\u2022 \tClicking on the \"all-contexts\" checkbox located at the top of the table (in the header) will select " +
            "all the contexts again, and represents the selection of ");
        
        Text instr4 = new Text("ANY");
        instr4.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr5 = new Text(" context.");
        
        TextFlow tf3 = new TextFlow(instr3, instr4, instr5);
        
        ImageView iv = new ImageView("context_table_header.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf4 = new TextFlow(iv);
        tf4.setTextAlignment(TextAlignment.CENTER);
        
        //--
        
        Text title2 = new Text("\nContext - WindowTitlePane:");
        title2.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr6 = new Text("\u2022 \tEvery Window in IRIS has a ");
        
        Text instr7 = new Text("\"Smart\" (context aware), WindowTitlePane\"");
        instr7.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        ImageView iv2 = new ImageView("context_window_title_pane.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf5 = new TextFlow(instr6, instr7);
        
        TextFlow tf6 = new TextFlow(iv2);
        tf6.setTextAlignment(TextAlignment.CENTER);
        
        Text instr8 = new Text(" in the left control area of the App (Control Pane). These context-aware configuration panels, " +
            "change their contents depending on the state of their associated window. When the \"Context\" tab is selected, the " +
            "title pane shows a \"Range-slider\" which allows the user to select the number and range of \"Contexts\" to retrieve " +
            "from the server.");
        
        TextFlow tf7 = new TextFlow(instr8);
        
        //--
        
        Text title3 = new Text("Context Rangeslider:");
        title3.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        ImageView iv3 = new ImageView("context_rangeslider.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf8 = new TextFlow(iv3);
        tf8.setTextAlignment(TextAlignment.CENTER);
        
        Text instr9 = new Text("\u2022 \tUse the rangeslider to select the number of contexts to retrieve from the server; using the \"start\" " +
            "and \"end\" indexes to determine the start and end locations within the list of contexts. The selected \"window\" of contexts will " +
            "be returned from the server if there are contexts residing within the range selected.");
        
        TextFlow tf9 = new TextFlow(instr9);
        
        Text instr10 = new Text("\u2022 \tIn addition, you can type a number directly into the start-index and end-index index number fields.");
        
        TextFlow tf10 = new TextFlow(instr10);
        
        
        getChildren().addAll(title, tf1, tf2, tf3, tf4, title2, tf5, tf6, tf7, title3, tf8, tf9, tf10);
    }
}
