package mod.azure.mchalo.network;

import commonnetwork.api.Network;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import io.netty.buffer.Unpooled;
import mod.azure.mchalo.client.gui.GunTableScreenHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class CraftingPacket {
    private static int index;

    public CraftingPacket(int index) {
        CraftingPacket.index = index;
    }

    public static void send(int index) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(index);
        Network.getNetworkHandler().sendToServer(new CraftingPacket(buf.readInt()));
    }

    public static CraftingPacket decode(FriendlyByteBuf buf) {
        return null;
    }

    public static void handle(PacketContext<CraftingPacket> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            AbstractContainerMenu container = ctx.sender().containerMenu;
            if (container instanceof GunTableScreenHandler gunTableScreenHandler) {
                gunTableScreenHandler.setRecipeIndex(index);
                gunTableScreenHandler.switchTo(index);
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
    }
}
