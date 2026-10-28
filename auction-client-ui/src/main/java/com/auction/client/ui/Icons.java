package com.auction.client.ui;

import javafx.scene.layout.Region;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Lightweight SVG icons which do not depend on the operating system font. */
public final class Icons {
    private static final Map<String, String> PATHS = Map.ofEntries(
            Map.entry("eye", "M2 12C4 7 8 4 12 4s8 3 10 8c-2 5-6 8-10 8S4 17 2 12zm2.2 0c1.7 3.8 4.5 6 7.8 6s6.1-2.2 7.8-6C18.1 8.2 15.3 6 12 6s-6.1 2.2-7.8 6zM12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"),
            Map.entry("eye-off", "M3 2l19 19-1.4 1.4-3.2-3.2C15.8 19.7 14 20 12 20 8 20 4 17 2 12c.8-2 2-3.7 3.4-5L1.6 3.4zM7 8.6C5.9 9.5 5 10.7 4.2 12c1.7 3.8 4.5 6 7.8 6 1.3 0 2.6-.3 3.7-.9l-1.3-1.3A4 4 0 0 1 8.2 9.8zM12 4c4 0 8 3 10 8-.6 1.4-1.4 2.7-2.4 3.8l-1.4-1.4c.6-.7 1.1-1.5 1.6-2.4C18.1 8.2 15.3 6 12 6c-.7 0-1.4.1-2 .3L8.4 4.7C9.5 4.2 10.7 4 12 4z"),
            Map.entry("trophy", "M7 3h10v3h3v3a5 5 0 0 1-5 5h-1v3h3v2H7v-2h3v-3H9a5 5 0 0 1-5-5V6h3V3zm-1 5a3 3 0 0 0 3 3V5H6v3zm9 3a3 3 0 0 0 3-3V5h-3v6z"),
            Map.entry("search", "M10 3a7 7 0 1 0 4.9 12L21 21l-1.4 1.4-6.1-6.1A7 7 0 0 0 10 3zm0 2a5 5 0 1 1 0 10 5 5 0 0 1 0-10z"),
            Map.entry("bag", "M6 8h12l1 13H5L6 8zm3 0V6a3 3 0 0 1 6 0v2h-2V6a1 1 0 0 0-2 0v2H9z"),
            Map.entry("clock", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zm0 2a7 7 0 1 1 0 14 7 7 0 0 1 0-14zm-1 2v6l5 3 1-1.7-4-2.3V7z"),
            Map.entry("stopwatch", "M9 2h6v2H9zM11 6h2v2h-2zM12 9a6 6 0 1 0 0 12 6 6 0 0 0 0-12zm0 2a4 4 0 1 1 0 8 4 4 0 0 1 0-8zm1-1 4-4 1.4 1.4-4 4z"),
            Map.entry("watch", "M8 2h8l1 4a7 7 0 0 1 0 12l-1 4H8l-1-4a7 7 0 0 1 0-12zm1.6 2-.5 2h5.8l-.5-2zm5.5 14H8.9l.5 2h5.2zm-3.1-1a5 5 0 1 0 0-10 5 5 0 0 0 0 10zm0-8a3 3 0 1 1 0 6 3 3 0 0 1 0-6zm-1 1h2v2l2 1-1 1.7-3-1.7z"),
            Map.entry("palette", "M12 3a9 9 0 0 0 0 18h2a2 2 0 0 0 0-4h-1a2 2 0 0 1 0-4h2a4 4 0 0 0 0-8h-3zm-4 7a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm4-2a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm4 2a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z"),
            Map.entry("home", "M3 11 12 3l9 8-1.5 1.5L18 11v9h-5v-6h-3v6H6v-9l-1.5 1.5z"),
            Map.entry("laptop", "M5 4h14v11H5zm2 2v7h10V6zm-5 11h20l-2 3H4z"),
            Map.entry("car", "M5 7h14l3 7v5h-2v-2H4v2H2v-5zm1.4 2-1.5 4h14.2l-1.5-4zM6 15a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3zm12 0a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z"),
            Map.entry("book", "M4 4h7a3 3 0 0 1 3 3 3 3 0 0 1 3-3h3v16h-3a3 3 0 0 0-3 2 3 3 0 0 0-3-2H4V4zm2 2v12h5a5 5 0 0 1 1 .1V7a1 1 0 0 0-1-1H6z"),
            Map.entry("calendar", "M6 3h2v3h8V3h2v3h3v15H3V6h3zm-1 7v9h14v-9zm2 2h3v3H7zm5 0h3v3h-3z"),
            Map.entry("user", "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm-7 9a7 7 0 0 1 14 0H5z"),
            Map.entry("fire", "M13 2c1 4-3 5-1 8 1-1 2-2 2-4 4 4 4 10-2 14-5 2-11-1-11-7 0-4 3-7 6-9-1 4 1 5 4 6-1-3 2-5 0-8z"),
            Map.entry("tag", "M3 12V4h8l10 10-8 8L3 12zm5-5a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z"),
            Map.entry("image", "M3 4h18v16H3zm2 2v12h14V6zm2 9 3-3 2 2 3-4 4 5H7z"),
            Map.entry("gavel", "m14 4 6 6-2 2-1-1-5 5 1 1-2 2-6-6 2-2 1 1 5-5-1-1 2-2zM3 20h14v2H3z"),
            Map.entry("arrow-left", "M13 4 5 12l8 8 1.4-1.4L8.8 13H21v-2H8.8l5.6-5.6z"),
            Map.entry("arrow-right", "m11 4-1.4 1.4 5.6 5.6H3v2h12.2l-5.6 5.6L11 20l8-8z"),
            Map.entry("check", "m9.2 17.2-5-5L5.6 10l3.6 3.6L18.4 4.4l1.4 1.4-10.6 11.4z"),
            Map.entry("warning", "M12 3 2 21h20zm1 14h-2v-2h2zm0-4h-2V9h2z"),
            Map.entry("lock", "M6 10V7a6 6 0 0 1 12 0v3h2v11H4V10zm2 0h8V7a4 4 0 0 0-8 0zm3 4v4h2v-4z"),
            Map.entry("bolt", "m13 2-9 12h7l-1 8 10-13h-7z"),
            Map.entry("moon", "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6.5 6.5 0 0 0 8.268 8.268c.344-.215.825-.003.803.401z"),
            Map.entry("sun", "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0-5h2v3h-2zm0 17h2v3h-2zM2 11h3v2H2zm17 0h3v2h-3zM5 4.4 6.4 3l2.1 2.1-1.4 1.4zM16 18.9l1.4-1.4 2.1 2.1-1.4 1.4zM18.9 5l2.1-2.1 1.4 1.4-2.1 2.1zM5.1 19l2.1-2.1 1.4 1.4-2.1 2.1z"),
            Map.entry("chevron-down", "m6 9 6 6 6-6-1.4-1.4L12 12.2 7.4 7.6z"),
            Map.entry("mail", "M3 5h18v14H3zm2 2v.5l7 5 7-5V7l-7 5zM5 10v7h14v-7l-7 5z"),
            Map.entry("copy", "M8 3h11a2 2 0 0 1 2 2v13h-2V5H8zm-3 4h11a2 2 0 0 1 2 2v12H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zm0 2v10h11V9z"),
            Map.entry("logout", "M10 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H10v-2h10V5H10zm1 4 5 5-5 5v-3H3v-4h8z")
    );

    private Icons() { }

    public static List<String> names() {
        return PATHS.keySet().stream().sorted().collect(Collectors.toList());
    }

    public static Region icon(String name, double size) {
        String svgContent = PATHS.get(name);
        if (svgContent == null) {
            throw new IllegalArgumentException("Unknown icon: " + name);
        }

        SVGPath glyph = new SVGPath();
        glyph.setContent(svgContent);
        glyph.setFillRule("moon".equals(name) ? FillRule.NON_ZERO : FillRule.EVEN_ODD);
        glyph.getStyleClass().add("glyph");

        Region icon = new Region() {
            {
                getChildren().add(glyph);
            }

            @Override
            protected void layoutChildren() {
                glyph.relocate((getWidth() - 24) / 2, (getHeight() - 24) / 2);
                double scale = Math.min(getWidth(), getHeight()) / 24;
                glyph.setScaleX(scale);
                glyph.setScaleY(scale);
            }
        };
        icon.setMinSize(size, size);
        icon.setPrefSize(size, size);
        icon.setMaxSize(size, size);
        icon.getStyleClass().add("icon");
        return icon;
    }
}
