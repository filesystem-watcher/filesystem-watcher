package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.FilesystemUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.github.filesystemwatcher.FilesystemConstraints.FileType.REGULAR;
import static io.github.filesystemwatcher.FilesystemEventType.CREATED;

class ResilienceTest {

    private static final Duration WAIT_TIME = Duration.ofSeconds(30);

    private FilesystemNotifier monitor;

    @AfterEach
    void stopMonitor() {
        if (monitor != null) {
            monitor.stopWatching();
        }
    }

    @Test
    void shouldKeepWatchingDirectoryWhenFileDisappearsBeforeItsEventIsProcessed(@TempDir Path temporaryDirectory) throws InterruptedException {
        //given
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        startMonitor(temporaryDirectory, FilesystemConstraints.DEFAULT.withFileTypes(List.of(REGULAR)), receivedEvents::add);

        //when
        List<Path> transientFiles = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            transientFiles.add(FilesystemUtils.createFile(temporaryDirectory, "transient" + i + ".txt"));
        }
        transientFiles.forEach(FilesystemUtils::delete);
        Thread.sleep(500);
        Path sentinel = FilesystemUtils.createFile(temporaryDirectory, "sentinel.txt");

        //then
        awaitEvent(receivedEvents, FilesystemEvent.of(sentinel, CREATED));
    }

    @Test
    void shouldKeepDeliveringEventsWhenConsumerThrows(@TempDir Path temporaryDirectory) throws InterruptedException {
        //given
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean failed = new AtomicBoolean();
        startMonitor(temporaryDirectory, FilesystemConstraints.DEFAULT, event -> {
            if (failed.compareAndSet(false, true)) {
                throw new IllegalStateException("Consumer failure");
            }
            receivedEvents.add(event);
        });

        //when
        FilesystemUtils.createFile(temporaryDirectory, "first.txt");
        Awaitility.await().atMost(WAIT_TIME).untilTrue(failed);
        Path second = FilesystemUtils.createFile(temporaryDirectory, "second.txt");

        //then
        awaitEvent(receivedEvents, FilesystemEvent.of(second, CREATED));
    }

    private void startMonitor(Path path, FilesystemConstraints constraints, Consumer<FilesystemEvent> consumer) throws InterruptedException {
        monitor = FilesystemMonitor.builder()
                .watchedPath(path)
                .watchedConstraints(constraints)
                .watchedConsumer(consumer)
                .build();
        monitor.startWatching();
        Thread.sleep(100);
    }

    private static void awaitEvent(List<FilesystemEvent> receivedEvents, FilesystemEvent expectedEvent) {
        Awaitility.await()
                .atMost(WAIT_TIME)
                .until(() -> receivedEvents.contains(expectedEvent));
    }
}
