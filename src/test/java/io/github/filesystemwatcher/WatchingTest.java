package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.AwaitilityUtils;
import io.github.filesystemwatcher.utilities.WatchCoordinator;
import io.github.filesystemwatcher.utilities.WatchImplementation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.filesystemwatcher.FilesystemEventType.MODIFIED;

class WatchingTest {

    @ParameterizedTest
    @MethodSource("scenarios")
    void shouldWatchCorrectlyChangesInAllScenarios(Scenario scenario, @TempDir Path temporaryDirectory) throws InterruptedException, ExecutionException {
        //when
        WatchCoordinator coordinator = new WatchCoordinator();
        Future<List<FilesystemEvent>> futureEvents = Executors.newSingleThreadExecutor().submit(() -> scenario.apply(temporaryDirectory, coordinator));
        coordinator.awaitSetup();

        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        FilesystemNotifier monitor = FilesystemMonitor.builder()
                .watchedPath(temporaryDirectory)
                .watchedConstraints(FilesystemConstraints.DEFAULT.withRecursive(true))
                .watchedConsumer(receivedEvents::add)
                .build();
        //when
        monitor.startWatching();
        coordinator.watcherCompleted();

        //then
        List<FilesystemEvent> filesystemEvents = futureEvents.get();
        AwaitilityUtils.awaitForSize(receivedEvents, filesystemEvents.size());
        Assertions.assertThat(withoutRepeatedModifications(receivedEvents)).containsExactlyInAnyOrderElementsOf(filesystemEvents);
    }

    // One write can be reported as one or more modifications, depending on timing.
    private static List<FilesystemEvent> withoutRepeatedModifications(List<FilesystemEvent> events) {
        Set<FilesystemEvent> modifications = new HashSet<>();
        synchronized (events) {
            return events.stream()
                    .filter(event -> event.getEventType() != MODIFIED || modifications.add(event))
                    .collect(Collectors.toList());
        }
    }

    static Stream<Arguments> scenarios() {
        ArrayList<Scenario> scenarios = new ArrayList<>();
        scenarios.addAll(Arrays.asList(SingleFileScenario.values()));
        scenarios.addAll(Arrays.asList(SingleDirectoryScenario.values()));
        scenarios.addAll(Arrays.asList(SymlinkFileScenario.values()));
        scenarios.addAll(Arrays.asList(SymlinkDirectoryScenario.values()));
        scenarios.addAll(Arrays.asList(RecursiveFileScenario.values()));
        scenarios.addAll(Arrays.asList(RecursiveDirectoryScenario.values()));

        WatchImplementation currentImplementation = WatchImplementation.determineImplementation();
        return scenarios.stream()
                .filter(scenario -> scenario.getImplementations().contains(currentImplementation))
                .map(Arguments::of);
    }
}
