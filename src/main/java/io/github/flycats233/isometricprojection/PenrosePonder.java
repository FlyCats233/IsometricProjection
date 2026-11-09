package io.github.flycats233.isometricprojection;

import java.util.List;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

final class PenrosePonder extends PonderUI {
    private static final String MOD_ID = "isometricprojection";
    private static final String SCENE = "penrose_crossing";
    private static final ResourceLocation SUBJECT = ResourceLocation.withDefaultNamespace("quartz_block");
    private static final StoryBoardEntry ENTRY = new PonderStoryBoardEntry(PenrosePonder::storyboard, MOD_ID, SCENE, SUBJECT);

    private PenrosePonder() {
        super(PonderIndex.getSceneAccess().compile(List.of(ENTRY)));
    }

    static void register() {
        PonderIndex.addPlugin(new PonderPlugin() {
            @Override
            public String getModId() {
                return MOD_ID;
            }

            @Override
            public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
                helper.addStoryBoard(SUBJECT, SCENE, PenrosePonder::storyboard);
            }
        });
    }

    static void open() {
        ScreenOpener.open(null, new PenrosePonder());
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
