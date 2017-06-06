package io.cortical.iris.view.output;

import io.cortical.iris.view.BackPanel;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class SimilarTermsBackPanel extends BackPanel {
    public SimilarTermsBackPanel(double spacing) {
        super(spacing);
        
        Text title = new Text("SimilarTermsDisplay Usage");
        title.setFont(Font.font("Questrial", FontWeight.BOLD, 18));
        
        Text instr1 = new Text("\u2022 \tThe \"SimilarTermsDisplay\" consists of two parts; the read-only display of selected contexts, " +
            "and the pane containing the SimilarTerms Bubbles. Each similar terms query contains the setting of ");
        
        Text instr2 = new Text("either one selected context");
        instr2.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr3 = new Text(" or the setting of ");
        
        Text instr4 = new Text("\"ANY\"");
        instr4.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr5 = new Text(" context.");
        
        TextFlow tf = new TextFlow(instr1, instr2, instr3, instr4, instr5);
        
        //--
        
        Text title2 = new Text("\nContext List (Read-Only):");
        title2.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr6 = new Text("\u2022 \tThe \"Context List\" is read-only because its contents are set in the \"Contexts\" tab - and " +
            "cannot be altered here in the \"SimilarTerms\" tab. This list is sync'd with the selection in the \"Contexts\" tab, and displays " +
            "either a single selected context, or all contexts (meaning \"ANY\" context).");
        
        TextFlow tf2 = new TextFlow(instr6);
        
        //--
        
        Text title3 = new Text("\nSimilarTerms:");
        title3.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr7 = new Text("\u2022 \tHere you'll find a list of similar terms which appear within the selected context(s). " +
            "This panel is similar to the InputWindow's \"Expression\" tab in that terms appear in bubbles which can be selected.");
        
        TextFlow tf3 = new TextFlow(instr7);
        
        ImageView iv = new ImageView("similar_term_selected.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf4 = new TextFlow(iv);
        tf4.setTextAlignment(TextAlignment.CENTER);
        
        Text instr8 = new Text("\n\u2022 \tEach selected term is also highlighted in the \"FingerprintDisplay\" of the \"Fingerprint\" tab. Customizing the " +
            "colors, allows each similar term to be distinguished within that display.");

        TextFlow tf5 = new TextFlow(instr8);
        
        ImageView iv2 = new ImageView("similar_term_custom_color.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf6 = new TextFlow(iv2);
        tf6.setTextAlignment(TextAlignment.CENTER);
        
        Text instr9 = new Text("\n...selecting a color...\n\n");
        
        ImageView iv3 = new ImageView("similar_terms_custom_color_selection.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf7 = new TextFlow(instr9, iv3);
        tf7.setTextAlignment(TextAlignment.CENTER);
        
        Text instr11= new Text("\n...as seen on the \"Fingerprints\" tab...\n\n");
        
        ImageView iv5 = new ImageView("similar_term_custom_color_fp.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf10 = new TextFlow(instr11, iv5);
        tf10.setTextAlignment(TextAlignment.CENTER);
        
        //--
        
        Text title4 = new Text("\nPOS Type:");
        title4.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr10 = new Text("\u2022 \tIn addition to \"Contexts\", Similar Terms can also be filtered by POS (Part of Speech) Type. " +
            "Selecting a POS Type such as \"verb\" or \"noun\" will filter the similar terms returned such that they only include the " +
            "selected type.");
        
        TextFlow tf8 = new TextFlow(instr10);
        
        ImageView iv4 = new ImageView("similar_term_pos_selection.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf9 = new TextFlow(iv4);
        tf9.setTextAlignment(TextAlignment.CENTER);
        
        //--
        
        Text title5 = new Text("\nSimilar Terms - WindowTitlePane:");
        title5.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr12 = new Text("\u2022 \tEvery Window in IRIS has a ");
        
        Text instr13 = new Text("\"Smart\" (context aware), WindowTitlePane\"");
        instr13.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        ImageView iv6 = new ImageView("simterms_window_title_pane.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf16 = new TextFlow(instr12, instr13);
        
        TextFlow tf11 = new TextFlow(iv6);
        tf11.setTextAlignment(TextAlignment.CENTER);
        
        Text instr14 = new Text(" in the left control area of the App (Control Pane). These context-aware configuration panels, " +
            "change their contents depending on the state of their associated window. When the \"Similar Terms\" tab is selected, the " +
            "title pane shows a \"Range-slider\" which allows the user to select the number and range of \"Similar Terms\" to retrieve " +
            "from the server.");
        
        TextFlow tf12 = new TextFlow(instr14);
        
        //--
        
        Text title6 = new Text("\nSimilar Terms Rangeslider:");
        title6.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        ImageView iv7 = new ImageView("simterms_rangeslider.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf13 = new TextFlow(iv7);
        tf13.setTextAlignment(TextAlignment.CENTER);
        
        Text instr15 = new Text("\u2022 \tUse the rangeslider to select the number of similar terms to retrieve from the server; using the \"start\" " +
            "and \"end\" indexes to determine the start and end locations within the list of contexts. The selected \"window\" of similar terms will " +
            "be returned from the server if there are similar terms residing within the range selected.");
        
        TextFlow tf14 = new TextFlow(instr15);
        
        Text instr16 = new Text("\u2022 \tIn addition, you can type a number directly into the start-index and end-index index number fields.");
        
        TextFlow tf15 = new TextFlow(instr16);
        
        
        getChildren().addAll(title, tf, title2, tf2, title3, tf3, tf4, tf5, tf6, tf7, tf10, title4, tf8, tf9, title5, tf16, tf11, tf12, title6, tf13, tf14, tf15);
    }
}
