package io.cortical.iris.ui.util;

import impl.org.controlsfx.version.VersionChecker;
import javafx.scene.control.Control;

public abstract class ControlsFXControl extends Control {

    public ControlsFXControl() {
        VersionChecker.doVersionCheck();
    }

    private String stylesheet;

    /**
     * A helper method that ensures that the resource based lookup of the user
     * agent stylesheet only happens once. Caches the external form of the
     * resource.
     *
     * @param clazz
     *            the class used for the resource lookup
     * @param fileName
     *            the name of the user agent stylesheet
     * @return the external form of the user agent stylesheet (the path)
     */
    protected final String getUserAgentStylesheet(Class<?> clazz,
            String fileName) {

        /*
         * For more information please see RT-40658
         */
        if (stylesheet == null) {
            stylesheet = clazz.getClassLoader().getResource(fileName).toExternalForm();
        }

        return stylesheet;
    }
}
