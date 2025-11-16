package wtf.opal.client.command.impl.irc;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.opal.client.command.Command;
import wtf.opal.utility.misc.chat.ChatUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class OnlineCommand extends Command {

    public OnlineCommand() {
        super("online", "Displays the online Opal users to you.");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
//            if (ClientSocket.getInstance().isAuthenticated()) {
//                ClientSocket.getInstance().sendPacket(new C2SIRCPacket(IRCPacketType.LIST_ONLINE));
//            } else {
//                ChatUtility.error("You are not connected to the IRC server!");
//            }
            return SINGLE_SUCCESS;
        });
    }
}
