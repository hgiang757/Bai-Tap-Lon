package com.auction.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UiResourcesTest {
    @Test
    void decorativeResourcesAreAbsentAndProductSupportRemains() {
        for (String resource :
                new String[] {
                    "images/app-bg.jpg", "images/login-bg.jpg", "ui/AppBackground.class"
                }) {
            assertNull(getClass().getResource("/com/auction/client/" + resource), resource);
        }
        assertNotNull(ProductImages.class);
        assertNotNull(getClass().getResource("/com/auction/client/css/app.css"));
        assertNotNull(getClass().getResource("/com/auction/client/views/auth-view.fxml"));
    }
}
