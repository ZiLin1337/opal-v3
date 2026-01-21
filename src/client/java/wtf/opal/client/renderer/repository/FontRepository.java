package wtf.opal.client.renderer.repository;

import net.fabricmc.loader.impl.launch.knot.Knot;
import wtf.opal.client.renderer.text.NVGTextRenderer;

import java.io.InputStream;
import java.util.HashMap;

public final class FontRepository {

    private static final HashMap<String, NVGTextRenderer> TEXT_RENDERER_MAP = new HashMap<>();

    public static NVGTextRenderer getFont(final String name) {
        return getFontFromResources(name, "assets/opal/fonts/" + name + ".ttf");
    }

    public static NVGTextRenderer getFontFromResources(final String key, final String... resourcePaths) {
        final NVGTextRenderer font = getOptionalFontFromResources(key, resourcePaths);
        if (font != null) {
            return font;
        }

        throw new RuntimeException("Font not found: " + key);
    }

    public static NVGTextRenderer getOptionalFontFromResources(final String key, final String... resourcePaths) {
        if (TEXT_RENDERER_MAP.containsKey(key)) {
            return TEXT_RENDERER_MAP.get(key);
        }

        for (final String resourcePath : resourcePaths) {
            final InputStream stream = Knot.getLauncher().getTargetClassLoader().getResourceAsStream(resourcePath);
            if (stream != null) {
                TEXT_RENDERER_MAP.put(key, new NVGTextRenderer(key, stream));
                return TEXT_RENDERER_MAP.get(key);
            }
        }

        return null;
    }
}
