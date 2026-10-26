module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires java.prefs;
    requires java.logging;

    opens com.auction.client to javafx.fxml, javafx.graphics;
    opens com.auction.client.controller to javafx.fxml;
    opens com.auction.client.dev to javafx.fxml;
    opens com.auction.client.ui to javafx.fxml;
    opens com.auction.client.util to javafx.fxml;
    opens com.auction.client.model to javafx.base;

    exports com.auction.client;
    exports com.auction.client.controller;
    exports com.auction.client.model;
    exports com.auction.client.service;
    exports com.auction.client.util;
    exports com.auction.client.ui;
}