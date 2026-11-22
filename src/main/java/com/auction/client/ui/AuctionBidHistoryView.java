package com.auction.client.ui;

import com.auction.client.model.Bid;
import com.auction.client.util.FormatUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
  private static final ZoneId AUCTION_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final DateTimeFormatter WITH_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final DateTimeFormatter WITH_DATE = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private static final DateTimeFormatter WITH_DATE_SECONDS =
      DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
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
    updateEmptyState(true);
  }

  /** Clears the chart and bid list before displaying another auction. */
  public void clear() {
    priceSeries.getData().clear();
    lvRecentBids.getItems().clear();
    updateEmptyState(true);
  }

  private void updateEmptyState(boolean empty) {
    priceChart.setVisible(!empty);
    priceChart.setManaged(!empty);
    lblNoBids.setVisible(empty);
    lblNoBids.setManaged(empty);
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
            LocalDateTime time = chartTime(value.longValue());
            if (!chartTime((long) timeAxis.getLowerBound()).toLocalDate()
                .equals(chartTime((long) timeAxis.getUpperBound()).toLocalDate())) {
              return time.format(timeAxis.getTickUnit() < 60_000 ? WITH_DATE_SECONDS : WITH_DATE);
            }
            return timeAxis.getTickUnit() < 60_000
                ? time.format(WITH_SECONDS) : FormatUtil.formatChartTime(time);
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
            return value == null ? "" : FormatUtil.formatCurrency(value.doubleValue());
          }

          @Override
          public Number fromString(String value) {
            return 0;
          }
        });
  }

  /** Replaces the chart and bid list from one authoritative auction history. */
  public void render(List<Bid> bids, LocalDateTime now) {
    priceSeries.getData().clear();
    lvRecentBids.getItems().clear();
    updateEmptyState(bids.isEmpty());
    if (bids.isEmpty()) {
      return;
    }
    List<Bid> chronologicalBids = new ArrayList<>(bids);
    chronologicalBids.sort(Comparator.comparing(bid -> bid.getTimestamp() == null
        ? now : bid.getTimestamp()));
    lvRecentBids.getItems().setAll(chronologicalBids.reversed().stream()
        .map(this::formatBid).toList());
    int firstChartBid = Math.max(0, chronologicalBids.size() - MAX_CHART_POINTS);
    for (Bid bid : chronologicalBids.subList(firstChartBid, chronologicalBids.size())) {
      LocalDateTime time = bid.getTimestamp() == null ? now : bid.getTimestamp();
      addPricePoint(time, bid.getAmount(), bid.getBidderUsername(), bid.isAuto());
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
            time.atZone(AUCTION_ZONE).toInstant().toEpochMilli(),
            price,
            time);
    priceSeries.getData().add(dataPoint);
    attachPointTooltip(dataPoint, time, price, bidder, auto);
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
                  time.format(WITH_DATE_SECONDS)
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
    double span = last - first;
    double timePadding = Math.max(2_000, span * 0.05);
    double timeTick = Math.max(1_000, Math.ceil(span / 4_000) * 1_000);
    timeAxis.setLowerBound(Math.floor((first - timePadding) / timeTick) * timeTick);
    timeAxis.setUpperBound(Math.ceil((last + timePadding) / timeTick) * timeTick);
    timeAxis.setTickUnit(timeTick);

    double priceRange = maxPrice - minPrice;
    if (priceRange <= 0) {
      priceRange = Math.max(Math.abs(maxPrice) * 0.1, 1);
    }
    double lowerBound = Math.floor(Math.max(0, minPrice - priceRange * 0.05));
    double upperBound = Math.ceil(maxPrice + priceRange * 0.1);
    priceAxis.setLowerBound(lowerBound);
    priceAxis.setUpperBound(upperBound);
    priceAxis.setTickUnit(Math.ceil((upperBound - lowerBound) / 4));
  }

  private static LocalDateTime chartTime(long millis) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), AUCTION_ZONE);
  }
}
