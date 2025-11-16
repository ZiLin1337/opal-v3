package wtf.opal.client.renderer.repository;

import net.fabricmc.loader.impl.launch.knot.Knot;
import wtf.opal.client.renderer.text.NVGTextRenderer;

import java.io.InputStream;
import java.util.HashMap;

public final class FontRepository {

    private static final HashMap<String, NVGTextRenderer> TEXT_RENDERER_MAP = new HashMap<>();

    public static NVGTextRenderer getFont(final String name) {
        if (TEXT_RENDERER_MAP.containsKey(name))
            return TEXT_RENDERER_MAP.get(name);

        final InputStream pathURL = Knot.getLauncher().getTargetClassLoader().getResourceAsStream("assets/opal/fonts/" + name + ".ttf");

        if (pathURL != null) {
            TEXT_RENDERER_MAP.put(name, new NVGTextRenderer(name, pathURL));

            return TEXT_RENDERER_MAP.get(name);
        }

        throw new RuntimeException("Font not found: " + name);
    }
}