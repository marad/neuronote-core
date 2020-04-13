package io.github.marad.neuronote.infra

import io.github.marad.neuronote.core.IdGenerator

class InMemoryIdGenerator : IdGenerator {
    private var nextId = 0
    override fun nextId(): Int = nextId++

}