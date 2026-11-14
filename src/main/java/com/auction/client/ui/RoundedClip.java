package com.auction.client.ui;

import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/** Binds a rounded rectangular clip to a region size. */
public final class RoundedClip {
  private RoundedClip() {}

  /** Clips a region to a rounded rectangle that follows its dimensions. */
  public static void apply(Region region, double radius) {
    Rectangle clip = new Rectangle();
    clip.setArcWidth(radius * 2);
    clip.setArcHeight(radius * 2);
    clip.widthProperty().bind(region.widthProperty());
    clip.heightProperty().bind(region.heightProperty());
    region.setClip(clip);
  }
}
