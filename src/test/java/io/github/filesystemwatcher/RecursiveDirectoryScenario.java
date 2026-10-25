package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.FilesystemUtils;
import io.github.filesystemwatcher.utilities.WatchCoordinator;
import io.github.filesystemwatcher.utilities.WatchImplementation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.github.filesystemwatcher.FilesystemEventType.*;

@RequiredArgsConstructor
enum RecursiveDirectoryScenario implements Scenario {

    RECURSIVE_DIRECTORY_CREATE(RecursiveDirectoryScenario::create, WatchImplementation.all()),
    RECURSIVE_DIRECTORY_DELETE(RecursiveDirectoryScenario::delete, WatchImplementation.all()),
    RECURSIVE_DIRECTORY_RENAME(RecursiveDirectoryScenario::rename, WatchImplementation.all()),
    RECURSIVE_DIRECTORY_ADD_POSIX_PERMISSIONS(RecursiveDirectoryScenario::addPermissions, WatchImplementation.nativeWithPosixPermissions()),
    RECURSIVE_DIRECTORY_REMOVE_POSIX_PERMISSIONS(RecursiveDirectoryScenario::removePermissions, WatchImplementation.nativeWithPosixPermissions()),
    RECURSIVE_DIRECTORY_SET_SAME_PERMISSIONS(RecursiveDirectoryScenario::setSamePermissions, WatchImplementation.nativeWithPosixPermissions());

    private final Scenario scenario;
    @Getter
    private final List<WatchImplementation> implementations;

    @Override
    public List<FilesystemEvent> apply(Path path, WatchCoordinator watchCoordinator) {
        return scenario.apply(path, watchCoordinator);
    }

    private static List<FilesystemEvent> create(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");

        coordinator.setupCompleted();

        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");


        List<FilesystemEvent> initial = new ArrayList<>(Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL)));

        initial.add(FilesystemEvent.of(recursiveDirectory, CREATED));
        if (WatchImplementation.determineImplementation() == WatchImplementation.POOLING) {
            initial.add(FilesystemEvent.of(third, MODIFIED));
        }
        return initial;
    }

    private static List<FilesystemEvent> delete(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");

        coordinator.setupCompleted();

        FilesystemUtils.delete(recursiveDirectory);

        List<FilesystemEvent> initial = new ArrayList<>(Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL)));

        initial.add(FilesystemEvent.of(recursiveDirectory, DELETED));
        if (WatchImplementation.determineImplementation() == WatchImplementation.POOLING) {
            initial.add(FilesystemEvent.of(third, MODIFIED));
        }
        return initial;
    }

    //TODO move not empty dir?
    //TODO copy?
    private static List<FilesystemEvent> rename(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");

        coordinator.setupCompleted();

        Path moved = FilesystemUtils.move(recursiveDirectory, third.resolve("moved"));

        List<FilesystemEvent> initial = new ArrayList<>(Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL)));

        initial.add(FilesystemEvent.of(moved, CREATED));
        initial.add(FilesystemEvent.of(recursiveDirectory, DELETED));
        if (WatchImplementation.determineImplementation() == WatchImplementation.POOLING) {
            initial.add(FilesystemEvent.of(third, MODIFIED));
        }
        return initial;
    }

    private static List<FilesystemEvent> addPermissions(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(recursiveDirectory);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        FilesystemUtils.setPosixFilePermissions(recursiveDirectory, permissions);
        return Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL),
                FilesystemEvent.of(recursiveDirectory, MODIFIED)
        );
    }

    private static List<FilesystemEvent> removePermissions(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(recursiveDirectory);
        PosixFilePermission toRemove = permissions.stream()
                .filter(permission -> !permission.toString().startsWith("OWNER"))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", recursiveDirectory)));
        permissions.remove(toRemove);
        FilesystemUtils.setPosixFilePermissions(recursiveDirectory, permissions);
        return Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL),
                FilesystemEvent.of(recursiveDirectory, MODIFIED)
        );
    }

    private static List<FilesystemEvent> setSamePermissions(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path recursiveDirectory = FilesystemUtils.createFile(third, "recursive");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(recursiveDirectory);
        FilesystemUtils.setPosixFilePermissions(recursiveDirectory, permissions);

        return Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(recursiveDirectory, INITIAL),
                FilesystemEvent.of(recursiveDirectory, MODIFIED)
        );
    }
}
