module org.dynamisengine.debug.bridge {
    requires org.dynamisengine.debug.api;
    requires org.dynamisengine.debug.core;
    requires org.dynamisengine.core;
    requires dynamis.event;

    exports org.dynamisengine.debug.bridge;
    exports org.dynamisengine.debug.bridge.event;
}
