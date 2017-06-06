package io.cortical.iris.view.input.expression;

import io.cortical.iris.view.BackPanel;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class ExpressionBackPanel extends BackPanel {
    
    /**
     * Contructs a new {@code ExpressionBackPanel}
     * @param spacing   vertical spacing between child nodes
     */
    public ExpressionBackPanel(double spacing) {
        super(spacing);
        
        Text title3 = new Text("Expressions\n\n");
        title3.setFont(Font.font(title3.getFont().getFamily(), FontWeight.BOLD, 18));
        
        Text instr16 = new Text("\u2022 \tBy adding, subtracting and combining terms, you can refine " +
            "the meaning of your expressions using the supported Boolean operators: ");
        instr16.setFont(Font.font(title3.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr22 = new Text("AND, ");
        instr22.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr23 = new Text("OR, ");
        instr23.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr24 = new Text("NOT, ");
        instr24.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr25 = new Text("and ");
        instr25.setFont(Font.font(title3.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr26 = new Text("XOR\n");
        instr26.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        TextFlow tf7 = new TextFlow(title3, instr16, instr22, instr23, instr24, instr25, instr26);
        
        Text instr17 = new Text("Example: ");
        instr17.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr18 = new Text("Apple - Fruit  =  \"Computers\" or \"Music\"\n");
        instr18.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        
        Text instr27 = new Text("(\"Apple\", semantically emphasizing \"computers\" or \"music\").\n\n");
        instr27.setFont(Font.font("Arial", FontWeight.NORMAL, FontPosture.ITALIC, 12));
        
        TextFlow tf8 = new TextFlow(instr17, instr18, instr27);
        tf8.setTextAlignment(TextAlignment.CENTER);
        
        getChildren().addAll(tf7, tf8);
        
        //---
        
        Text title = new Text("Using the ExpressionDisplay");
        title.setFont(Font.font(title.getFont().getFamily(), FontWeight.BOLD, 18));
        
        Text t = new Text("REMEMBER: THE <CONTROL> KEY IS ");
        t.setFont(Font.font("Arial", FontWeight.MEDIUM, 10));
        
        Text t2 = new Text("KING");
        t2.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        
        Text t3 = new Text(" (MORE ON THE <CONTROL> KEY BELOW)\n");
        t3.setFont(Font.font("Arial", FontWeight.MEDIUM, FontPosture.ITALIC, 9));
        
        TextFlow fl = new TextFlow(t, t2, t3);
        
        Text header1 = new Text("Entering Terms:\n\n");
        header1.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr1 = new Text("\u2022 \tTo use the \"ExpressionDisplay\" just start typing a word (Term), then press");
        instr1.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr15 = new Text(" <ENTER>\n\n");
        instr15.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        
        Text instr14 = new Text("\u2022 \tAfter pressing <ENTER> following the ");
        instr14.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr12 = new Text("2nd");
        instr12.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        
        Text instr13 = new Text(" term, you will be prompted to choose connecting words or ");
        instr13.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr2 = new Text("\"Operators\"");
        instr2.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr3 = new Text(" as you type.\n");
        instr3.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf = new TextFlow(header1, instr1, instr15, instr14, instr12, instr13, instr2, instr3);
        
        //-----
        
        Text header2 = new Text("\nPlacing Operators using the \"OperatorWheel\":\n");
        header2.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        ImageView iv2 = new ImageView("operator_wheel.png") {
            public double getBaselineOffset() {
                return 87;
            }
        };
                    
        TextFlow tf2 = new TextFlow(header2);
        
        TextFlow tf3 = new TextFlow(iv2);
        tf3.setTextAlignment(TextAlignment.CENTER);
        
        Text instr4 = new Text("\n\u2022 \tTo select an ");
        instr4.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr5 = new Text("\"Operator\" ");
        instr2.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr6 = new Text("use the arrow keys");
        instr6.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv = new ImageView("keyboardarrows.png") {
            public double getBaselineOffset() {
                return 36;
            }
        };
        
        Text instr7 = new Text("\n\n\u2022 \tTapping the arrow keys will cycle through each \"Operator\" - highlighting them. ");
        instr7.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf4 = new TextFlow(instr4, instr5, instr6, iv, instr7);
        
        ImageView iv5 = new ImageView("wheelselected.png") {
            public double getBaselineOffset() {
                return 36;
            }
        };
        
        Text instr10 = new Text("\n\nPress <ENTER> to select an \"Operator\"");
        instr10.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf5 = new TextFlow(iv5, instr10);
        tf5.setTextAlignment(TextAlignment.CENTER);
        
        Text instr8 = new Text("\n\n\u2022 \tWatch the \"Progress Meter\" in the");
        instr8.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr20 = new Text(" connected");
        instr20.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        Text instr21 = new Text("  OutputWindow switch from: ");
        instr21.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv3 = new ImageView("resend.png") {
            public double getBaselineOffset() {
                return 18;
            }
        };
        
        Text instr9 = new Text("  to  ");
        instr9.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv4 = new ImageView("progress_meter.png") {
            public double getBaselineOffset() {
                return 18;
            }
        };
        
        Text instr11 = new Text("  and back to  ");
        instr9.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv6 = new ImageView("resend.png") {
            public double getBaselineOffset() {
                return 18;
            }
        };
        
        Text instr19 = new Text("  indicating that the results have returned from the server.\n\n");
        instr19.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf6 = new TextFlow(instr8, instr20, instr21, iv3, instr9, iv4, instr11, iv6, instr19);
        
        //---
        
        Text title4 = new Text("Navigating the ExpressionDisplay (Keyboard)");
        title4.setFont(Font.font(title.getFont().getFamily(), FontWeight.BOLD, 18));
        
        Text header3 = new Text("The <TAB> Key:\n\n");
        header3.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr28 = new Text("\u2022 \tPressing ");
        instr28.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv7 = new ImageView("tabkey.png") {
            public double getBaselineOffset() {
                return 25;
            }
        };
        
        Text instr29 = new Text(" will cycle the editor to the ");
        instr29.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr30 = new Text("right");
        instr30.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 14));
        
        Text instr31 = new Text(" (wrapping around to the beginning).\n\n");
        instr31.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr32 = new Text("\u2022 \tPressing ");
        instr32.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv8 = new ImageView("shift.png") {
            public double getBaselineOffset() {
                return 25;
            }
        };
        
        Text instr33 = new Text("  +  ");
        instr33.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        ImageView iv9 = new ImageView("tabkey.png") {
            public double getBaselineOffset() {
                return 25;
            }
        };
        
        Text instr34 = new Text(" will cycle the editor to the ");
        instr34.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr35 = new Text("left");
        instr35.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 14));
        
        Text instr36 = new Text(" (wrapping around to the end).\n\n");
        instr36.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf9 = new TextFlow(instr28, iv7, instr29, instr30, instr31, instr32, iv8, instr33, iv9, instr34, instr35, instr36);
        
        //--
        
        Text title5 = new Text("While pressing <TAB> or <SHIFT> + <TAB>, will cycle the Editor to the next Term; pressing " +
            "the <LEFT> and <RIGHT> arrow keys will cycle the Editor ");
        title5.setFont(Font.font("Arial", FontWeight.NORMAL, FontPosture.ITALIC, 14));
        
        Text title6 = new Text("In-Between ");
        title6.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 16));
        
        Text title7 = new Text(" each Term Bubble (For placing Parenthesis).\n\n\n");
        title7.setFont(Font.font("Arial", FontWeight.NORMAL, FontPosture.ITALIC, 14));
        
        Text header4 = new Text("The <ARROW> Keys:\n\n");
        header4.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr37 = new Text("\u2022 \tPressing ");
        instr37.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv10 = new ImageView("rightarrow.png") {
            public double getBaselineOffset() {
                return 17;
            }
        };
        
        Text instr38 = new Text(" will cycle the editor to the ");
        instr38.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr39 = new Text("right");
        instr39.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 14));
        
        Text instr40 = new Text(" (wrapping around to the beginning).\n\n");
        instr40.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr41 = new Text("\u2022 \tPressing ");
        instr41.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv12 = new ImageView("leftarrow.png") {
            public double getBaselineOffset() {
                return 17;
            }
        };
        
        Text instr42 = new Text(" will cycle the editor to the ");
        instr42.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr43 = new Text("left");
        instr43.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 14));
        
        Text instr44 = new Text(" (wrapping around to the end).\n\n");
        instr44.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf10 = new TextFlow(title5, title6, title7, instr37, iv10, instr38, instr39, instr40, instr41, iv12, instr42, instr43, instr44);
        
        //--
        
        Text header5 = new Text("The <CONTROL> Key:\n\n");
        header5.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Text instr45 = new Text("\u2022 \tPressing  ");
        instr45.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv13 = new ImageView("controlkey.png") {
            public double getBaselineOffset() {
                return 17;
            }
        };
        
        Text instr46 = new Text("  will activate the ");
        instr46.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr47 = new Text("current bubble");
        instr47.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 12));
        
        Text instr48 = new Text(" (Bubble with the Green Cursor),");
        instr48.setFont(Font.font("Arial", FontWeight.BOLD, 12));
             
        Text instr49 = new Text(" either allowing the editing of a Term or " +
           "placement of an Operator, or Parenthesis.\n\n");
        instr49.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf11 = new TextFlow(header5, instr45, iv13, instr46, instr47, instr48, instr49);
        
        //--
        
        Text title8 = new Text("Navigating the ExpressionDisplay (Mouse)");
        title8.setFont(Font.font(title.getFont().getFamily(), FontWeight.BOLD, 18));
        
        Text instr50 = new Text("While the ExpressionDisplay is designed to be used without fingers " +
            "ever having to leave the keyboard, there are 3 features only available by using the mouse.\n\n");
        instr50.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr51 = new Text("\u2022 \tSelecting a Term Bubble\n");
        instr51.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf12 = new TextFlow(instr50, instr51);
        
        ImageView iv14 = new ImageView("expression_display_unselected.png") {
            public double getBaselineOffset() {
                return 17;
            }
        };
        
        TextFlow tf13 = new TextFlow(iv14);
        tf13.setTextAlignment(TextAlignment.CENTER);
        
        Text instr52 = new Text("\n");
        instr52.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv15 = new ImageView("expression_display_selected.png") {
            public double getBaselineOffset() {
                return 17;
            }
        };
        
        TextFlow tf14 = new TextFlow(instr52, iv15);
        tf14.setTextAlignment(TextAlignment.CENTER);
        
        Text instr53 = new Text("\nWhen selecting a Term Bubble, also notice that the OutputWindow's \"FingerprintDisplay\" " +
            "highlights positions associated with the selected term.\n");
        
        TextFlow tf15 = new TextFlow(instr53);
        
        ImageView iv16 = new ImageView("partial_grid_unselected.png") {
            public double getBaselineOffset() {
                return 17;
            }
        };
        
        ImageView iv17 = new ImageView("partial_grid_selected.png") {
            public double getBaselineOffset() {
                return 17;
            }
        };
        
        Text instr54 = new Text("     ");
        
        TextFlow tf16 = new TextFlow(iv16, instr54, iv17);
        tf14.setTextAlignment(TextAlignment.CENTER);
        
        Text instr55 = new Text("\u2022 \tSelecting an \"Operator\" (brings up the \"OperatorWheel\")\n\n");
        instr55.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        Text instr56 = new Text("\u2022 \tSelecting a \"ParenthesisBubble\" (highlights the matching \"ParenthesisBubble\")" +
            " Unmatched parenthesis are highlighted in red.\n");
        instr56.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        TextFlow tf17 = new TextFlow(instr55, instr56);
        
        ImageView iv18 = new ImageView("expression_display_parens_unselected.png") {
            public double getBaselineOffset() {
                return 17;
            }
        };
        
        TextFlow tf18 = new TextFlow(iv18);
        tf18.setTextAlignment(TextAlignment.CENTER);
        
        Text instr57 = new Text("\n");
        instr57.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
        
        ImageView iv19 = new ImageView("expression_display_parens_selected.png") {
            public double getBaselineOffset() {
                return 17;
            }
        };
        
        TextFlow tf19 = new TextFlow(instr57, iv19);
        tf19.setTextAlignment(TextAlignment.CENTER);
        
        
        getChildren().addAll(title, fl, tf, tf2, tf3, tf4, tf5, tf6, title4, tf9, tf10, tf11, title8, tf12, tf13, tf14, tf15, tf16, tf17, tf18, tf19);
                    
    }
}
