package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.AwaitilityUtils;
import io.github.filesystemwatcher.utilities.FilesystemUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.github.filesystemwatcher.FilesystemEventType.CREATED;

class CreatedDirectoryTest extends AbstractMonitorTest {

    private static final FilesystemConstraints RECURSIVE = FilesystemConstraints.DEFAULT.withRecursive(true);

    @Test
    void shouldWatchNestedDirectoriesCreatedAtOnce(@TempDir Path directory) {
        //given
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        startMonitor(directory, RECURSIVE, receivedEvents::add);

        //when
        Path nestedDirectory = FilesystemUtils.createDirectories(directory.resolve("first/second/third"));
        AwaitilityUtils.awaitForElement(receivedEvents, FilesystemEvent.of(nestedDirectory, CREATED));
        Path file = FilesystemUtils.createFile(nestedDirectory, "file.txt");

        //then
        AwaitilityUtils.awaitForElement(receivedEvents, FilesystemEvent.of(file, CREATED));
    }

    @Test
    void shouldReportFilesOfCreatedDirectoryOnce(@TempDir Path directory) {
        //given
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        startMonitor(directory, RECURSIVE, receivedEvents::add);

        //when
        Path createdDirectory = FilesystemUtils.createDirectory(directory, "created");
        List<FilesystemEvent> expectedEvents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            expectedEvents.add(FilesystemEvent.of(FilesystemUtils.createFile(createdDirectory, "file" + i + ".txt"), CREATED));
        }
        expectedEvents.forEach(event -> AwaitilityUtils.awaitForElement(receivedEvents, event));
        Path sentinel = FilesystemUtils.createFile(createdDirectory, "sentinel.txt");
        AwaitilityUtils.awaitForElement(receivedEvents, FilesystemEvent.of(sentinel, CREATED));

        //then
        Assertions.assertThat(receivedEvents)
                .filteredOn(event -> event.getEventType() == CREATED)
                .doesNotHaveDuplicates();
    }
}
