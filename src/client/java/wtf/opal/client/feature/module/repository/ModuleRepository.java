package wtf.opal.client.feature.module.repository;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.UnknownModuleException;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ModuleRepository {

    private final ClassToInstanceMap<Module> classToInstanceMap;
    private final ImmutableMap<String, Module> idToInstanceMap;

    private ModuleRepository(final ClassToInstanceMap<Module> classToInstanceMap, final ImmutableMap<String, Module> idToInstanceMap) {
        this.classToInstanceMap = classToInstanceMap;
        this.idToInstanceMap = idToInstanceMap;
    }

    public void findModule(final String id, final Consumer<Module> moduleConsumer, final Consumer<UnknownModuleException> exceptionHandler) {
        try {
            moduleConsumer.accept(getModule(id));
        } catch (UnknownModuleException e) {
            exceptionHandler.accept(e);
        }
    }

    public <T extends Module> T getModule(final Class<T> moduleClass) {
        return classToInstanceMap.getInstance(moduleClass);
    }

    public Module getModule(final String id) throws UnknownModuleException {
        Module module = idToInstanceMap.get(id);
        if (module == null)
            throw new UnknownModuleException(id);
        return module;
    }

    public Collection<Module> getModules() {
        return classToInstanceMap.values();
    }

    public Collection<Module> getModulesInCategory(final ModuleCategory category) {
        return classToInstanceMap.values().stream()
                .filter(module -> category.equals(module.getCategory()))
                .collect(Collectors.toList());
    }

    public static ModuleRepository fromModules(final Module... modules) {
        final Builder builder = builder();
        for (final Module module : modules) {
            builder.register(module);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ImmutableClassToInstanceMap.Builder<Module> classToInstanceMapBuilder = ImmutableClassToInstanceMap.builder();
        private final ImmutableMap.Builder<String, Module> idToInstanceMapBuilder = ImmutableMap.builder();

        private Builder() {

        }

        @SuppressWarnings("unchecked")
        public Builder register(final Module module) {
            classToInstanceMapBuilder.put((Class<Module>) module.getClass(), module);
            idToInstanceMapBuilder.put(module.getId(), module);
            return this;
        }

        public ModuleRepository build() {
            return new ModuleRepository(classToInstanceMapBuilder.build(), idToInstanceMapBuilder.build());
        }
    }
}