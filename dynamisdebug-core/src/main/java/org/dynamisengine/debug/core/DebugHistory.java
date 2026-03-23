package org.dynamisengine.debug.core;

import org.dynamisengine.debug.api.DebugSnapshot;

import java.util.*;

/**
 * Ring buffer of recent frame snapshots for time-series inspection.
 */
public final class DebugHistory {

    private final int maxFrames;
    private final ArrayDeque<FrameRecord> frames;

    public DebugHistory(int maxFrames) {
        this.maxFrames = maxFrames;
        this.frames = new ArrayDeque<>(maxFrames);
    }

    public void record(long frameNumber, Map<String, DebugSnapshot> snapshots) {
        if (frames.size() >= maxFrames) frames.removeFirst();
        frames.addLast(new FrameRecord(frameNumber, Map.copyOf(snapshots)));
    }

    public List<FrameRecord> recent(int count) {
        List<FrameRecord> result = new ArrayList<>(Math.min(count, frames.size()));
        var it = frames.descendingIterator();
        while (it.hasNext() && result.size() < count) result.add(it.next());
        Collections.reverse(result);
        return result;
    }

    public Optional<FrameRecord> latest() {
        return frames.isEmpty() ? Optional.empty() : Optional.of(frames.getLast());
    }

    public int size() { return frames.size(); }

    /** Find a frame by number. Returns empty if not retained. */
    public Optional<FrameRecord> frame(long frameNumber) {
        for (var f : frames) {
            if (f.frameNumber() == frameNumber) return Optional.of(f);
        }
        return Optional.empty();
    }

    /** Oldest retained frame, or empty if history is empty. */
    public Optional<FrameRecord> oldest() {
        return frames.isEmpty() ? Optional.empty() : Optional.of(frames.getFirst());
    }

    /** Newest retained frame number, or -1 if empty. */
    public long newestFrameNumber() {
        return frames.isEmpty() ? -1 : frames.getLast().frameNumber();
    }

    /** Oldest retained frame number, or -1 if empty. */
    public long oldestFrameNumber() {
        return frames.isEmpty() ? -1 : frames.getFirst().frameNumber();
    }

    public record FrameRecord(long frameNumber, Map<String, DebugSnapshot> snapshots) {}
}
