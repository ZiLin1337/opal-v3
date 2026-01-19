package wtf.opal.client.feature.helper.impl.chat;

import net.minecraft.util.Formatting;
import wtf.opal.utility.misc.chat.ChatUtility;

public final class ChatHelper {

    private String channel = "ALL";
    private String whisperUsername;

    private ChatHelper() {
    }

    public String getChannel() {
        return this.channel;
    }

    public String getWhisperUsername() {
        return this.whisperUsername;
    }

    public void setChannel(final String channel) {
        this.channel = this.channel == channel ? "ALL" : channel;
        ChatUtility.success("You are now in the " + Formatting.GOLD + this.channel + Formatting.GRAY + " channel.");
    }

    public void setWhisperUsername(final String whisperUsername) {
        this.whisperUsername = whisperUsername;
    }

    private static ChatHelper instance;

    public static ChatHelper getInstance() {
        return instance;
    }

    public static void setInstance() {
        instance = new ChatHelper();
    }

}
