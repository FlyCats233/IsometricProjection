package io.github.flycats233.isometricprojection;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

final class PonderCommand {
    private PonderCommand() {}

    static boolean available() {
        return ModList.get().isLoaded("ponder");
    }

    static void register() {
        if (available()) PenrosePonder.register();
        NeoForge.EVENT_BUS.addListener(PonderCommand::registerCommand);
    }

    private static void registerCommand(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("isometricprojection")
                .then(Commands.literal("ponder").executes(context -> {
                    if (!available()) {
                        context.getSource().sendFailure(Component.translatable("message.isometricprojection.ponder_missing"));
                        return 0;
                    }
                    PenrosePonder.open();
                    return 1;
                })));
    }
}
