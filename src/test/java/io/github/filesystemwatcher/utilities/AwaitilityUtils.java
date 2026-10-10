package io.github.filesystemwatcher.utilities;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.TimeoutEvent;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Slf4j
@UtilityClass
public class AwaitilityUtils {

    private static final Duration DEFAULT_WAIT_TIME = findWaitingDuration();

    public static <T> void awaitForSize(Collection<T> collection, int expectedSize) {
        await(() -> String.format("size=%d for collection=%s", expectedSize, collection), () -> collection.size() >= expectedSize);
    }

    public static <T> void awaitForElement(Collection<T> collection, T expectedElement) {
        await(() -> String.format("element=%s in collection=%s", expectedElement, collection), () -> collection.contains(expectedElement));
    }

    private static void await(Supplier<String> description, Callable<Boolean> condition) {
        ConditionEvaluationListener<Boolean> listener = new ConditionEvaluationListener<>() {
            @Override
            public void conditionEvaluated(EvaluatedCondition condition) {

            }

            @Override
            public void onTimeout(TimeoutEvent timeoutEvent) {
                log.error("Timeout while waiting for {}", description.get());
            }
        };

        Awaitility.await()
                .atMost(DEFAULT_WAIT_TIME)
                .conditionEvaluationListener(listener)
                .until(condition);
    }

    private static Duration findWaitingDuration() {
        WatchImplementation implementation = WatchImplementation.determineImplementation();
        return implementation.getSensitivity().multipliedBy(10);
    }
}
