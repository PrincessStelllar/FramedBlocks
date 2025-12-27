package io.github.xfacthd.framedblocks.common.net;

import io.github.xfacthd.framedblocks.common.net.payload.clientbound.ClientboundCullingUpdatePayload;
import io.github.xfacthd.framedblocks.common.net.payload.clientbound.ClientboundOpenSignScreenPayload;
import io.github.xfacthd.framedblocks.common.net.payload.serverbound.ServerboundEncodeFramingSawPatternPayload;
import io.github.xfacthd.framedblocks.common.net.payload.serverbound.ServerboundSelectFramingSawRecipePayload;
import io.github.xfacthd.framedblocks.common.net.payload.serverbound.ServerboundSignUpdatePayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler
{
    private static final String PROTOCOL_VERSION = "3";

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registerNetworkThreadPayloads(registrar);
        registerMainThreadPayloads(registrar);
    }

    private static void registerNetworkThreadPayloads(PayloadRegistrar registrar)
    {
        registrar.executesOn(HandlerThread.NETWORK)
                .playToServer(
                        ServerboundSignUpdatePayload.TYPE,
                        ServerboundSignUpdatePayload.CODEC,
                        ServerboundSignUpdatePayload::handle
                );
    }

    private static void registerMainThreadPayloads(PayloadRegistrar registrar)
    {
        registrar.executesOn(HandlerThread.MAIN)
                .playToClient(
                        ClientboundOpenSignScreenPayload.TYPE,
                        ClientboundOpenSignScreenPayload.CODEC
                )
                .playToClient(
                        ClientboundCullingUpdatePayload.TYPE,
                        ClientboundCullingUpdatePayload.CODEC
                )
                .playToServer(
                        ServerboundSelectFramingSawRecipePayload.TYPE,
                        ServerboundSelectFramingSawRecipePayload.CODEC,
                        ServerboundSelectFramingSawRecipePayload::handle
                )
                .playToServer(
                        ServerboundEncodeFramingSawPatternPayload.TYPE,
                        ServerboundEncodeFramingSawPatternPayload.STREAM_CODEC,
                        ServerboundEncodeFramingSawPatternPayload::handle
                );
    }

    private NetworkHandler() { }
}
