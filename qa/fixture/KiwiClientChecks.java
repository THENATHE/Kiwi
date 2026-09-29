package kiwi.qa;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;
import snownee.kiwi.config.ClothConfigIntegration;

/** Test-only entrypoint: validate mixin application and render configuration screens. */
public final class KiwiClientChecks implements ClientModInitializer {
    private final List<String> namespaces = new ArrayList<>(List.of("kiwi"));
    private int ticks;
    private int index = -1;

    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("snowrealmagic")) namespaces.add("snowrealmagic");
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.gui.overlay() != null) return;
            if (index < 0) {
                if (!(mc.gui.screen() instanceof TitleScreen)) return;
                // Apply mixins for inventory/rendering classes without requiring an existing world.
                for (String name : List.of(
                        "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
                        "net.minecraft.client.gui.screens.inventory.InventoryScreen",
                        "net.minecraft.client.gui.screens.inventory.EffectsInInventory",
                        "net.minecraft.client.renderer.entity.EntityRenderDispatcher",
                        "net.minecraft.client.renderer.fog.environment.LavaFogEnvironment",
                        "net.minecraft.client.renderer.ScreenEffectRenderer")) {
                    try { Class.forName(name); }
                    catch (ClassNotFoundException e) { throw new AssertionError("KIWI_CLIENT_QA_FAIL " + name, e); }
                }
                index = 0;
            }
            if (ticks == 0) {
                var screen = ClothConfigIntegration.create(mc.gui.screen(), namespaces.get(index));
                if (screen == null) throw new AssertionError("KIWI_CLIENT_QA_FAIL missing config " + namespaces.get(index));
                mc.gui.setScreen(screen);
                System.out.println("KIWI_CLIENT_QA_CONFIG_OPEN " + namespaces.get(index));
            }
            if (++ticks == 40) {
                System.out.println("KIWI_CLIENT_QA_CONFIG_PASS " + namespaces.get(index));
                ticks = 0;
                if (++index == namespaces.size()) {
                    System.out.println("KIWI_CLIENT_QA_PASS");
                    mc.stop();
                }
            }
        });
    }
}
