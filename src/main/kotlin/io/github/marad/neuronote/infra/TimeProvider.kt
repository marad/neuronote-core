package io.github.marad.neuronote.infra

import io.github.marad.neuronote.core.TimeProvider
import java.time.Clock
import java.time.Instant

class DefaultUtcTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now(Clock.systemUTC())
}