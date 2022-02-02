package net.lomeli.knit.core.config;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.lomeli.knit.Knit;
import net.lomeli.knit.core.config.annotations.Config;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// Renamed it since SidedConfig doesn't make sense when the COMMON config type exists.
public class PolarConfig {
    private final List<Class<?>> configClasses;
    @SuppressWarnings("rawtypes")
    private final Map<Field, ForgeConfigSpec.ConfigValue> configValues;
    private final String modID;
    private ForgeConfigSpec configSpec;
    private ModConfig config;

    public PolarConfig(String modID) {
        this.modID = modID;
        configClasses = Lists.newArrayList();
        configValues = Maps.newHashMap();
    }

    protected void addClassConfig(Class<?> clazz) {
        if (!configClasses.contains(clazz))
            configClasses.add(clazz);
    }

    protected void readClassConfig(ForgeConfigSpec.Builder builder) {
        if (configClasses.isEmpty())
            return;
        List<Field> fields = Lists.newArrayList();
        configClasses.forEach(clazz -> {
            var clazzFields = clazz.getDeclaredFields();
            for (Field f : clazzFields) {
                if (isValidField(f)) {
                    f.setAccessible(true);
                    fields.add(f);
                }
            }
        });

        if (fields.isEmpty()) return;

        fields.sort((o1, o2) -> {
            var info1 = o1.getAnnotation(Config.class);
            var info2 = o2.getAnnotation(Config.class);
            return info1.category().compareTo(info2.category());
        });

        parseFields(fields, builder);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void parseFields(List<Field> fields, ForgeConfigSpec.Builder builder) {
        var currentCategory = new AtomicReference<String>("");
        var needsPop = new AtomicBoolean(false);
        fields.forEach(field -> {
            var configInfo = field.getAnnotation(Config.class);

            if (!currentCategory.get().equals(configInfo.category())) {
                if (needsPop.get()) {
                    builder.pop();
                    needsPop.set(false);
                }

                currentCategory.set(configInfo.category());

                if (!Strings.isNullOrEmpty(configInfo.categoryComment()))
                    builder.comment(configInfo.categoryComment());
                //TODO: Fix translation. Only doesn't use translation key properly
                if (!Strings.isNullOrEmpty(configInfo.categoryTranslation()))
                    builder.translation(configInfo.categoryTranslation());

                builder.push(currentCategory.get());
                needsPop.set(true);
            }

            Object defaultValue = null;
            try {
                defaultValue = field.get(null);
            } catch (IllegalAccessException ex) {
                Knit.log.error("Failed to get default value", ex);
            }

            String name = field.getName();

            if (!Strings.isNullOrEmpty(configInfo.comment())) builder.comment(configInfo.comment());
            //TODO: Fix translation. Only doesn't use translation key properly
            if (!Strings.isNullOrEmpty(configInfo.translation())) builder.comment(configInfo.translation());

            if (configInfo.requireRestart())
                builder.worldRestart();

            ForgeConfigSpec.ConfigValue fieldSpec = null;
            if (defaultValue != null) {
                if (field.getType() == int.class) {
                    fieldSpec = builder.defineInRange(name, (int) defaultValue, (int) configInfo.minValue(), (int)
                            configInfo.maxValue());
                } else if (field.getType() == boolean.class) {
                    fieldSpec = builder.define(name, (boolean) defaultValue);
                } else if (field.getType() == double.class) {
                    fieldSpec = builder.defineInRange(name, (double) defaultValue, configInfo.minValue(),
                            configInfo.maxValue());
                } else if (field.getType() == String.class) {
                    fieldSpec = builder.define(name, defaultValue.toString());
                } else if (field.getType().isEnum()) {
                    fieldSpec = builder.defineEnum(name, (Enum) defaultValue);
                } else if (List.class.isAssignableFrom(field.getType())) {
                    var stBuilder = new StringBuilder();
                    ((List) defaultValue).forEach(item -> stBuilder.append(item.toString()).append(";"));
                    fieldSpec = builder.define(name, stBuilder.toString());
                }
            }

            if (fieldSpec != null) {
                configValues.put(field, fieldSpec);
            }
        });
        if (needsPop.get())
            builder.pop();
    }

    protected void buildSpec() {
        var builder = new ForgeConfigSpec.Builder();

        readClassConfig(builder);

        configSpec = builder.build();
    }

    protected void reloadConfig() {
        if (config != null) {
            config.save();
            bakeConfig(config);
        }
    }

    public void bakeConfig(final ModConfig config) {
        this.config = config;

        configValues.forEach((field, spec) -> {
            try {
                if (List.class.isAssignableFrom(field.getType()))
                    field.set(null, Arrays.asList(spec.get().toString().split(";")));
                else field.set(null, spec.get());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isValidField(Field field) {
        return field.isAnnotationPresent(Config.class) && !Modifier.isTransient(field.getModifiers()) && (
                field.getType() == int.class ||
                        field.getType() == double.class ||
                        field.getType() == boolean.class ||
                        field.getType() == String.class ||
                        field.getType().isEnum() ||
                        List.class.isAssignableFrom(field.getType())
        );
    }

    public ForgeConfigSpec getSpec() {
        return configSpec;
    }

    public boolean containsSpecs() {
        return !configValues.isEmpty();
    }
}
