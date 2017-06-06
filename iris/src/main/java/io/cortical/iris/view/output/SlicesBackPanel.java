package io.cortical.iris.view.output;

import io.cortical.iris.ApplicationService;
import io.cortical.iris.view.BackPanel;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class SlicesBackPanel extends BackPanel {

    public SlicesBackPanel(double spacing) {
        super(spacing);
        
        Text title = new Text("SlicesDisplay Usage");
        title.setFont(Font.font("Questrial", FontWeight.BOLD, 18));
        
        Text descr = new Text("Displays an ordered list of text objects (ordered according to where the text slice appears in the input text). " + 
            "A Text object consists of a text slice (defined as a slice by the Retina)");
        
        TextFlow tf = new TextFlow(descr);
        
        
        //--
        
        Text title2 = new Text("\nSlices - WindowTitlePane:");
        title2.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr6 = new Text("\u2022 \tEvery Window in IRIS has a ");
        
        Text instr7 = new Text("\"Smart\" (context aware), WindowTitlePane\"");
        instr7.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        ImageView iv2 = new ImageView("slices_window_title_pane.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf5 = new TextFlow(instr6, instr7);
        
        TextFlow tf6 = new TextFlow(iv2);
        tf6.setTextAlignment(TextAlignment.CENTER);
        
        Text instr8 = new Text(" in the left control area of the App (Control Pane). These context-aware configuration panels, " +
            "change their contents depending on the state of their associated window. When the \"Slices\" tab is selected, the " +
            "title pane shows a \"Range-slider\" which allows the user to select the number and range of \"Slices\" to retrieve " +
            "from the server.");
        
        TextFlow tf7 = new TextFlow(instr8);
        
        //--
        
        Text title3 = new Text("\nSlices Rangeslider:");
        title3.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        ImageView iv3 = new ImageView("slices_rangeslider.png") {
            public double getBaselineOffset() {
                return 15;
            }
        };
        
        TextFlow tf8 = new TextFlow(iv3);
        tf8.setTextAlignment(TextAlignment.CENTER);
        
        Text instr9 = new Text("\u2022 \tUse the rangeslider to select the number of slices to retrieve from the server; using the \"start\" " +
            "and \"end\" indexes to determine the start and end locations within the list of contexts. The selected \"window\" of contexts will " +
            "be returned from the server if there are contexts residing within the range selected.");
        
        TextFlow tf9 = new TextFlow(instr9);
        
        Text instr10 = new Text("\u2022 \tIn addition, you can type a number directly into the start-index and end-index index number fields.");
        
        TextFlow tf10 = new TextFlow(instr10);
        
        //--
        
        Text instr11 = new Text("For more information on \"working with Text\": ");
        
        Hyperlink instr12= new Hyperlink("Text Endpoint documentation on the Cortical.io website.");
        instr12.setFocusTraversable(false);
        instr12.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        instr12.setOnAction(e -> {
            ApplicationService.getInstance().getHostServices().showDocument("http://documentation.cortical.io/working_with_text.html#rest-api");
            ((Hyperlink)e.getSource()).setVisited(false);
        });
        
        TextFlow tf11 = new TextFlow(instr11, instr12);
        
        //--
        
        Text instr13 = new Text("Click Here: ");
        
        Hyperlink instr14 = new Hyperlink("to try out the Text Endpoint on the Retina API. (Click the \"Text\" tab)");
        instr14.setFocusTraversable(false);
        instr14.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        instr14.setOnAction(e -> {
            ApplicationService.getInstance().getHostServices().showDocument("http://http://api.cortical.io");
            ((Hyperlink)e.getSource()).setVisited(false);
        });
        
        TextFlow tf12 = new TextFlow(instr13, instr14);
        
        
        getChildren().addAll(title, tf, title2, tf5, tf6, tf7, title3, tf8, tf9, tf10, tf11, tf12);
    }
}
