package com.auction.client.ui;

import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

public final class RoundedClip {
    private RoundedClip() { }
    public static void apply(Region region, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2); clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }
}
