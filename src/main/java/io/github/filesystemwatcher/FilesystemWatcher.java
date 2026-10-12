package io.github.filesystemwatcher;

import com.sun.nio.file.SensitivityWatchEventModifier;
import io.vavr.control.Try;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.filesystemwatcher.FilesystemEventType.*;
import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
@Builder
public class FilesystemWatcher implements FilesystemNotifier {

    private final WatchableUtility watchableUtility = new WatchableUtility();

    private final Path watchedPath;
    private final FilesystemConstraints watchedConstraints;
    private final Consumer<FilesystemEvent> watchedConsumer;

    private final BlockingQueue<FilesystemEvent> blockingQueue;
    private final ExecutorService producersExecutor;
    private final ExecutorService consumersExecutor;

    private final Map<Path, WatchKey> watchedKeys = new ConcurrentHashMap<>();
    // Paths already reported by listing a created directory; their own CREATED events are skipped.
    private final Set<Path> listedPaths = new HashSet<>();

    @NonFinal
    private Future<?> consumer;
    @NonFinal
    private Future<?> producer;

    @Override
    public void startWatching() {
        startWatching(watchedPath);
        consumer = consumersExecutor.submit(this::consumeEvents);
        producer = producersExecutor.submit(this::produceEvents);
    }

    @Override
    public void stopWatching() {
        try {
            watchableUtility.closeWatchService();
        } finally {
            blockingQueue.clear();
            producersExecutor.shutdownNow();
            consumersExecutor.shutdownNow();
        }
    }

    void startWatching(Path path) {
        WatchKey key = watchableUtility.registerWatchable(path);
        watchedKeys.putIfAbsent(path, key);
        log.info("Watching started: {}", path);
    }

    private void stopWatching(Path path) {
        WatchKey key = watchedKeys.get(path);
        if (key != null) {
            key.cancel();
        }
        watchedKeys.remove(path);
        log.info("Watching stopped: {}", path);
    }

    private void produceEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey watchedKey;
            try {
                watchedKey = watchableUtility.getWatchService().take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ClosedWatchServiceException e) {
                return;
            }

            try {
                produceKeyEvents(watchedKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                watchedKey.reset();
            }
        }
    }

    private void produceKeyEvents(WatchKey watchedKey) throws InterruptedException {
        log.info("Watched key: {}", watchedKey.watchable());

        Path watchedDirectory = (Path) watchedKey.watchable();
        for (WatchEvent<?> watchEvent : watchedKey.pollEvents()) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            log.info("Watched event: {} {}", watchEvent.kind(), watchEvent.context());

            if (watchEvent.kind() == OVERFLOW) {
                log.error("OVERFLOW watchEvent occurred {} times. Operating system queue was overflowed. File events could be lost.", watchEvent.count());
                continue;
            }

            try {
                blockingQueue.put(FilesystemEvent.of(watchEvent, watchedDirectory));
            } catch (RuntimeException e) {
                log.error("Skipping event {} {} in {}", watchEvent.kind(), watchEvent.context(), watchedDirectory, e);
            }
        }
    }

    private void consumeEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            FilesystemEvent event;
            try {
                event = blockingQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            try {
                consumeEvent(event);
            } catch (RuntimeException e) {
                log.error("Failed to consume event: {}", event, e);
            }
        }
    }

    private void consumeEvent(FilesystemEvent event) {
        Path path = event.getPath();
        log.info("Consumed event: " + event);

        if (CREATED == event.getEventType()) {
            if (!listedPaths.remove(path)) {
                consumeCreated(path);
            }
            return;
        }
        if (DELETED == event.getEventType()) {
            listedPaths.remove(path);
            stopWatching(path);
        }
        deliver(event);
    }

    private void consumeCreated(Path path) {
        boolean watchable = watchedConstraints.isRecursive() && Files.isDirectory(path);
        if (watchable) {
            startWatching(path);
        }
        deliver(FilesystemEvent.of(path, CREATED));

        if (watchable && !Files.isSymbolicLink(path)) {
            for (Path child : listChildren(path)) {
                listedPaths.add(child);
                try {
                    consumeCreated(child);
                } catch (RuntimeException e) {
                    log.error("Failed to consume created path: {}", child, e);
                }
            }
        }
    }

    private static List<Path> listChildren(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            return children.collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void deliver(FilesystemEvent event) {
        if (watchedConstraints.test(event)) {
            watchedConsumer.accept(event);
        }
    }

    @Value
    private static class WatchableUtility {
        private static final Supplier<IllegalStateException> EXCEPTION_SUPPLIER = IllegalStateException::new;
        private static final WatchEvent.Kind[] ALL_EVENT_KINDS = new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW};

        private final WatchService watchService = createWatchService();

        private WatchService createWatchService() {
            FileSystem fileSystem = FileSystems.getDefault();
            return Try.of(fileSystem::newWatchService).getOrElseThrow(EXCEPTION_SUPPLIER);
        }

        private WatchKey registerWatchable(Watchable watchable) {
            return Try.of(() -> watchable.register(watchService, ALL_EVENT_KINDS, SensitivityWatchEventModifier.HIGH)).getOrElseThrow(EXCEPTION_SUPPLIER);
        }

        void closeWatchService() {
            Try.run(watchService::close).getOrElseThrow(EXCEPTION_SUPPLIER);
        }
    }
}
