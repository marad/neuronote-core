package infra

import io.github.marad.neuronote.core.TimeProvider

import java.time.Instant

class MockTimeProvider implements TimeProvider {
    Instant mockedTime

    MockTimeProvider(Instant mockedTime) {
        this.mockedTime = mockedTime
    }

    @Override
    Instant now() {
        return mockedTime
    }
}
