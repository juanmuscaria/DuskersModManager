package com.juanmuscaria.dmm.util;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;

import java.io.PrintWriter;
import java.io.StringWriter;

public class DialogHelper {

    public static void reportAndExit(Throwable e) {
        var sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("An unrecoverable error occurred!");
        alert.setContentText("The application will now exit");
        var text = new TextArea(sw.toString());
        text.setMaxHeight(Long.MAX_VALUE);
        text.setMaxWidth(Long.MAX_VALUE);
        AnchorPane.setRightAnchor(text, 0d);
        AnchorPane.setBottomAnchor(text, 0d);
        AnchorPane.setTopAnchor(text, 0d);
        AnchorPane.setLeftAnchor(text, 0d);
        var panel = new AnchorPane(text);
        panel.setMaxHeight(Long.MAX_VALUE);
        panel.setMaxWidth(Long.MAX_VALUE);
        alert.getDialogPane().setExpandableContent(panel);
        alert.showAndWait();
        System.exit(1);
    }

    public static void reportAndWait(Throwable e, String header, String description) {
        var sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(description);
        var text = new TextArea(sw.toString());
        text.setMaxHeight(Long.MAX_VALUE);
        text.setMaxWidth(Long.MAX_VALUE);
        AnchorPane.setRightAnchor(text, 0d);
        AnchorPane.setBottomAnchor(text, 0d);
        AnchorPane.setTopAnchor(text, 0d);
        AnchorPane.setLeftAnchor(text, 0d);
        var panel = new AnchorPane(text);
        panel.setMaxHeight(Long.MAX_VALUE);
        panel.setMaxWidth(Long.MAX_VALUE);
        alert.getDialogPane().setExpandableContent(panel);
        alert.showAndWait();
    }

    public static void reportAndWait(ReportedException e) {
        var sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        var alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(e.getHeader());
        alert.setContentText(e.getMessage());
        var text = new TextArea(sw.toString());
        text.setMaxHeight(Long.MAX_VALUE);
        text.setMaxWidth(Long.MAX_VALUE);
        AnchorPane.setRightAnchor(text, 0d);
        AnchorPane.setBottomAnchor(text, 0d);
        AnchorPane.setTopAnchor(text, 0d);
        AnchorPane.setLeftAnchor(text, 0d);
        var panel = new AnchorPane(text);
        panel.setMaxHeight(Long.MAX_VALUE);
        panel.setMaxWidth(Long.MAX_VALUE);
        alert.getDialogPane().setExpandableContent(panel);
        alert.showAndWait();
    }

    public static void infoAndWait(String header, String description) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(header);
        alert.setContentText(description);
        alert.showAndWait();
    }

    @Getter
    public static class ReportedException extends Exception {
        private final String header;

        public ReportedException(String header, String message) {
            super(message);
            this.header = header;
        }

        public ReportedException(String header, String message, Throwable cause) {
            super(message, cause);
            this.header = header;
        }

        public ReportedException(String header, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
            this.header = header;
        }
    }
}
