package dev.lumina.ui.api;

import java.util.UUID;

/**
 * Event delivered to page listeners. Immutable and free of platform types so listeners can be
 * unit tested and reused on both Java and Bedrock surfaces.
 */
public record UiEvent(
        UiEventType type,
        String pageId,
        String nodeId,
        UUID playerId,
        float cursorX,
        float cursorY,
        float amount,
        String input) {

    public static UiEvent click(UiEventType type, String pageId, String nodeId, UUID playerId) {
        return new UiEvent(type, pageId, nodeId, playerId, 0f, 0f, 0f, "");
    }

    public static UiEvent cursor(
            UiEventType type, String pageId, String nodeId, UUID playerId, float x, float y) {
        return new UiEvent(type, pageId, nodeId, playerId, x, y, 0f, "");
    }

    public static UiEvent input(String pageId, String nodeId, UUID playerId, String value) {
        return new UiEvent(UiEventType.INPUT_TEXT, pageId, nodeId, playerId, 0f, 0f, 0f, value);
    }
}
