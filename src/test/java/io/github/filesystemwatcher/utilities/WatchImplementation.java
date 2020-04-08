package io.github.filesystemwatcher.utilities;

import com.sun.nio.file.SensitivityWatchEventModifier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public enum WatchImplementation {
    POOLING(Duration.ofSeconds(SensitivityWatchEventModifier.HIGH.sensitivityValueInSeconds())),
    NATIVE(Duration.ofMillis(200));

    @Getter
    final Duration sensitivity;

    public static WatchImplementation determineImplementation() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            String name = watchService.getClass().getSimpleName();
            if ("PollingWatchService".equals(name)) {
                return POOLING;
            }
            return NATIVE;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<WatchImplementation> all() {
        return Arrays.asList(values());
    }
}
