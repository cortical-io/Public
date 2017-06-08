package io.cortical.iris.ui.util;

import javafx.scene.paint.Color;

/**
 * Calculates the highest foreground contrast color given
 * the specified background color.
 * 
 * Uses the formula <a href="http://www.w3.org/TR/WCAG20/">W3C Recommendation</a>
 * 
 * @author cogmission
 */
public enum Contrast {
    WHITE(Color.WHITE), BLACK(Color.BLACK); 
    
    private Color color;
    private Contrast(Color c) {
        this.color = c;
    }
    
    /**
     * Returns the {@code Contrast} enum containing the correct
     * foreground color to use based on the specified background
     * color
     * @param background
     * @return
     */
    public static Contrast get(Color background) {
        return contrast(background);
    }
    
    /**
     * Returns this {@link Contrast}'s color.
     * @return
     */
    public Color color() {
        return color;
    }
    
    /**
     * Calculates the highest foreground contrast color given
     * the specified background color.
     *  
     * @param backGround    the color against which we calculate the 
     *                      best foreground color.
     * @return  the highest contrast foreground color
     */
    private static Contrast contrast(Color backGround) {
       double R = backGround.getRed();
       double G = backGround.getGreen();
       double B = backGround.getBlue();
       double L; // Luminance
       double[] C = { R/255, G/255, B/255 };
       
       for(int i = 0;i < C.length;++i) {
           if(C[i] <= 0.03928) {
               C[i] = C[i] / 12.92;
           }else{
               C[i] = Math.pow( ( C[i] + 0.055 ) / 1.055, 2.4);
           }
       }

       L = 0.2126 * C[0] + 0.7152 * C[1] + 0.0722 * C[2];

       return L > 0.179 ? Contrast.BLACK : Contrast.WHITE;
    }
}
