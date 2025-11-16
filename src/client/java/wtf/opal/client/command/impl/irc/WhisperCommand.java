package wtf.opal.client.command.impl.irc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.opal.client.command.Command;
import wtf.opal.client.feature.helper.impl.chat.ChatHelper;
import wtf.opal.utility.misc.chat.ChatUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class WhisperCommand extends Command {

    public WhisperCommand() {
        super("whisper", "Allows you to direct message Opal users.", "w", "msg");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("user", StringArgumentType.word()).executes(context -> {
//            if (ClientSocket.getInstance().isAuthenticated()) {
//                final String user = context.getArgument("user", String.class);
//
//                ChatHelper.getInstance().setWhisperUsername(user);
//                ChatHelper.getInstance().setChannel(ChatChannel.WHISPER);
//            } else {
//                ChatUtility.error("You are not connected to the IRC server!");
//            }
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("user", StringArgumentType.word()).then(argument("message", StringArgumentType.greedyString()).executes(context -> {
//            if (ClientSocket.getInstance().isAuthenticated()) {
//                final String user = context.getArgument("user", String.class);
//                final String message = context.getArgument("message", String.class);
//
//                ClientSocket.getInstance().sendPacket(new C2SIRCPacket(IRCPacketType.WHISPER_RECEIVED, user, message));
//            } else {
//                ChatUtility.error("You are not connected to the IRC server!");
//            }
            return SINGLE_SUCCESS;
        })));
    }
}
