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
enum SymlinkFileScenario implements Scenario {

    SYMLINK_FILE_CREATE(SymlinkFileScenario::create, WatchImplementation.all()),
    SYMLINK_FILE_DELETE(SymlinkFileScenario::delete, WatchImplementation.all()),
    SYMLINK_FILE_MODIFY_CONTENT(SymlinkFileScenario::modifyContent, WatchImplementation.all()),
    SYMLINK_FILE_ADD_POSIX_PERMISSIONS(SymlinkFileScenario::addPermissions, List.of(WatchImplementation.NATIVE)),
    SYMLINK_FILE_REMOVE_POSIX_PERMISSIONS(SymlinkFileScenario::removePermissions, List.of(WatchImplementation.NATIVE)),
    SYMLINK_FILE_SET_SAME_PERMISSIONS(SymlinkFileScenario::setSamePermissions, List.of(WatchImplementation.NATIVE));

    private final Scenario scenario;
    @Getter
    private final List<WatchImplementation> implementations;

    @Override
    public List<FilesystemEvent> apply(Path path, WatchCoordinator watchCoordinator) {
        return scenario.apply(path, watchCoordinator);
    }

    private static List<FilesystemEvent> create(Path base, WatchCoordinator coordinator) {
        Path futureFile = base.resolve("test.txt");
        Path symlink = FilesystemUtils.createLink(futureFile, "test_symlink", base);
        coordinator.setupCompleted();

        Path created = FilesystemUtils.createFile(base, "test.txt");
        return List.of(
                FilesystemEvent.of(symlink, INITIAL),
                FilesystemEvent.of(created, CREATED)
        );
    }

    private static List<FilesystemEvent> delete(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createFile(base, "test.txt");
        Path symlink = FilesystemUtils.createLink(base, "test_symlink", base);
        coordinator.setupCompleted();

        FilesystemUtils.delete(created);
        return List.of(
                FilesystemEvent.of(symlink, INITIAL),
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, DELETED)
        );
    }

    private static List<FilesystemEvent> modifyContent(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createFile(base, "test.txt");
        Path symlink = FilesystemUtils.createLink(created, "test_symlink", base);
        coordinator.setupCompleted();

        FilesystemUtils.writeFile(symlink, "Hello");

        return List.of(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(symlink, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }

    private static List<FilesystemEvent> addPermissions(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createFile(base, "test.txt");
        Path symlink = FilesystemUtils.createLink(created, "test_symlink", base);
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(created);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        FilesystemUtils.setPosixFilePermissions(created, permissions);
        return List.of(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(symlink, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }

    private static List<FilesystemEvent> removePermissions(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createFile(base, "test.txt");
        Path symlink = FilesystemUtils.createLink(created, "test_symlink", base);
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(created);
        PosixFilePermission toRemove = permissions.stream()
                .filter(permission -> !permission.toString().startsWith("OWNER"))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", created)));
        permissions.remove(toRemove);
        FilesystemUtils.setPosixFilePermissions(created, permissions);
        return List.of(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(symlink, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }

    private static List<FilesystemEvent> setSamePermissions(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createFile(base, "test.txt");
        Path symlink = FilesystemUtils.createLink(created, "test_symlink", base);
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(created);
        FilesystemUtils.setPosixFilePermissions(created, permissions);

        return List.of(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(symlink, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }
}
