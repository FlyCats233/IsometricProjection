package io.github.flycats233.isometricprojection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class LocalCutaway {
    private static boolean enabled = true;
    private static volatile CutawayVolume current = CutawayVolume.DISABLED;
    private static final ThreadLocal<CutawayVolume> COMPILING = new ThreadLocal<>();
    private static ClientLevel lastLevel;

    private LocalCutaway() { }

    public static boolean toggle() {
        enabled = !enabled;
        ClientConfig.CUTAWAY_ENABLED.set(enabled);
        ClientConfig.SPEC.save();
        return enabled;
    }
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static CutawayVolume snapshot() { return current; }
    public static void beginCompile(CutawayVolume volume) { COMPILING.set(volume); }
    public static void endCompile() { COMPILING.remove(); }

    public static boolean hidesDuringCompile(BlockPos pos) {
        CutawayVolume volume = COMPILING.get();
        return volume != null && volume.hides(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean hides(BlockPos pos) {
        return current.hides(pos.getX(), pos.getY(), pos.getZ());
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        CutawayVolume next = CutawayVolume.DISABLED;
        if (enabled && CameraController.active()) {
            BlockPos feet = mc.player.blockPosition();
            next = CutawayVolume.create(true, feet.getX() + .5, mc.player.getEyeY(), feet.getZ() + .5,
                    CameraController.yaw(), CameraController.PITCH, ClientConfig.CUTAWAY_RADIUS.get(), ClientConfig.CUTAWAY_DEPTH.get());
        }
        CutawayVolume previous = current;
        boolean sameLevel = mc.level == lastLevel;
        if (sameLevel && next.equals(previous)) return;
        current = next;
        lastLevel = mc.level;
        if (mc.level == null) return;
        // Only nearby render sections need rebuilding, including the cut boundary's neighbours.
        if (sameLevel) markDirty(previous);
        markDirty(next);
    }

    private static void markDirty(CutawayVolume volume) {
        if (!volume.enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        int minX = (volume.minX() - 1) >> 4;
        int maxX = (volume.maxX() + 1) >> 4;
        int minZ = (volume.minZ() - 1) >> 4;
        int maxZ = (volume.maxZ() + 1) >> 4;
        int minY = Math.max(mc.level.getMinBuildHeight(), volume.firstHiddenY() - 1) >> 4;
        int maxY = Math.min(mc.level.getMaxBuildHeight() - 1, volume.maxY() + 1) >> 4;
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) mc.levelRenderer.setSectionDirty(x, y, z);
    }

    /** Only the camera's selection ray skips cut geometry; physics and eye-to-target checks do not. */
    public static ClipContext selectionContext(Vec3 from, Vec3 to, Entity player) {
        CutawayVolume volume = current;
        return new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player) {
            @Override
            public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
                return volume.hides(pos.getX(), pos.getY(), pos.getZ())
                        ? Shapes.empty() : super.getBlockShape(state, level, pos);
            }
        };
    }
}
