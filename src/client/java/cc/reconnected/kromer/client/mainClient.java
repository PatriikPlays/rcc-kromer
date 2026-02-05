package cc.reconnected.kromer.client;

import cc.reconnected.kromer.Kromer;
import cc.reconnected.kromer.arguments.AddressArgumentType;
import cc.reconnected.kromer.arguments.KromerArgumentInfo;
import cc.reconnected.kromer.arguments.KromerArgumentType;
import cc.reconnected.kromer.networking.BalanceRequestPacket;
import cc.reconnected.kromer.networking.BalanceResponsePacket;
import cc.reconnected.kromer.networking.TransactionPacket;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class mainClient implements ClientModInitializer {
    private final AtomicReference<Optional<BigDecimal>> balance = new AtomicReference<>(Optional.empty());

    @Override
    public void onInitializeClient() {
        ArgumentTypeRegistry.registerArgumentType(
                new ResourceLocation("rcc-kromer", "kromer_amount"),
                KromerArgumentType.class,
                new KromerArgumentInfo()
        );
        ArgumentTypeRegistry.registerArgumentType(
                new ResourceLocation("rcc-kromer", "kromer_address"),
                AddressArgumentType.class,
                SingletonArgumentInfo.contextFree(AddressArgumentType::address)
        );

        AutoConfig.register(KromerClientConfig.class, GsonConfigSerializer::new);
        ConfigHolder<KromerClientConfig> config = AutoConfig.getConfigHolder(KromerClientConfig.class);

        ClientPlayConnectionEvents.JOIN.register((packetListener, sender, client) -> {
            ClientPlayNetworking.send(BalanceRequestPacket.ID, PacketByteBufs.empty());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            balance.set(Optional.empty());
        });

        ClientPlayNetworking.registerGlobalReceiver(TransactionPacket.ID, (client, handler, buf, responseSender) -> {
            TransactionPacket.TransactionWithBalance tx;
            try {
                tx = TransactionPacket.deserialize(buf);
            } catch (Exception e) {
                Kromer.LOGGER.error("Failed to deserialize incoming transaction packet:", e);
                return;
            }

            @Nullable
            BigDecimal balance = tx.balance();

            if (balance != null) {
                this.balance.set(Optional.of(balance));
            }

            if(client.getToasts().queued.size() < 3 && config.getConfig().toastPopup) {
                var toastContents = "Incoming " + tx.transaction().value + "KRO from " + tx.transaction().from + "! ";

                if (balance != null) {
                    toastContents += ("Balance is now " + balance.setScale(2, RoundingMode.DOWN) + "KRO.");
                }

                client.getToasts().addToast(SystemToast.multiline(client, SystemToast.SystemToastIds.TUTORIAL_HINT,
                        Component.literal("Transaction"),
                        Component.literal(toastContents)));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(BalanceResponsePacket.ID, (client, handler, buf, responseSender) -> {
            try {
                balance.set(Optional.of(BalanceResponsePacket.deserialize(buf)));
            } catch (Exception e) {
                Kromer.LOGGER.error("Failed to deserialize incoming balance response packet:", e);
                return;
            }
        });

        ScreenEvents.AFTER_INIT.register((mc, screen, sw, sh) -> {
            if (screen instanceof PauseScreen) {
                ScreenEvents.afterRender(screen).register((scr, guiGraphics, mouseX, mouseY, tickDelta) -> {
                    // if singleplayer, return
                    if (mc.isLocalServer() || !config.getConfig().balanceDisplay) {
                        return;
                    }

                    renderBalanceDisplay(guiGraphics);
                });
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            var mc = net.minecraft.client.Minecraft.getInstance();

            if (mc.isLocalServer() || !config.getConfig().balanceDisplay) {
                return;
            }

            if (mc.options.keyPlayerList.isDown() && mc.player != null) {
                renderBalanceDisplay(guiGraphics);
            }
        });
    }

    private void renderBalanceDisplay(GuiGraphics guiGraphics) {
        var mc = net.minecraft.client.Minecraft.getInstance();

        int x = 10;
        int y = 10;
        int valueX = x + mc.font.width("Balance: ");

        guiGraphics.drawString(mc.font, "Balance: ", x, y, 0x55FF55, true);

        balance.get().ifPresentOrElse(bal -> {
            guiGraphics.drawString(mc.font, bal.setScale(2, RoundingMode.DOWN) + "KRO", valueX, y, 0x00AA00, true);
        }, () -> {
            guiGraphics.drawString(mc.font, "Loading..", valueX, y, 0xAAAAAA, true);
        });
    }
}
