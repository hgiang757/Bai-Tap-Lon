package com.auction.client.ui;

import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;

/** Normalizes wheel movement to pixel-like scrolling for application pages. */
public final class SmoothScroll {
    private SmoothScroll() { }

    public static void apply(ScrollPane scrollPane, double speed) {
        double effectiveSpeed = speed > 0 ? speed : 3.0;
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0 || isOwnedByInnerScrollable(event.getTarget())) return;
            Node content = scrollPane.getContent();
            if (content == null) return;
            double scrollable = content.getBoundsInLocal().getHeight() - scrollPane.getViewportBounds().getHeight();
            if (scrollable <= 0) return;
            double next = scrollPane.getVvalue() - event.getDeltaY() * effectiveSpeed / scrollable;
            scrollPane.setVvalue(Math.max(0, Math.min(1, next)));
            event.consume();
        });
    }

    private static boolean isOwnedByInnerScrollable(Object target) {
        if (!(target instanceof Node)) return false;
        for (Node node = (Node) target; node != null; node = node.getParent()) {
            if (node instanceof ListView || node instanceof TableView || node instanceof TextArea) return true;
        }
        return false;
    }
}
