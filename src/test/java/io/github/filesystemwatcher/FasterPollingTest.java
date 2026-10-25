package io.github.filesystemwatcher;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class FasterPollingTest {

    @Test
    void shouldFindHighSensitivityModifierOnCurrentJdk() {
        Assertions.assertThat(FilesystemWatcher.fasterPollingOnOlderJdks())
                .extracting(Object::toString)
                .containsExactly("HIGH");
    }
}
