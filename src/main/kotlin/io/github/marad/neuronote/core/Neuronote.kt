package io.github.marad.neuronote.core

import io.github.marad.neuronote.infra.DefaultUtcTimeProvider
import io.github.marad.neuronote.infra.InMemoryBlockRepository
import io.github.marad.neuronote.infra.InMemoryIdGenerator
import java.time.Instant

fun main() {
    val blockRepository = InMemoryBlockRepository()
    val api = Neuronote(
        InMemoryIdGenerator(),
        DefaultUtcTimeProvider(),
        blockRepository
    )
    println(api.createTextBlock("Hello World"))
    println(api.createHeaderBlock("My title", HeaderLevel.H1))

    println(blockRepository.listBlocks())
}

interface IdGenerator {
    fun nextId(): Int
}

interface TimeProvider {
    fun now(): Instant
}

interface BlockRepository {
    fun save(block: Block)
    fun find(blockId: Int): Block?
}

// TODO: implementacja
interface ContentRepository {
    fun append(noteId: Int, blockToAppend: Int)
    fun insert(noteId: Int, blockToInsert: Int, blockUnderWhichToInsert: Int?)
    fun detachBlock(noteId: Int, blockToDetach: Int)
    fun getContent(noteId: Int): List<Block> // wszystkie bloki zadanej notatki
    // FIXME: być może trzeba połączyć BlockRepository i ContentRepository w jedno NoteRepository? DataStore?
}

class NoteAggregate {
    // TODO: agregat zarządzający stanem notatki
    // dba o spójność procesów, wewnętrzna klasa
}

class Neuronote(
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val blockRepository: BlockRepository
) {
    fun createTextBlock(text: String): Block =
        Block(
            idGenerator.nextId(),
            null,
            BlockType.TEXT,
            timeProvider.now(),
            timeProvider.now(),
            mapOf("value" to text)
        ).also { blockRepository.save(it) }

    fun createHeaderBlock(text: String, headerLevel: HeaderLevel): Block =
        Block(
            idGenerator.nextId(),
            null,
            BlockType.HEADER,
            timeProvider.now(),
            timeProvider.now(),
            mapOf(
                "value" to text,
                "level" to headerLevel.name
            )
        ).also { blockRepository.save(it) }

    fun createNoteBlock(name: String) =
        Block(
            idGenerator.nextId(),
            null,
            BlockType.NOTE,
            timeProvider.now(),
            timeProvider.now(),
            mapOf(
                "name" to name
            )
        ).also { blockRepository.save(it) }

    fun updateBlock(block: Block) = blockRepository.save(block)
    fun findBlock(blockId: Int) = blockRepository.find(blockId)
}