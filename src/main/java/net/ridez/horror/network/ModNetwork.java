package net.ridez.horror.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.item.custom.ToyGunItem;

// The mod's network channel: messages from a player's game to the server.
// - Toy Gun: the reload key was pressed, right-click toggled the scope, or left-click fired it (the server checks
//   the player really holds the gun, loaded to fire, before doing anything).
public class ModNetwork {
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel()
            .messageBuilder(ToyGunAction.class, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ToyGunAction::encode)
            .decoder(ToyGunAction::decode)
            .consumerMainThread(ToyGunAction::handle)
            .add();

    // Builds the channel (called once while the mod loads).
    public static void register() {
    }

    public static void sendToServer(Object pMessage) {
        CHANNEL.send(pMessage, PacketDistributor.SERVER.noArg());
    }

    public enum ToyGunAction {
        RELOAD, FIRE, SCOPE;

        void encode(FriendlyByteBuf pBuf) {
            pBuf.writeEnum(this);
        }

        static ToyGunAction decode(FriendlyByteBuf pBuf) {
            return pBuf.readEnum(ToyGunAction.class);
        }

        void handle(CustomPayloadEvent.Context pContext) {
            ServerPlayer player = pContext.getSender();
            if (player == null) {
                return;
            }
            switch (this) {
                case RELOAD -> ToyGunItem.startReload(player);
                case FIRE -> ToyGunItem.fire(player);
                case SCOPE -> ToyGunItem.toggleScope(player);
            }
        }
    }
}
