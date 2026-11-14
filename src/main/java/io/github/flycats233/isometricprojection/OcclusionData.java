package io.github.flycats233.isometricprojection;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class OcclusionData extends SavedData {
    private final List<OcclusionRule> rules = new ArrayList<>();
    private long nextId = 1;
    public static OcclusionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(OcclusionData::new, OcclusionData::load),
                "isometricprojection_occlusion");
    }
    public List<OcclusionRule> rules() { return List.copyOf(rules); }

    public boolean hasReverse(BlockPos a, BlockPos b) {
        return rules.stream().anyMatch(rule -> rule.hidden().equals(b) && rule.covering().equals(a));
    }

    public boolean add(BlockPos a, Direction faceA, BlockPos b, Direction faceB) {
        if (!OcclusionRule.hasTopSeam(a, b) || hasReverse(a, b)) return false;
        boolean replaces = rules.stream().anyMatch(rule -> rule.hidden().equals(a));
        if (!replaces && rules.size() >= 128) return false;
        // The clicked faces select blocks; rendering evaluates all their visible faces.
        rules.removeIf(rule -> rule.hidden().equals(a));
        rules.add(new OcclusionRule(nextId++, a.immutable(), faceA, b.immutable(), faceB));
        setDirty();
        return true;
    }

    public boolean remove(BlockPos pos, Direction face) {
        boolean removed = rules.removeIf(rule -> rule.hidden().equals(pos) || rule.covering().equals(pos));
        if (removed) setDirty();
        return removed;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (var rule : rules) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("id", rule.id());
            entry.putLong("a", rule.hidden().asLong());
            entry.putInt("face_a", rule.hiddenFace().get3DDataValue());
            entry.putLong("b", rule.covering().asLong());
            entry.putInt("face_b", rule.coveringFace().get3DDataValue());
            entries.add(entry);
        }
        tag.put("rules", entries);
        tag.putLong("next_id", nextId);
        return tag;
    }

    static OcclusionData load(CompoundTag tag, HolderLookup.Provider registries) {
        OcclusionData data = new OcclusionData();
        ListTag entries = tag.getList("rules", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(128, entries.size()); i++) {
            var entry = entries.getCompound(i);
            int a = entry.getInt("face_a"), b = entry.getInt("face_b");
            if (a < 0 || a > 5 || b < 0 || b > 5) continue;
            long id = entry.getLong("id");
            data.nextId = Math.max(data.nextId, id + 1);
            BlockPos hidden = BlockPos.of(entry.getLong("a"));
            BlockPos covering = BlockPos.of(entry.getLong("b"));
            if (data.hasReverse(hidden, covering)) {
                // Keep the first saved direction when upgrading legacy contradictory pairs.
                data.setDirty();
                continue;
            }
            data.rules.add(new OcclusionRule(id, hidden, Direction.from3DDataValue(a),
                    covering, Direction.from3DDataValue(b)));
        }
        data.nextId = Math.max(data.nextId, tag.getLong("next_id"));
        return data;
    }
}
