package com.auction.client.ui;

import com.auction.client.model.Auction;
import com.auction.client.model.Bid;
import com.auction.client.util.FormatUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

/** Renders one authoritative history into a price curve and a newest-first bid list on FX. */
public final class AuctionBidHistoryView {
  private static final int MAX_CHART_POINTS = 30;
  private final LineChart<Number, Number> priceChart;
  private final NumberAxis timeAxis;
  private final NumberAxis priceAxis;
  private final Label lblNoBids;
  private final ListView<String> lvRecentBids;
  private XYChart.Series<Number, Number> priceSeries;

  /** Connects the supplied controls to a shared history and price chart. */
  public AuctionBidHistoryView(
      LineChart<Number, Number> chart,
      NumberAxis time,
      NumberAxis price,
      Label empty,
      ListView<String> history) {
    priceChart = chart;
    timeAxis = time;
    priceAxis = price;
    lblNoBids = empty;
    lvRecentBids = history;
    Label placeholder = new Label("Chưa có lượt đặt giá");
    placeholder.getStyleClass().add("empty-placeholder");
    lvRecentBids.setPlaceholder(placeholder);
    RoundedClip.apply(lvRecentBids, 12);
    setupChart();
  }

  /** Clears the chart and bid list before displaying another auction. */
  public void clear() {
    priceSeries.getData().clear();
    lvRecentBids.getItems().clear();
    lblNoBids.setVisible(true);
    lblNoBids.setManaged(true);
  }

  private void setupChart() {
    priceSeries = new XYChart.Series<>();
    priceChart.getData().clear();
    priceChart.getData().add(priceSeries);
    priceChart.setAnimated(false);
    priceChart.setLegendVisible(false);
    priceChart.setTitle(null);
    timeAxis.setLabel("");
    priceAxis.setLabel("");
    timeAxis.setAnimated(false);
    priceAxis.setAnimated(false);
    timeAxis.setAutoRanging(false);
    priceAxis.setAutoRanging(false);
    timeAxis.setForceZeroInRange(false);
    priceAxis.setForceZeroInRange(false);
    timeAxis.setTickMarkVisible(false);
    priceAxis.setTickMarkVisible(false);
    priceAxis.setMinorTickVisible(false);
    timeAxis.setTickLabelRotation(0);
    timeAxis.setTickLabelFormatter(
        new StringConverter<>() {
          @Override
          public String toString(Number value) {
            if (value == null || priceSeries == null || priceSeries.getData().isEmpty()) {
              return "";
            }
            return FormatUtil.formatChartTime(
                java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(value.longValue()),
                    java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
          }

          @Override
          public Number fromString(String value) {
            return 0;
          }
        });
    priceAxis.setTickLabelFormatter(
        new StringConverter<>() {
          @Override
          public String toString(Number value) {
            return value == null ? "" : FormatUtil.formatCompactPriceAxis(value.doubleValue());
          }

          @Override
          public Number fromString(String value) {
            return 0;
          }
        });
  }

  /** Replaces the chart and bid list from one authoritative auction history. */
  public void render(Auction auction, List<Bid> bids, LocalDateTime now) {
    priceSeries.getData().clear();
    lvRecentBids.getItems().clear();

    if (bids.isEmpty()) {
      LocalDateTime startTime = auction.getStartTime() == null ? now : auction.getStartTime();
      addPricePoint(startTime, auction.getStartingPrice(), null, false);
      lblNoBids.setVisible(true);
      lblNoBids.setManaged(true);
    } else {
      List<Bid> chronologicalBids = new ArrayList<>(bids);
      chronologicalBids.sort(
          Comparator.comparing(
              Bid::getTimestamp, Comparator.nullsFirst(Comparator.naturalOrder())));
      for (Bid bid : chronologicalBids) {
        LocalDateTime time = bid.getTimestamp() == null ? now : bid.getTimestamp();
        addPricePoint(time, bid.getAmount(), bid.getBidderUsername(), bid.isAuto());
        lvRecentBids.getItems().add(0, formatBid(bid));
      }
      lblNoBids.setVisible(false);
      lblNoBids.setManaged(false);
    }
    updateChartAxes();
  }

  private String formatBid(Bid bid) {
    String maskedName = FormatUtil.maskUsername(bid.getBidderUsername());
    return (bid.isAuto() ? "TỰ ĐỘNG · " : "")
        + maskedName
        + "  ·  "
        + bid.getFormattedTime()
        + "  ·  "
        + FormatUtil.formatCurrency(bid.getAmount());
  }

  private void addPricePoint(LocalDateTime time, double price, String bidder, boolean auto) {
    XYChart.Data<Number, Number> dataPoint =
        new XYChart.Data<>(
            time.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli(),
            price,
            time);
    priceSeries.getData().add(dataPoint);
    attachPointTooltip(dataPoint, time, price, bidder, auto);
    while (priceSeries.getData().size() > MAX_CHART_POINTS) {
      priceSeries.getData().remove(0);
    }
  }

  private void attachPointTooltip(
      XYChart.Data<Number, Number> dataPoint,
      LocalDateTime time,
      double price,
      String bidder,
      boolean auto) {
    Consumer<Node> install =
        node -> {
          if (node == null) {
            return;
          }
          node.getStyleClass().add("chart-point");
          Tooltip.install(
              node,
              new Tooltip(
                  FormatUtil.formatChartTime(time)
                      + " · "
                      + FormatUtil.formatCurrency(price)
                      + " · "
                      + FormatUtil.maskUsername(bidder)
                      + (auto ? " · TỰ ĐỘNG" : "")));
        };
    dataPoint.nodeProperty().addListener((observable, before, node) -> install.accept(node));
    install.accept(dataPoint.getNode());
  }

  private void updateChartAxes() {
    if (priceSeries == null || priceSeries.getData().isEmpty()) {
      return;
    }
    double minPrice = Double.POSITIVE_INFINITY;
    double maxPrice = Double.NEGATIVE_INFINITY;
    for (XYChart.Data<Number, Number> point : priceSeries.getData()) {
      double price = point.getYValue().doubleValue();
      minPrice = Math.min(minPrice, price);
      maxPrice = Math.max(maxPrice, price);
    }

    double first = priceSeries.getData().getFirst().getXValue().doubleValue();
    double last = priceSeries.getData().getLast().getXValue().doubleValue();
    double span = Math.max(1000, last - first);
    timeAxis.setLowerBound(first - span * 0.05);
    timeAxis.setUpperBound(last + span * 0.05);
    timeAxis.setTickUnit(Math.max(1000, span / 5));

    double priceRange = maxPrice - minPrice;
    if (priceRange <= 0) {
      priceRange = Math.max(Math.abs(maxPrice) * 0.1, 1);
    }
    double lowerBound = Math.max(0, minPrice - priceRange * 0.05);
    double upperBound = maxPrice + priceRange * 0.1;
    priceAxis.setLowerBound(lowerBound);
    priceAxis.setUpperBound(upperBound);
    priceAxis.setTickUnit((upperBound - lowerBound) / 4);
  }
}
