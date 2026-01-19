package wtf.opal.client.command.impl.irc.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.opal.client.command.Command;
import wtf.opal.utility.misc.chat.ChatUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class CrashCommand extends Command {

    public CrashCommand() {
        super("crash", "Crashes the specified user.");
    }

    @Override
    protected void onCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("user", StringArgumentType.word()).executes(context -> {
            return SINGLE_SUCCESS;
        }));
    }
}
