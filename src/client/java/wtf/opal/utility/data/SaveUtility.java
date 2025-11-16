package wtf.opal.utility.data;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.ibm.icu.impl.Pair;
import wtf.opal.client.OpalClient;
import wtf.opal.client.binding.BindingService;
import wtf.opal.client.binding.IBindable;
import wtf.opal.client.binding.type.InputType;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.UnknownModuleException;
import wtf.opal.client.feature.module.property.Property;

import wtf.opal.utility.data.serializer.PairSerializer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static wtf.opal.client.Constants.DIRECTORY;


public final class SaveUtility {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Pair.class, new PairSerializer())
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static final BindingService BINDING_SERVICE = OpalClient.getInstance().getBindRepository().getBindingService();

    private SaveUtility() {
    }

    public static void saveBindings() {
        try {
            if (!DIRECTORY.exists()) {
                DIRECTORY.mkdir();
            }

            final File file = new File(DIRECTORY, "bindings.json");

            final JsonArray bindingsArray = new JsonArray();
            for (final Pair<Integer, InputType> binding : BINDING_SERVICE.getBindingMap().keySet()) {
                final JsonObject bindingJson = new JsonObject();
                bindingJson.addProperty("keyCode", binding.first);

                JsonArray bindablesArray = new JsonArray();
                for (IBindable bindable : BINDING_SERVICE.getBindingMap().get(binding)) {
                    if (bindable instanceof Module module) {
                        JsonObject moduleJson = new JsonObject();
                        moduleJson.addProperty("module", module.getId());
                        bindablesArray.add(moduleJson);
                    } else if (bindable instanceof Config config) {
                        JsonObject configJson = new JsonObject();
                        configJson.addProperty("config", config.getName());
                        bindablesArray.add(configJson);
                    }
                }
                bindingJson.add("bindables", bindablesArray);

                bindingsArray.add(bindingJson);
            }

            Files.writeString(
                    file.toPath(),
                    GSON.toJson(bindingsArray)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadBindings() {
        try (final FileReader reader = new FileReader(new File(DIRECTORY, "bindings.json"))) {
            final JsonArray bindingsArray = JsonParser.parseReader(reader).getAsJsonArray();

            for (final JsonElement bindingElement : bindingsArray) {
                final JsonObject bindingJson = bindingElement.getAsJsonObject();

                final int keyCode = bindingJson.get("keyCode").getAsInt();
                final InputType inputType = keyCode < 10 ? InputType.MOUSE : InputType.KEYBOARD;

                final JsonArray bindablesArray = bindingJson.getAsJsonArray("bindables");
                for (final JsonElement bindableElement : bindablesArray) {
                    final JsonObject bindableJson = bindableElement.getAsJsonObject();

                    if (bindableJson.has("module")) {
                        final String moduleID = bindableJson.get("module").getAsString();
                        final Module module = OpalClient.getInstance().getModuleRepository().getModule(moduleID);
                        BINDING_SERVICE.register(keyCode, module, inputType);
                    } else if (bindableJson.has("config")) {
                        final String configName = bindableJson.get("config").getAsString();
                        final Config config = new Config(configName);

                        BINDING_SERVICE.register(keyCode, config, inputType);
                    }
                }
            }
        } catch (IOException | UnknownModuleException e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig(final String name) {
    }

    public static boolean loadConfig(final String jsonString) {
        try {
            final List<?> jsonModules = GSON.fromJson(jsonString, List.class);

            for (final Object jsonModuleObj : jsonModules) {
                final LinkedTreeMap<?, ?> jsonModule = (LinkedTreeMap<?, ?>) jsonModuleObj;
                final String jsonModuleID = (String) jsonModule.get("name");
                final Boolean jsonEnabled = (Boolean) jsonModule.get("enabled");
                final Boolean jsonVisible = (Boolean) jsonModule.get("visible");
                final List<?> jsonProperties = (List<?>) jsonModule.get("properties");

                for (final Module clientModule : OpalClient.getInstance().getModuleRepository().getModules()) {
                    if (jsonModuleID.equals(clientModule.getId())) {

                        if (jsonEnabled != null && jsonEnabled != clientModule.isEnabled()) {
                            clientModule.setEnabled(jsonEnabled);
                        }
                        if (jsonVisible != null && jsonVisible != clientModule.isVisible()) {
                            clientModule.setVisible(jsonVisible);
                        }

                        for (final Object jsonPropertyObj : jsonProperties) {
                            final LinkedTreeMap<?, ?> jsonProperty = (LinkedTreeMap<?, ?>) jsonPropertyObj;
                            final String propertyName = (String) jsonProperty.get("name");
                            final Object propertyValue = jsonProperty.get("value");

                            for (final Property<?> clientProperty : clientModule.getPropertyList()) {
                                if (propertyName.equals(clientProperty.getId())) {
                                    clientProperty.applyValue(propertyValue);
                                }
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
