package cc.reconnected.kromer.networking;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;

public class BalanceResponsePacket {
    public static final ResourceLocation ID = new ResourceLocation("rcc-kromer", "balance_response");

    public static FriendlyByteBuf serialize(BigDecimal balance) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(balance.toString());
        return buf;
    }

    public static BigDecimal deserialize(FriendlyByteBuf buf) throws Exception {
        return new BigDecimal(buf.readUtf());
    }
}
