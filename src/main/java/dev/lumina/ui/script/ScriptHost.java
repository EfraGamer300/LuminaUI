package dev.lumina.ui.script;

/**
 * Bridge between the script interpreter and the plugin runtime.
 *
 * <p>Keeping the host as an interface means the interpreter can be unit tested with a fake, and a
 * future Bedrock or console surface can implement the same contract.
 */
public interface ScriptHost {

    /** Value of {@code %name%}, or the raw placeholder when unknown. */
    String placeholder(String name);

    void message(String text);

    void actionBar(String text);

    void title(String text);

    void command(String line);

    void openPage(String pageId);

    void closePage();

    void sound(String key);

    void log(String text);

    void cancelEvent(boolean cancelled);

    /** Schedules {@code continuation} after {@code ticks}, on the thread that owns the player. */
    void after(long ticks, Runnable continuation);

    /** Reported when a script uses a verb this host does not implement. */
    void unsupported(String verb, String argument);

    default String playerName() {
        return "console";
    }

    default String pageId() {
        return "";
    }

    default String elementId() {
        return "";
    }
}
