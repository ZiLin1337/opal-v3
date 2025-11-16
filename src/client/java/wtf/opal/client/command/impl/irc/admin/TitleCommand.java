package wtf.opal.client.command.impl.irc.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.opal.client.command.Command;
import wtf.opal.utility.misc.chat.ChatUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class TitleCommand extends Command {

    public TitleCommand() {
        super("title", "Displays title on specified users screen.");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("user", StringArgumentType.word())
                .then(argument("message", StringArgumentType.string())
                        .then(argument("fadeInTicks", IntegerArgumentType.integer())
                                .then(argument("stayTicks", IntegerArgumentType.integer())
                                        .then(argument("fadeOutTicks", IntegerArgumentType.integer())
                                                .executes(context -> {
//                                                    if (ClientSocket.getInstance().isAuthenticated()) {
//                                                        final String user = context.getArgument("user", String.class);
//                                                        final String message = context.getArgument("message", String.class);
//                                                        final int fadeInTicks = context.getArgument("fadeInTicks", Integer.class);
//                                                        final int stayTicks = context.getArgument("stayTicks", Integer.class);
//                                                        final int fadeOutTicks = context.getArgument("fadeOutTicks", Integer.class);
//
//                                                        ClientSocket.getInstance().sendPacket(
//                                                                new C2STitlePacket(user, message, fadeInTicks, stayTicks, fadeOutTicks)
//                                                        );
//                                                    } else {
//                                                        ChatUtility.error("You are not connected to the IRC server!");
//                                                    }
                                                    return SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                )
        );
    }
}
