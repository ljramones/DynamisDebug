package org.dynamisengine.debug.bridge.draw;

import org.dynamisengine.collision.bounds.Aabb;
import org.dynamisengine.collision.contact.ContactPoint3D;
import org.dynamisengine.collision.debug.CollisionDebugSnapshot3D;
import org.dynamisengine.collision.events.CollisionEventType;
import org.dynamisengine.collision.narrowphase.CollisionManifold3D;
import org.dynamisengine.collision.pipeline.CollisionPair;
import org.dynamisengine.debug.api.draw.*;
import org.dynamisengine.debug.draw.DebugDrawQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CollisionDebugDrawTest {

    private DebugDrawQueue queue;

    @BeforeEach
    void setUp() {
        queue = new DebugDrawQueue();
    }

    @Test
    void drawsAabbForEachCollider() {
        var items = List.of(
                new CollisionDebugSnapshot3D.ItemBounds<>("a", new Aabb(0, 0, 0, 2, 2, 2)),
                new CollisionDebugSnapshot3D.ItemBounds<>("b", new Aabb(4, 4, 4, 6, 6, 6))
        );
        var snapshot = new CollisionDebugSnapshot3D<>(items, List.of());

        CollisionDebugDraw.draw(snapshot, queue);

        var cmds = queue.activeCommands();
        long boxCount = cmds.stream().filter(c -> c instanceof DebugBoxCommand).count();
        assertEquals(2, boxCount);

        // First box: center=(1,1,1) half=(1,1,1)
        DebugBoxCommand box = (DebugBoxCommand) cmds.get(0);
        assertEquals(1f, box.cx(), 0.01);
        assertEquals(1f, box.halfX(), 0.01);
        assertEquals(0f, box.r()); // cyan
        assertEquals(1f, box.g());
        assertEquals(1f, box.b());
    }

    @Test
    void drawsContactPointsWithEventTypeColors() {
        var pair = new CollisionPair<>("a", "b");
        var manifold = new CollisionManifold3D(0, 1, 0, 0.1);
        var contacts = List.of(
                new CollisionDebugSnapshot3D.Contact<>(pair, CollisionEventType.ENTER, true,
                        manifold, new ContactPoint3D(1, 2, 3)),
                new CollisionDebugSnapshot3D.Contact<>(pair, CollisionEventType.EXIT, false,
                        manifold, new ContactPoint3D(4, 5, 6))
        );
        var snapshot = new CollisionDebugSnapshot3D<>(List.<CollisionDebugSnapshot3D.ItemBounds<String>>of(), contacts);

        CollisionDebugDraw.draw(snapshot, queue);

        var cmds = queue.activeCommands();
        // Each contact: 3 cross lines + 1 normal = 4 lines per contact
        long lineCount = cmds.stream().filter(c -> c instanceof DebugLineCommand).count();
        assertEquals(8, lineCount);

        // ENTER contact should have green (0,1,0)
        DebugLineCommand firstLine = (DebugLineCommand) cmds.get(0);
        assertEquals(0f, firstLine.r());
        assertEquals(1f, firstLine.g());
        assertEquals(0f, firstLine.b());
        assertEquals(DepthMode.ALWAYS_VISIBLE, firstLine.depthMode());
    }

    @Test
    void drawsContactNormals() {
        var pair = new CollisionPair<>("a", "b");
        var manifold = new CollisionManifold3D(0, 1, 0, 0.5);
        var contacts = List.of(
                new CollisionDebugSnapshot3D.Contact<>(pair, CollisionEventType.STAY, true,
                        manifold, new ContactPoint3D(0, 0, 0))
        );
        var snapshot = new CollisionDebugSnapshot3D<>(List.<CollisionDebugSnapshot3D.ItemBounds<String>>of(), contacts);

        CollisionDebugDraw.draw(snapshot, queue);

        // Normal line should be the 4th command (after 3 cross lines)
        var cmds = queue.activeCommands();
        DebugLineCommand normal = (DebugLineCommand) cmds.get(3);
        assertEquals(0f, normal.x1());
        assertEquals(0f, normal.y1());
        assertEquals(0.2f, normal.y2(), 0.01); // normal Y * 0.2
        assertEquals(1f, normal.r()); // white
        assertEquals(1f, normal.g());
        assertEquals(1f, normal.b());
    }

    @Test
    void emptySnapshotProducesNoCommands() {
        var snapshot = new CollisionDebugSnapshot3D<>(
                List.<CollisionDebugSnapshot3D.ItemBounds<String>>of(),
                List.<CollisionDebugSnapshot3D.Contact<String>>of());
        CollisionDebugDraw.draw(snapshot, queue);
        assertEquals(0, queue.commandCount());
    }
}
