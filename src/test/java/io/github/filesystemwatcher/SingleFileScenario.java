package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.FilesystemUtils;
import io.github.filesystemwatcher.utilities.WatchCoordinator;
import io.github.filesystemwatcher.utilities.WatchImplementation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static io.github.filesystemwatcher.FilesystemEventType.*;


@RequiredArgsConstructor
enum SingleFileScenario implements Scenario {

    FILE_CREATE(SingleFileScenario::createFile, WatchImplementation.all()),
    FILE_DELETE(SingleFileScenario::deleteFile, WatchImplementation.all()),
    FILE_MODIFY_CONTENT(SingleFileScenario::modifyFileContent, List.of(WatchImplementation.NATIVE)),
    FILE_ADD_POSIX_PERMISSIONS(SingleFileScenario::addPermissions, List.of(WatchImplementation.NATIVE)),
    FILE_REMOVE_POSIX_PERMISSIONS(SingleFileScenario::removePermissions, List.of(WatchImplementation.NATIVE)),
    FILE_SET_SAME_PERMISSIONS(SingleFileScenario::setSamePermissions, List.of(WatchImplementation.NATIVE));

    private final Scenario scenario;
    @Getter
    private final List<WatchImplementation> implementations;

    @Override
    public List<FilesystemEvent> apply(Path path, WatchCoordinator watchCoordinator) {
        return scenario.apply(path, watchCoordinator);
    }

    private static List<FilesystemEvent> createFile(Path base, WatchCoordinator coordinator) {
        coordinator.setupCompleted();

        Path createdFile = FilesystemUtils.createFile(base, "test.txt");
        return List.of(FilesystemEvent.of(createdFile, CREATED));
    }

    private static List<FilesystemEvent> deleteFile(Path base, WatchCoordinator coordinator) {
        Path createdFile = FilesystemUtils.createFile(base, "test.txt");

        coordinator.setupCompleted();

        FilesystemUtils.delete(createdFile);
        return List.of(
                FilesystemEvent.of(createdFile, INITIAL),
                FilesystemEvent.of(createdFile, DELETED)
        );
    }

    private static List<FilesystemEvent> modifyFileContent(Path base, WatchCoordinator coordinator) {
        Path createdFile = FilesystemUtils.createFile(base, "test.txt");
        coordinator.setupCompleted();

        FilesystemUtils.writeFile(createdFile, "Hello");

        return List.of(
                FilesystemEvent.of(createdFile, INITIAL),
                FilesystemEvent.of(createdFile, MODIFIED)
        );
    }

    private static List<FilesystemEvent> addPermissions(Path base, WatchCoordinator coordinator) {
        Path createdFile = FilesystemUtils.createFile(base, "test.txt");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(createdFile);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        FilesystemUtils.setPosixFilePermissions(createdFile, permissions);
        return List.of(
                FilesystemEvent.of(createdFile, INITIAL),
                FilesystemEvent.of(createdFile, MODIFIED)
        );
    }

    private static List<FilesystemEvent> removePermissions(Path base, WatchCoordinator coordinator) {
        Path createdFile = FilesystemUtils.createFile(base, "test.txt");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(createdFile);
        PosixFilePermission toRemove = permissions.stream()
                .filter(permission -> !permission.toString().startsWith("OWNER"))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", createdFile)));
        permissions.remove(toRemove);
        FilesystemUtils.setPosixFilePermissions(createdFile, permissions);
        return List.of(
                FilesystemEvent.of(createdFile, INITIAL),
                FilesystemEvent.of(createdFile, MODIFIED)
        );
    }

    private static List<FilesystemEvent> setSamePermissions(Path base, WatchCoordinator coordinator) {
        Path createdFile = FilesystemUtils.createFile(base, "test.txt");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(createdFile);
        FilesystemUtils.setPosixFilePermissions(createdFile, permissions);

        return List.of(
                FilesystemEvent.of(createdFile, INITIAL),
                FilesystemEvent.of(createdFile, MODIFIED)
        );
    }

}
