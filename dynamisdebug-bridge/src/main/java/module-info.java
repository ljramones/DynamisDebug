module org.dynamisengine.debug.bridge {
    requires org.dynamisengine.debug.api;
    requires org.dynamisengine.debug.core;
    requires org.dynamisengine.core;
    requires dynamis.event;

    // Layer 2 subsystem telemetry sources
    requires org.dynamisengine.collision;
    requires dynamis.gpu.api;
    requires org.dynamisengine.animis.runtime;
    requires org.dynamisengine.animis;

    // Layer 3 subsystem telemetry sources
    requires org.dynamisengine.ecs.api;
    requires org.dynamisengine.scenegraph.core;
    requires org.dynamisengine.content.api;

    // Layer 4 subsystem telemetry sources
    requires org.dynamisengine.audio.dsp;
    requires org.dynamisengine.light.api;
    requires org.dynamisengine.terrain.api;
    requires org.dynamisengine.vfx.api;
    requires org.dynamisengine.sky.api;

    // Layer 5 subsystem telemetry sources
    requires org.dynamisengine.physics.api;
    requires org.dynamisengine.ai.core;
    requires org.dynamisengine.scripting.runtime;

    exports org.dynamisengine.debug.bridge;
    exports org.dynamisengine.debug.bridge.event;
    exports org.dynamisengine.debug.bridge.adapter;
}
