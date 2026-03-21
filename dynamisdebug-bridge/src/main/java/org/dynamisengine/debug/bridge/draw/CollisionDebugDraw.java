package org.dynamisengine.debug.bridge.draw;

import org.dynamisengine.collision.bounds.Aabb;
import org.dynamisengine.collision.contact.ContactPoint3D;
import org.dynamisengine.collision.debug.CollisionDebugSnapshot3D;
import org.dynamisengine.collision.events.CollisionEventType;
import org.dynamisengine.collision.narrowphase.CollisionManifold3D;
import org.dynamisengine.debug.api.draw.DebugBoxCommand;
import org.dynamisengine.debug.api.draw.DebugLineCommand;
import org.dynamisengine.debug.api.draw.DepthMode;
import org.dynamisengine.debug.draw.DebugDrawQueue;

/**
 * Submits collision debug draw commands from a {@link CollisionDebugSnapshot3D}.
 *
 * <p>Draws:
 * <ul>
 *   <li>AABB bounds for each collider (cyan)</li>
 *   <li>Contact points as small crosses (color by event type: green=ENTER, yellow=STAY, red=EXIT)</li>
 *   <li>Contact normals as lines from contact point (white)</li>
 * </ul>
 */
public final class CollisionDebugDraw {

    private CollisionDebugDraw() {}

    /**
     * Submit collision debug draw commands to the queue.
     *
     * @param snapshot collision debug snapshot for this frame
     * @param queue    the draw queue to submit to
     */
    public static <T> void draw(CollisionDebugSnapshot3D<T> snapshot, DebugDrawQueue queue) {
        // Draw collider AABBs
        for (var item : snapshot.items()) {
            Aabb aabb = item.bounds();
            float cx = (float) ((aabb.minX() + aabb.maxX()) * 0.5);
            float cy = (float) ((aabb.minY() + aabb.maxY()) * 0.5);
            float cz = (float) ((aabb.minZ() + aabb.maxZ()) * 0.5);
            float hx = (float) ((aabb.maxX() - aabb.minX()) * 0.5);
            float hy = (float) ((aabb.maxY() - aabb.minY()) * 0.5);
            float hz = (float) ((aabb.maxZ() - aabb.minZ()) * 0.5);
            queue.submit(DebugBoxCommand.of(cx, cy, cz, hx, hy, hz, 0, 1, 1)); // cyan
        }

        // Draw contacts
        for (var contact : snapshot.contacts()) {
            ContactPoint3D pt = contact.point();
            float px = (float) pt.x();
            float py = (float) pt.y();
            float pz = (float) pt.z();

            // Color by event type
            float r, g, b;
            switch (contact.type()) {
                case ENTER -> { r = 0; g = 1; b = 0; }   // green
                case STAY  -> { r = 1; g = 1; b = 0; }   // yellow
                case EXIT  -> { r = 1; g = 0; b = 0; }   // red
                default    -> { r = 1; g = 1; b = 1; }
            }

            // Cross at contact point
            float s = 0.05f;
            queue.submit(DebugLineCommand.of(px - s, py, pz, px + s, py, pz, r, g, b, DepthMode.ALWAYS_VISIBLE));
            queue.submit(DebugLineCommand.of(px, py - s, pz, px, py + s, pz, r, g, b, DepthMode.ALWAYS_VISIBLE));
            queue.submit(DebugLineCommand.of(px, py, pz - s, px, py, pz + s, r, g, b, DepthMode.ALWAYS_VISIBLE));

            // Normal line
            CollisionManifold3D m = contact.manifold();
            float nl = 0.2f;
            queue.submit(DebugLineCommand.of(
                    px, py, pz,
                    px + (float) m.normalX() * nl,
                    py + (float) m.normalY() * nl,
                    pz + (float) m.normalZ() * nl,
                    1, 1, 1, DepthMode.ALWAYS_VISIBLE)); // white normal
        }
    }
}
