package cc.reconnected.kromer.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import ovh.sad.jkromer.models.Transaction;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

public class TransactionPacket {
    public static final ResourceLocation ID = new ResourceLocation("rcc-kromer", "transaction");

    public static FriendlyByteBuf serialize(Transaction tx, @Nullable BigDecimal balance) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        writeTransaction(buf, tx);
        buf.writeBoolean(balance != null);
        if (balance != null) {
            buf.writeUtf(balance.toString());
        }
        return buf;
    }

    public record TransactionWithBalance(Transaction transaction, @Nullable BigDecimal balance) {}
    public static TransactionWithBalance deserialize(FriendlyByteBuf buf) throws Exception {
        Transaction tx = readTransaction(buf);

        boolean hasBalance = buf.readBoolean();

        BigDecimal balance = null;

        if (hasBalance) {
             balance = new BigDecimal(buf.readUtf());
        }

        return new TransactionWithBalance(tx, balance);
    }

    private static void writeTransaction(FriendlyByteBuf buf, Transaction tx) {
        buf.writeUtf(Objects.requireNonNullElse(tx.sent_metaname, ""));
        buf.writeInt(tx.id);
        buf.writeUtf(tx.from);
        buf.writeUtf(tx.to);
        buf.writeUtf(tx.value.toString());
        buf.writeLong(tx.time.getTime());
        buf.writeUtf(Objects.requireNonNullElse(tx.name, ""));
        buf.writeUtf(Objects.requireNonNullElse(tx.metadata, ""));
        buf.writeUtf(Objects.requireNonNullElse(tx.sent_name, ""));
        buf.writeUtf(tx.type);
    }

    private static Transaction readTransaction(FriendlyByteBuf buf) {
        String sent_metaname = buf.readUtf();
        int id = buf.readInt();
        String from = buf.readUtf();
        String to = buf.readUtf();
        BigDecimal value = new BigDecimal(buf.readUtf());
        Date time = new Date(buf.readLong());
        String name = buf.readUtf();
        String metadata = buf.readUtf();
        String sent_name = buf.readUtf();
        String type = buf.readUtf();

        return new Transaction(sent_metaname, id, from, to, value, time, name, metadata, sent_name, type);
    }
}
