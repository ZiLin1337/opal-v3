package wtf.opal.client.command.impl.irc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.opal.client.command.Command;
import wtf.opal.client.feature.helper.impl.chat.ChatHelper;
import wtf.opal.utility.misc.chat.ChatUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class ReplyCommand extends Command {

    public ReplyCommand() {
        super("reply", "Reply to a user's whisper.", "r");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("message", StringArgumentType.greedyString()).executes(context -> {
//            if (ClientSocket.getInstance().isAuthenticated()) {
//                final String username = ClientSocket.getInstance().getLastReceivedWhisperUsername();
//                if (username == null) {
//                    ChatUtility.error("There is nobody to reply to!");
//                } else {
//                    final String message = context.getArgument("message", String.class);
//                    ClientSocket.getInstance().sendPacket(new C2SIRCPacket(IRCPacketType.WHISPER_RECEIVED, username, message));
//                }
//            } else {
//                ChatUtility.error("You are not connected to the IRC server!");
//            }
            return SINGLE_SUCCESS;
        }));

        builder.executes(context -> {
//            if (ClientSocket.getInstance().isAuthenticated()) {
//                final String username = ClientSocket.getInstance().getLastReceivedWhisperUsername();
//                if (username == null) {
//                    ChatUtility.error("There is nobody to reply to!");
//                } else {
//                    ChatHelper.getInstance().setWhisperUsername(username);
//                    ChatHelper.getInstance().setChannel(ChatChannel.WHISPER);
//                }
//            } else {
//                ChatUtility.error("You are not connected to the IRC server!");
//            }
            return SINGLE_SUCCESS;
        });
    }
}
