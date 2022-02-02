package net.lomeli.knit;

import net.lomeli.knit.core.config.ModConfigFactory;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Knit.MOD_ID)
public class Knit {
    public static final String MOD_ID = "knit";
    public static final String MOD_NAME = "Knit";

    public static Logger log = LogManager.getLogger(MOD_NAME);
    public static ModConfigFactory knitConfig = new ModConfigFactory(MOD_ID);

    public Knit() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonInit);
    }

    public void commonInit(final FMLCommonSetupEvent event) {
    }
}
