module com.hieu {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.prefs;
    requires java.logging;

    opens com.hieu to javafx.fxml, javafx.graphics;
    opens com.hieu.controllers to javafx.fxml;
    opens com.hieu.util to javafx.fxml;
    opens com.hieu.models to javafx.base;

    exports com.hieu;
    exports com.hieu.controllers;
    exports com.hieu.models;
    exports com.hieu.services;
    exports com.hieu.util;
}