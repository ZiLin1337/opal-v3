package wtf.opal.client.command.impl.config;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import wtf.opal.client.command.Command;
import wtf.opal.client.command.arguments.ConfigArgumentType;
import wtf.opal.utility.data.SaveUtility;
import wtf.opal.utility.misc.chat.ChatUtility;

import java.util.List;

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
            final List<String> configNames = SaveUtility.getConfigNames();
            if (configNames.isEmpty()) {
                ChatUtility.print("No configurations found");
            } else {
                ChatUtility.print("Available configurations:");
                for (String configName : configNames) {
                    ChatUtility.print("  - " + configName);
                }
            }
            
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("load").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();
            
            if (SaveUtility.loadConfigFromFile(configName)) {
                ChatUtility.success("Successfully loaded config: " + configName);
            } else {
                ChatUtility.error("Failed to load config: " + configName + ". Make sure the config exists.");
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("delete").then(argument("config_name", ConfigArgumentType.create()).executes(context -> {
            final String configName = context.getArgument("config_name", String.class).toLowerCase();
            
            if (SaveUtility.deleteConfig(configName)) {
                ChatUtility.success("Successfully deleted config: " + configName);
            } else {
                ChatUtility.error("Failed to delete config: " + configName + ". Make sure the config exists.");
            }

            return SINGLE_SUCCESS;
        })));
    }
}
