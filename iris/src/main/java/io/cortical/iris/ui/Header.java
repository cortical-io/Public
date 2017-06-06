package io.cortical.iris.ui;

import io.cortical.iris.ApplicationService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/** 
 *  The decorative top portion of the application screen.
 */
public class Header extends Pane {
    
    /**
     * Creates a new {@code Header}
     */
    public Header() {
        double height = 90;
        
        prefWidthProperty().bind(widthProperty());
        setPrefHeight(height);
        getStyleClass().add("control-pane-header");
        
        Image image = new Image("iris_white_logo.png");
        ImageView logo = new ImageView(image);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setCache(true);
        
        Text subTitleText = new Text("...a specialized view into the Retina Engine. ");
        subTitleText.setFill(Color.WHITE);
        subTitleText.setFont(Font.font("Arial", FontWeight.MEDIUM, FontPosture.ITALIC, 14));
        subTitleText.setFill(Color.WHITE);
        subTitleText.setTextOrigin(VPos.BASELINE);
        
        logo.layoutXProperty().bind(subTitleText.layoutXProperty().add(60));
        
        Text t = new Text("Powered by Cortical.io");
        t.setFont(Font.font("Questrial-Regular", FontWeight.MEDIUM, FontPosture.REGULAR, 16));
        Hyperlink poweredBy = new Hyperlink("Powered by Cortical.io");
        poweredBy.setFocusTraversable(false);
        poweredBy.getStyleClass().add("header-powered-by");
        poweredBy.setPrefWidth(t.getLayoutBounds().getWidth() + 80);
        poweredBy.setPrefHeight(t.getLayoutBounds().getHeight());
        poweredBy.resize(t.getLayoutBounds().getWidth(), t.getLayoutBounds().getHeight());
        poweredBy.setAlignment(Pos.BASELINE_CENTER);
        poweredBy.setOnAction(e -> {
            ApplicationService.getInstance().getHostServices().showDocument("http://www.cortical.io");
            ((Hyperlink)e.getSource()).setVisited(false);
        });
                
        widthProperty().addListener((v, o, n) -> {
            double x2 = ((n.doubleValue() / 2.0) - (subTitleText.getLayoutBounds().getWidth() / 2.0)) + 30;
            subTitleText.setLayoutX(x2);
            
            double x3 = n.doubleValue() - poweredBy.getLayoutBounds().getWidth() + 20;
            poweredBy.setLayoutX(x3);
        });
        
        heightProperty().addListener((v, o, n) -> {
            logo.setLayoutY(-15);
            subTitleText.setLayoutY(81);
            poweredBy.setLayoutY(61);
        });
        
        getChildren().addAll(logo, subTitleText, poweredBy);
        
        Platform.runLater(() -> {
            requestLayout();
        });
    }
}
