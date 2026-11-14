package com.auction.client.navigation;

/** Giải phóng timer, observer và quyền cập nhật giao diện khi rời màn hình. */
public interface DisposableView {
  /** Releases tasks, timers and listeners owned by this view. */
  void dispose();
}
