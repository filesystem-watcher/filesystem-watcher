package io.github.filesystemwatcher;

import io.github.filesystemwatcher.threads.FilesystemMonitorThreadFactory;
import io.github.filesystemwatcher.threads.LoggingExecutorService;
import lombok.Builder;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
@Builder
public class FilesystemMonitor implements FilesystemNotifier {

    private final Path watchedPath;
    private final Consumer<FilesystemEvent> watchedConsumer;
    private final FilesystemConstraints watchedConstraints;

    private final BlockingQueue<FilesystemEvent> queue = new ArrayBlockingQueue<>(100000);
    private final ExecutorService producersExecutor = new LoggingExecutorService(Executors.newSingleThreadExecutor(
            new FilesystemMonitorThreadFactory(getClass().getSimpleName() + "Producers")
    ));
    private final ExecutorService consumersExecutor = new LoggingExecutorService(Executors.newSingleThreadExecutor(
            new FilesystemMonitorThreadFactory(getClass().getSimpleName() + "Consumers")
    ));

    @NonFinal
    private FilesystemWatcher watcher;
    @NonFinal
    private FilesystemNotifier reader;

    @Override
    public void startWatching() {

        watcher = FilesystemWatcher.builder()
                .watchedPath(watchedPath)
                .watchedConstraints(watchedConstraints)
                .watchedConsumer(watchedConsumer)
                .producersExecutor(producersExecutor)
                .consumersExecutor(consumersExecutor)
                .blockingQueue(queue)
                .build();

        reader = FilesystemReader.builder()
                .watchedPath(watchedPath)
                .watchedConstraints(watchedConstraints.withoutFilters())
                .watchedConsumer(this::consumeEvent)
                .build();

        watcher.startWatching();
        reader.startWatching();
    }

    @Override
    public void stopWatching() {
        reader.stopWatching();
        watcher.stopWatching();
    }

    private void consumeEvent(FilesystemEvent event) {
        Path path = event.getPath();
        if (watchedConstraints.isRecursive() && Files.isDirectory(path)) {
            try {
                watcher.startWatching(path);
            } catch (RuntimeException e) {
                log.error("Failed to watch directory: {}", path, e);
            }
        }

        try {
            queue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
