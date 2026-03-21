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

    exports org.dynamisengine.debug.bridge;
    exports org.dynamisengine.debug.bridge.event;
    exports org.dynamisengine.debug.bridge.adapter;
}
