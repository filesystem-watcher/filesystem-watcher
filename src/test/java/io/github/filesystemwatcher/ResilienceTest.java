package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.AwaitilityUtils;
import io.github.filesystemwatcher.utilities.FilesystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.filesystemwatcher.FilesystemConstraints.FileType.REGULAR;
import static io.github.filesystemwatcher.FilesystemEventType.CREATED;

class ResilienceTest extends AbstractMonitorTest {

    @Test
    void shouldKeepWatchingDirectoryWhenFileDisappearsBeforeItsEventIsProcessed(@TempDir Path temporaryDirectory) {
        //given
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        startMonitor(temporaryDirectory, FilesystemConstraints.DEFAULT.withFileTypes(Arrays.asList(REGULAR)), receivedEvents::add);

        //when
        List<Path> transientFiles = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            transientFiles.add(FilesystemUtils.createFile(temporaryDirectory, "transient" + i + ".txt"));
        }
        transientFiles.forEach(FilesystemUtils::delete);
        Path sentinel = FilesystemUtils.createFile(temporaryDirectory, "sentinel.txt");

        //then
        AwaitilityUtils.awaitForElement(receivedEvents, FilesystemEvent.of(sentinel, CREATED));
    }

    @Test
    void shouldKeepDeliveringEventsWhenConsumerThrows(@TempDir Path temporaryDirectory) {
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
        Path second = FilesystemUtils.createFile(temporaryDirectory, "second.txt");

        //then
        AwaitilityUtils.awaitForElement(receivedEvents, FilesystemEvent.of(second, CREATED));
    }
}
