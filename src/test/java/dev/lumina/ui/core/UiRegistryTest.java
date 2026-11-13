package dev.lumina.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.lumina.ui.api.UiEvent;
import dev.lumina.ui.api.UiEventType;
import dev.lumina.ui.api.UiPage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UiRegistryTest {

    private static UiPage page(String id) {
        return UiPage.empty(id, id);
    }

    @Test
    void registersAndResolvesPages() {
        UiRegistry registry = new UiRegistry();
        registry.registerPage(page("hud"));

        assertEquals("hud", registry.page("hud").id());
        assertEquals(1, registry.pageCount());
        assertNull(registry.page("missing"));
    }

    @Test
    void unregistersPages() {
        UiRegistry registry = new UiRegistry();
        registry.registerPage(page("hud"));

        assertTrue(registry.unregisterPage("hud"));
        assertFalse(registry.unregisterPage("hud"));
        assertEquals(0, registry.pageCount());
    }

    @Test
    void clearPagesKeepsListeners() {
        UiRegistry registry = new UiRegistry();
        registry.registerPage(page("hud"));
        List<UiEvent> received = new ArrayList<>();
        registry.on("hud", received::add);

        registry.clearPages();
        registry.dispatch(UiEvent.click(UiEventType.LEFT_CLICK, "hud", "node", UUID.randomUUID()));

        assertEquals(0, registry.pageCount());
        assertEquals(1, received.size());
    }

    @Test
    void dispatchesOnlyToMatchingPageListeners() {
        UiRegistry registry = new UiRegistry();
        List<UiEvent> hudEvents = new ArrayList<>();
        List<UiEvent> otherEvents = new ArrayList<>();
        registry.on("hud", hudEvents::add);
        registry.on("other", otherEvents::add);

        registry.dispatch(UiEvent.click(UiEventType.RIGHT_CLICK, "hud", "n", UUID.randomUUID()));

        assertEquals(1, hudEvents.size());
        assertTrue(otherEvents.isEmpty());
    }

    @Test
    void closingSubscriptionStopsDelivery() throws Exception {
        UiRegistry registry = new UiRegistry();
        List<UiEvent> received = new ArrayList<>();
        AutoCloseable subscription = registry.on("hud", received::add);

        registry.dispatch(UiEvent.click(UiEventType.LEFT_CLICK, "hud", "n", UUID.randomUUID()));
        subscription.close();
        registry.dispatch(UiEvent.click(UiEventType.LEFT_CLICK, "hud", "n", UUID.randomUUID()));

        assertEquals(1, received.size());
    }
}
