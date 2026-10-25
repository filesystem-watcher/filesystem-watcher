package io.github.filesystemwatcher;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SensitivityTest {

    @Test
    void shouldFindHighSensitivityModifierOnCurrentJdk() {
        Assertions.assertThat(FilesystemWatcher.sensitivityModifiers())
                .extracting(Object::toString)
                .containsExactly("HIGH");
    }
}
