package io.github.flycats233.isometricprojection;

import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.NeoForge;

final class PenrosePonder extends PonderUI {
    private static final String MOD_ID = "isometricprojection";
    private static final String SCENE = "penrose_crossing";
    private static final ResourceLocation SUBJECT = ResourceLocation.withDefaultNamespace("quartz_block");

    private static final String EDITOR_SCENE = "occlusion_editor";
    private static final ResourceLocation TOOL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "occlusion_tool");
    private static final ResourceLocation THEME = ResourceLocation.fromNamespaceAndPath(MOD_ID, "basics");
    private static final Set<PonderUI> INITIALIZED = Collections.newSetFromMap(new WeakHashMap<>());

    private PenrosePonder(boolean editor) {
        super(PonderIndex.getSceneAccess().compile(editor ? TOOL : SUBJECT));
    }

    static void register() {
        PonderIndex.addPlugin(new PonderPlugin() {
            @Override
            public String getModId() {
                return MOD_ID;
            }

            @Override
            public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
                for (var component : List.of(SUBJECT, TOOL)) {
                    helper.addStoryBoard(component, SCENE, PenrosePonder::storyboard, THEME);
                    helper.addStoryBoard(component, EDITOR_SCENE, PenrosePonder::editorStoryboard, THEME)
                            .orderAfter(MOD_ID, SCENE);
                }
            }
            @Override
            public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
                helper.registerTag(THEME)
                        .title("Isometric Projection")
                        .description("Learn illusion crossings and occlusion editing to build a Penrose triangle.")
                        .item(net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK)
                        .addToIndex()
                        .register();
                helper.addToTag(THEME).add(SUBJECT).add(TOOL);
            }
        });
        NeoForge.EVENT_BUS.addListener(PenrosePonder::initializeChapter);
    }

    private static void initializeChapter(ClientTickEvent.Post event) {
        if (!(Minecraft.getInstance().screen instanceof PonderUI ui)
                || !ui.getSubject().is(ModItems.OCCLUSION_TOOL.get()) || !INITIALIZED.add(ui)) return;
        // Keep the shared chapter order. Ponder has no public initial-page selector;
        // navigate once after its first tick resolves the topic entry point. This covers W too.
        // Remember screen instances weakly so resizing or returning does not reset the page.
        if (ui.getActiveScene().getId().equals(ResourceLocation.fromNamespaceAndPath(MOD_ID, SCENE)))
            ui.mouseScrolled(0, 0, 0, 1);
    }

    static void open() {
        ScreenOpener.open(null, new PenrosePonder(false));
    }

    static void openEditor() {
        ScreenOpener.open(null, new PenrosePonder(true));
    }

    private static void editorStoryboard(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title(EDITOR_SCENE, "Editing Penrose occlusion");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(.8f);
        scene.addInstruction(ponder -> {
            ponder.getTransform().xRotation.startWithValue(-CameraController.PITCH);
            ponder.getTransform().yRotation.startWithValue(225);
        });
        var upper = new net.minecraft.core.BlockPos(5, 5, 4);
        var lower = upper.below();
        var b = new net.minecraft.core.BlockPos(1, 1, 7);
        var selectUpper = util.select().position(upper);
        var selectLower = util.select().position(lower);
        var selectB = util.select().position(b);
        scene.world().showSection(util.select().everywhere().substract(selectB), Direction.DOWN);
        var coveringSection = scene.world().showIndependentSection(new RenderSelection(selectB, util.select().everywhere()), Direction.DOWN);
        scene.idle(25);
        scene.overlay().showText(100).pointAt(Vec3.atCenterOf(upper)).text("The pillar hides the rear beam. Its upper TWO blocks must both be covered by the same beam endpoint.");
        scene.idle(105);
        scene.addKeyframe();
        scene.rotateCameraY(-90);
        scene.idle(55);
        scene.overlay().showOutline(PonderPalette.RED, "editor_a1", selectUpper, 180);
        editorClick(scene, Vec3.atCenterOf(upper));
        scene.overlay().showText(80).pointAt(Vec3.atCenterOf(upper)).text("First right-click the TOP pillar block A1: this block should be hidden.");
        scene.idle(85);
        scene.overlay().showOutline(PonderPalette.BLUE, "editor_b1", util.select().position(b), 90);
        editorClick(scene, Vec3.atCenterOf(b));
        editorArrow(scene, upper, b, 90);
        scene.overlay().showText(85).pointAt(Vec3.atCenterOf(b)).text("Then right-click the beam endpoint B. This creates A1 -> B, meaning B covers A1.");
        scene.idle(90);
        scene.addKeyframe();
        scene.overlay().showOutline(PonderPalette.RED, "editor_a2", selectLower, 180);
        editorClick(scene, Vec3.atCenterOf(lower));
        scene.overlay().showText(90).pointAt(Vec3.atCenterOf(lower)).text("Next right-click A2, one block below A1. One relationship alone leaves part of the pillar in front.");
        scene.idle(95);
        scene.overlay().showOutline(PonderPalette.RED, "editor_a1_final", selectUpper, 100);
        scene.overlay().showOutline(PonderPalette.BLUE, "editor_b2", util.select().position(b), 100);
        editorClick(scene, Vec3.atCenterOf(b));
        editorArrow(scene, upper, b, 100);
        editorArrow(scene, lower, b, 100);
        scene.overlay().showText(95).pointAt(Vec3.atCenterOf(b)).text("Right-click the SAME endpoint B again. Both A1 -> B and A2 -> B are needed.");
        scene.idle(100);
        scene.addKeyframe();
        scene.rotateCameraY(90);
        scene.idle(55);
        // At 225 degrees this is the view ray: reverse depth without changing projection.
        // Bring B forward instead of moving A behind its own pillar. Only the tutorial section moves.
        scene.world().moveSection(coveringSection, new Vec3(6, 6, -6), 0);
        scene.overlay().showText(110).pointAt(Vec3.atCenterOf(b)).text("The beam now covers both upper pillar blocks, completing the impossible triangle without changing its real structure.");
        scene.idle(115);
        scene.addKeyframe();
        scene.overlay().showText(110).pointAt(Vec3.atCenterOf(b)).text("Hold the editor to inspect A-to-B arrows. Sneak-right-click a linked block to delete its rules. Only valid projected top seams can be linked.");
        scene.idle(115);
        scene.markAsFinished();
    }

    private static void editorClick(SceneBuilder scene, Vec3 position) {
        scene.overlay().showControls(position, net.createmod.catnip.math.Pointing.DOWN, 70)
                .rightClick().withItem(new net.minecraft.world.item.ItemStack(ModItems.OCCLUSION_TOOL.get()));
    }

    private static void editorArrow(SceneBuilder scene, net.minecraft.core.BlockPos a,
                                    net.minecraft.core.BlockPos b, int duration) {
        var from = Vec3.atCenterOf(a);
        var tip = Vec3.atCenterOf(b);
        var direction = tip.subtract(from).normalize();
        var side = direction.cross(new Vec3(0, 1, 0)).normalize().scale(.3);
        scene.overlay().showLine(PonderPalette.GREEN, from, tip, duration);
        scene.overlay().showLine(PonderPalette.GREEN, tip, tip.subtract(direction.scale(.6)).add(side), duration);
        scene.overlay().showLine(PonderPalette.GREEN, tip, tip.subtract(direction.scale(.6)).subtract(side), duration);
    }

    /** Draw only these blocks, retaining the full schematic as neighbor-culling context.
     * Ponder normally masks neighbors outside an independent section; that exposes B's
     * internal beam face and incorrectly covers the neighboring beam top after depth shifting.
     */
    private record RenderSelection(net.createmod.ponder.api.scene.Selection blocks,
                                   net.createmod.ponder.api.scene.Selection context)
            implements net.createmod.ponder.api.scene.Selection {
        @Override public java.util.Iterator<net.minecraft.core.BlockPos> iterator() { return blocks.iterator(); }
        @Override public boolean test(net.minecraft.core.BlockPos pos) { return context.test(pos); }
        @Override public net.createmod.ponder.api.scene.Selection copy() {
            return new RenderSelection(blocks.copy(), context.copy());
        }
        @Override public net.createmod.ponder.api.scene.Selection add(net.createmod.ponder.api.scene.Selection other) {
            return new RenderSelection(blocks.add(other), context);
        }
        @Override public net.createmod.ponder.api.scene.Selection substract(net.createmod.ponder.api.scene.Selection other) {
            return new RenderSelection(blocks.substract(other), context);
        }
        @Override public Vec3 getCenter() { return blocks.getCenter(); }
        @Override public net.createmod.catnip.outliner.Outline.OutlineParams makeOutline(
                net.createmod.catnip.outliner.Outliner outliner, Object key) {
            return blocks.makeOutline(outliner, key);
        }
    }

    private static void storyboard(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title(SCENE, "Crossing the Penrose triangle");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(.8f);
        scene.addInstruction(ponder -> {
            ponder.getTransform().xRotation.startWithValue(-CameraController.PITCH);
            ponder.getTransform().yRotation.startWithValue(135);
        });
        scene.world().showSection(util.select().everywhere(), Direction.DOWN);
        var start = new Vec3(1.5, 2, 7.5);
        var target = new Vec3(5.5, 5, 4.5);
        var marker = scene.special().createBirb(start, ParrotPose.FacePointOfInterestPose::new);
        scene.special().movePointOfInterest(target);
        scene.idle(15);
        scene.overlay().showText(75).pointAt(start).text("These two platforms are separated in the world.");
        scene.idle(80);

        scene.addKeyframe();
        // Ponder flips the world Y axis before rotating the scene.
        // At 225 degrees, the (3, 3, -3) crossing offset projects to one point.
        scene.rotateCameraY(90);
        scene.idle(55);
        scene.overlay().showOutline(PonderPalette.GREEN, "start", util.select().position(1, 1, 7), 90);
        scene.overlay().showOutline(PonderPalette.GREEN, "target", util.select().position(5, 4, 4), 90);
        scene.overlay().showText(90).pointAt(target)
                .text("From this angle their top edges meet. Turn on isometric view and illusion connections.");
        scene.idle(95);

        scene.addKeyframe();
        scene.overlay().showText(105).pointAt(start.add(.5, 0, 0))
                .text("Walk across the shared edge. In multiplayer, the server also needs Isometric Projection.");
        scene.special().moveParrot(marker, new Vec3(.5, 0, 0), 18);
        scene.idle(18);
        scene.special().moveParrot(marker, new Vec3(3, 3, -3), 1);
        scene.idle(1);
        scene.special().moveParrot(marker, new Vec3(.5, 0, 0), 18);
        scene.idle(75);
        scene.markAsFinished();
    }
}
