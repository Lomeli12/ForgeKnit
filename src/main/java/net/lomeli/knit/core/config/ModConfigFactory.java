package net.lomeli.knit.core.config;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ModConfigFactory {
    public SidedConfig commonConfig;
    public SidedConfig clientConfig;
    public SidedConfig serverConfig;

    public final String modID;

    public ModConfigFactory(String modID) {
        this.modID = modID;
        this.commonConfig = new SidedConfig(modID);
        this.clientConfig = new SidedConfig(modID);
        this.serverConfig = new SidedConfig(modID);
    }

    public ModConfigFactory addConfig(ModConfig.Type type, Class<?> clazz) {
        switch (type) {
            case CLIENT -> clientConfig.addClassConfig(clazz);
            case SERVER -> serverConfig.addClassConfig(clazz);
            default -> commonConfig.addClassConfig(clazz);
        }
        return this;
    }

    public void build() {
        commonConfig.buildSpec();
        if (commonConfig.containsSpecs() && commonConfig.getSpec() != null)
            ModLoadingContext.get().getActiveContainer().addConfig(
                    new ModConfig(ModConfig.Type.COMMON, commonConfig.getSpec(),
                            ModLoadingContext.get().getActiveContainer(), modID + ".toml"));

        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
            serverConfig.buildSpec();


            if (serverConfig.containsSpecs() && serverConfig.getSpec() != null)
                ModLoadingContext.get().getActiveContainer().addConfig(
                        new ModConfig(ModConfig.Type.SERVER, serverConfig.getSpec(),
                                ModLoadingContext.get().getActiveContainer(), modID + "-server.toml"));
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            clientConfig.buildSpec();

            if (clientConfig.containsSpecs() && clientConfig.getSpec() != null)
                ModLoadingContext.get().getActiveContainer().addConfig(
                        new ModConfig(ModConfig.Type.CLIENT, clientConfig.getSpec(),
                                ModLoadingContext.get().getActiveContainer(), modID + "-client.toml"));
        });

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::configEvent);

    }

    public void reloadConfigs() {
        commonConfig.reloadConfig();
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> serverConfig.reloadConfig());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> clientConfig.reloadConfig());
    }

    public void configEvent(final ModConfigEvent event) {
        var config = event.getConfig();
        if (config.getFileName().startsWith(modID)) {
            if (commonConfig.containsSpecs() && config.getSpec() == commonConfig.getSpec())
                commonConfig.bakeConfig(config);
            if (clientConfig.containsSpecs() && config.getSpec() == clientConfig.getSpec())
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> clientConfig.bakeConfig(config));
            if (serverConfig.containsSpecs() && config.getSpec() == serverConfig.getSpec())
                DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> serverConfig.bakeConfig(config));
        }
    }
}
