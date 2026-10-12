package io.github.filesystemwatcher;

import org.junit.jupiter.api.AfterEach;

import java.nio.file.Path;
import java.util.function.Consumer;

abstract class AbstractMonitorTest {

    private FilesystemNotifier monitor;

    @AfterEach
    void stopMonitor() {
        if (monitor != null) {
            monitor.stopWatching();
        }
    }

    void startMonitor(Path path, FilesystemConstraints constraints, Consumer<FilesystemEvent> consumer) {
        monitor = FilesystemMonitor.builder()
                .watchedPath(path)
                .watchedConstraints(constraints)
                .watchedConsumer(consumer)
                .build();
        monitor.startWatching();
    }
}
