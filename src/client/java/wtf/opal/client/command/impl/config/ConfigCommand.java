package wtf.opal.client.command.impl.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.opal.client.command.Command;
import wtf.opal.client.command.arguments.ConfigArgumentType;
import wtf.opal.utility.data.SaveUtility;
import wtf.opal.utility.misc.chat.ChatUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Interacts with configs.", "c");
    }

    @Override
    protected void onCommand(final LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("save").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();

            SaveUtility.saveConfig(configName);
            
            // Send success message to player
            ChatUtility.success("Successfully saved config: " + configName);

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("list").executes(context -> {
            // TODO: Implement config listing functionality
            ChatUtility.print("Config listing not implemented yet");
            
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("load").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();
            
            // TODO: Implement config loading functionality
            ChatUtility.print("Config loading not implemented yet");

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("delete").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();
            
            // TODO: Implement config deletion functionality
            ChatUtility.print("Config deletion not implemented yet");

            return SINGLE_SUCCESS;
        })));
    }
}
