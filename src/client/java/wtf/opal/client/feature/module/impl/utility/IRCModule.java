package wtf.opal.client.feature.module.impl.utility;

import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;

public final class IRCModule extends Module {

    public IRCModule() {
        super("IRC", "Lets you chat with other Opal users.", ModuleCategory.UTILITY);
        setEnabled(true);
    }

}
