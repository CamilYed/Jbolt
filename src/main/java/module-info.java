module github.com.camilyed.jbolt {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    opens github.com.camilyed.jbolt to javafx.fxml;
    exports github.com.camilyed.jbolt;
    exports github.com.camilyed.jbolt.ui;
    opens github.com.camilyed.jbolt.ui to javafx.fxml;
}