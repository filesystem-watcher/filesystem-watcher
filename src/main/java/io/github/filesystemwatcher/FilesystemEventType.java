package io.github.filesystemwatcher;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum FilesystemEventType {
    INITIAL,
    CREATED,
    DELETED,
    MODIFIED;

    private static final Map<WatchEvent.Kind, FilesystemEventType> CORRESPONDING_WATCH_KINDS = correspondingWatchKinds();

    private static Map<WatchEvent.Kind, FilesystemEventType> correspondingWatchKinds() {
        Map<WatchEvent.Kind, FilesystemEventType> kinds = new HashMap<>();
        kinds.put(StandardWatchEventKinds.ENTRY_CREATE, FilesystemEventType.CREATED);
        kinds.put(StandardWatchEventKinds.ENTRY_DELETE, FilesystemEventType.DELETED);
        kinds.put(StandardWatchEventKinds.ENTRY_MODIFY, FilesystemEventType.MODIFIED);
        return Collections.unmodifiableMap(kinds);
    }

    static FilesystemEventType of(WatchEvent.Kind kind) {
        return CORRESPONDING_WATCH_KINDS.get(kind);
    }
}
