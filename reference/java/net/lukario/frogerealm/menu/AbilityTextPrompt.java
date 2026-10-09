package net.lukario.frogerealm.menu;

import net.lukario.frogerealm.network.COpenTextPromptPacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * A small pop-up window with a text box. What the player types decides what happens.
 * Everything is defined here on the server; the phrases are never sent to the client,
 * so nobody can read them out of the game files.
 *
 *   private static final AbilityTextPrompt WORDS = AbilityTextPrompt.create("my_words")
 *           .title("Speak")
 *           .hint("Say the words...")
 *           .phrase("Open sesame", (player, level) -> { ... })
 *           .phrase("Close sesame", MyClass::close)
 *           .otherwise((player, level, text) -> player.displayClientMessage(Component.literal("Nothing happens."), true));
 *
 *   // inside an ability:
 *   WORDS.open(player);
 *
 * Matching ignores capital letters, punctuation and extra spaces:
 * "the sentence was god." matches "The sentence was God".
 */
public class AbilityTextPrompt {

    @FunctionalInterface
    public interface UnknownAction {
        void run(ServerPlayer player, ServerLevel level, String typedText);
    }

    public static final int MAX_LENGTH = 64;

    private static final Map<UUID, AbilityTextPrompt> OPEN_PROMPTS = new HashMap<>();

    private final String id;
    private String title = "";
    private String hint = "";
    private final Map<String, AbilityMenu.Action> phrases = new LinkedHashMap<>();
    private UnknownAction otherwise = (player, level, text) -> {};

    private AbilityTextPrompt(String id) {
        this.id = id;
    }

    public static AbilityTextPrompt create(String id) {
        return new AbilityTextPrompt(id);
    }

    /** Text at the top of the window. */
    public AbilityTextPrompt title(String title) {
        this.title = title;
        return this;
    }

    /** Grey placeholder text shown in the empty text box. */
    public AbilityTextPrompt hint(String hint) {
        this.hint = hint;
        return this;
    }

    /** What happens when the player types this phrase. */
    public AbilityTextPrompt phrase(String phrase, AbilityMenu.Action action) {
        phrases.put(normalize(phrase), action);
        return this;
    }

    /** What happens when the typed text matches no phrase (default: nothing). */
    public AbilityTextPrompt otherwise(UnknownAction action) {
        this.otherwise = action;
        return this;
    }

    public void open(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        OPEN_PROMPTS.put(serverPlayer.getUUID(), this);
        PacketHandler.sendToPlayer(new COpenTextPromptPacket(id, title, hint), serverPlayer);
    }

    /** Called by STextPromptAnswerPacket. Empty text = closed without answering. */
    public static void handleAnswer(ServerPlayer player, String promptId, String text) {
        AbilityTextPrompt prompt = OPEN_PROMPTS.get(player.getUUID());
        if (prompt == null || !prompt.id.equals(promptId)) return;
        OPEN_PROMPTS.remove(player.getUUID());

        if (text.length() > MAX_LENGTH) text = text.substring(0, MAX_LENGTH);
        String key = normalize(text);
        if (key.isEmpty()) return;
        if (Root.has(player, RootRestriction.ABILITIES)) return; // got rooted while typing

        AbilityMenu.Action action = prompt.phrases.get(key);
        if (action != null) {
            action.run(player, player.serverLevel());
        } else {
            prompt.otherwise.run(player, player.serverLevel(), text);
        }
    }

    /** lower case, letters/digits/spaces only, single spaces */
    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N} ]", "")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
