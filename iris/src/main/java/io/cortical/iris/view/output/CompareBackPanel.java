package io.cortical.iris.view.output;

import io.cortical.iris.ApplicationService;
import io.cortical.iris.view.BackPanel;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class CompareBackPanel extends BackPanel {
    
    public CompareBackPanel(double spacing) {
        super(spacing);
        
        Text title = new Text("CompareDisplay Setup ");
        title.setFont(Font.font("Questrial", FontWeight.BOLD, 20));
        
        Text subTitle = new Text("(Enabling the \"Compare\" Button/Tab)");
        subTitle.setFont(Font.font("Arial", FontWeight.NORMAL, FontPosture.ITALIC, 14));
        
        TextFlow titleFlow = new TextFlow(title, subTitle);
        
        Text instr1 = new Text("\u2022 \tThe \"CompareDisplay\" tab can only be activated when there are two \"InputWindow's\" to compare, " +
           "(two sources of input to compare).\n");
        instr1.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        
        TextFlow ttf = new TextFlow(instr1);
        
        Text t = new Text("Enable the Compare Button in 3 Steps:");
        t.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr2 = new Text("\t1. To compare two \"InputWindows\", add a new input window to the window pane by clicking on the ");
        
        Text instr2a = new Text("\"Add new input window\"");
        instr2a.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr2b = new Text(" button");
        
        TextFlow tf = new TextFlow(instr2, instr2a, instr2b);
        
        ImageView iv = new ImageView("add_new_input_button.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf2 = new TextFlow(iv);
        tf2.setTextAlignment(TextAlignment.CENTER);
        
        Text instr3 = new Text("\t2. Connect an additional \"InputWindow\" to your \"OutputWindow\" by locating the \"InputSelector\"...");
        
        TextFlow tf3 = new TextFlow(instr3);
        
        ImageView iv2 = new ImageView("input_selector.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf4 = new TextFlow(iv2);
        tf4.setTextAlignment(TextAlignment.CENTER);
        
        
        Text instr4 = new Text("\t3. Click on the \"InputSelector\" button to open the list of available \"InputWindows\", then select the \"InputWindow\" to which you " +
            "would like to compare your first \"InputWindow's\" results. Once there are two \"InputWindows\" selected, the \"OutputWindow's\" \"Compare\" tab becomes armed, making " +
            "that tab available.");
        
        TextFlow tf5 = new TextFlow(instr4);
        
        ImageView iv3 = new ImageView("input_selector_active.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf8 = new TextFlow(iv3);
        tf8.setTextAlignment(TextAlignment.CENTER);
        
        Text instr11 = new Text("...selected...");
        
        TextFlow tf10 = new TextFlow(instr11);
        tf10.setTextAlignment(TextAlignment.CENTER);
        
        ImageView iv5 = new ImageView("input_selector_selected.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf11 = new TextFlow(iv5);
        tf11.setTextAlignment(TextAlignment.CENTER);
        
        Text done = new Text("Done!\n");
        done.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        //--
        
        Text remember = new Text("Remember:  ");
        remember.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr5 = new Text("Any two \"InputWindow\" tabs (Expression or Text), can be compared to another, and as you enter text into either display, you will see the " +
            "OutputWindow \"react\" by activating its progress meter, indicating that a server query is underway.");
        
        TextFlow tf6 = new TextFlow(remember, instr5);
        
        Text instr6 = new Text("Another important thing to keep in mind is that the \"Compare\" tab is the ");
        
        Text instr7 = new Text("only");
        instr7.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr8 = new Text(" tab that will \"react\" to changes in a secondary \"InputWindow\". All other \"OutputWindow\" tabs only react to changes in the ");
        
        Text instr9 = new Text("primary");
        instr9.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr10 = new Text(" window. However you can easily toggle between primary and secondary windows by clicking on the \"compare to\" secondary button in the \"InputSelector\", " +
            "which switches which window is considered \"primary\". \n");
        
        TextFlow tf7 = new TextFlow(instr6, instr7, instr8, instr9, instr10);
        
        ImageView iv4 = new ImageView("input_selector_selected_swap.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf9 = new TextFlow(iv4);
        tf9.setTextAlignment(TextAlignment.CENTER);
        
        //--
        
        Text title2 = new Text("\nCompareDisplay Usage");
        title2.setFont(Font.font("Questrial", FontWeight.BOLD, 18));
        
        Text instr12 = new Text("The \"CompareDisplay\" has 3 different views:");
        instr12.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr13 = new Text("\u2022 \tThe default view which is the \"Metrics\" view.\n");
        Text instr14 = new Text("\u2022 \tThe \"Fingerprint\" view, which shows 3 comparison fingerprints in a single Impression grid.\n");
        Text instr15 = new Text("\u2022 \tThe \"ComparisonView\" which is a large view containing 3 separate fingerprint Impression grids, " +
           "shown as an overlay over the entire app.\n");
        
        TextFlow tf12 = new TextFlow(instr13, instr14, instr15);
        
        Text instr16 = new Text("As mentioned above, the \"Metrics\" view is the default view you come to at first. The other 2 views are " +
           "accessed by clicking on the 2 buttons at the top of the \"CompareDisplay\"");
        
        TextFlow tf13 = new TextFlow(instr16);
        
        ImageView iv6 = new ImageView("compare_display_top.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf14 = new TextFlow(iv6);
        tf14.setTextAlignment(TextAlignment.CENTER);
        
        //--
        
        Text title3 = new Text("\nMetrics View:");
        title3.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr17 = new Text("\u2022 \tThe main view contains 2 major displays. The multi-directional-axis \"Metrics Chart\", and the \"Overlap Display\", " +
           "which is an interactive spatial percentage display.");
        
        TextFlow tf15 = new TextFlow(instr17);
        
        Text instr18 = new Text("\u2022 \tYou will see that all of the 3 axis in the \"Metrics Chart\" do not increase in size as the two items being compared get more similar. " +
           "Thiis is because one of the units of measure (Cosine Similarity), is ");
        
        Text instr19 = new Text("inversely");
        instr19.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr20 = new Text(" related to the other two. However the direction of \"similarity\" is conceptually illustrated at the top of the chart.");
        
        TextFlow tf16 = new TextFlow(instr18, instr19, instr20);
        
        ImageView iv7 = new ImageView("compare_display_chart.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf17 = new TextFlow(iv7);
        tf17.setTextAlignment(TextAlignment.CENTER);
        
        Text title4 = new Text("\nOverlap Display:");
        title4.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr21 = new Text("\u2022 \tThe \"Overlap Display\" is interactive and will display the spatial comparison metrics which yield a sense for how directionally related " +
           "one side is to the other, and how many common concepts one side contains of the other.");
        
        TextFlow tf19 = new TextFlow(instr21);
        
        ImageView iv8 = new ImageView("compare_overlap_display.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf18 = new TextFlow(iv8);
        tf18.setTextAlignment(TextAlignment.CENTER);
        
        Text instr22 = new Text("\u2022 \tFor a thorough description of the overlap values please refer to the ");
        instr22.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Hyperlink instr23 = new Hyperlink("Cortical.io Metrics Guide Here");
        instr23.setFocusTraversable(false);
        instr23.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        instr23.setOnAction(e -> {
            ApplicationService.getInstance().getHostServices().showDocument("http://documentation.cortical.io/similarity_metrics.html#similarity-metrics-guide");
            ((Hyperlink)e.getSource()).setVisited(false);
        });
        
        TextFlow tf20 = new TextFlow(instr22, instr23);
                
        getChildren().addAll(titleFlow, ttf, t, tf, tf2, tf3, tf4, tf5, tf8, tf10, tf11, done, tf6, tf7, tf9, title2, instr12, tf12, tf13, tf14, title3, tf15, tf16, tf17, title4, tf19, tf18, tf20);
    }
}
