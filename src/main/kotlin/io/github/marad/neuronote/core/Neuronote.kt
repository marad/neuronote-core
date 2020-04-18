package io.github.marad.neuronote.core

import io.github.marad.neuronote.infra.DefaultUtcTimeProvider
import io.github.marad.neuronote.infra.InMemoryDataStore
import io.github.marad.neuronote.infra.InMemoryIdGenerator
import java.lang.RuntimeException
import java.time.Instant

fun main() {
    val blockRepository = InMemoryDataStore()
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

interface DataStore {
    fun saveBlock(block: Block)
    fun findBlock(blockId: Int): Block?

    fun prepend(noteId: Int, blockToPrepend: Int)
    fun append(noteId: Int, blockToAppend: Int)
    fun insertOrMove(noteId: Int, blockToInsert: Int, blockUnderWhichToInsert: Int?)
    fun detachBlock(noteId: Int, blockToDetach: Int)
    fun detachBlockFromAll(blockToDetach: Int)
    fun getContent(noteId: Int): List<Block> // wszystkie bloki zadanej notatki
    fun getParents(noteId: Int): List<Block>
}

data class BlockDoesNotExist(val blockid: Int): RuntimeException("Block $blockid does not exist")

class Note(private val block: Block, private val dataStore: DataStore) {
    val id = block.id

    init {
        assert(block.type == BlockType.NOTE) { "This is not a block note" }
    }

    fun append(blockToAppend: Int) {
        val block = dataStore.findBlock(blockToAppend)
        if (block != null) {
            if (block.type == BlockType.NOTE) {
                dataStore.detachBlockFromAll(block.id)
            }
            dataStore.append(id, blockToAppend)
        } else {
            throw BlockDoesNotExist(blockToAppend)
        }
    }

    fun insertOrMove(blockToInsert: Int, blockUnderWhichToInsert: Int?) {
        val block = dataStore.findBlock(blockToInsert)
        if (block != null) {
            if (block.type == BlockType.NOTE) {
                dataStore.detachBlockFromAll(block.id)
            }
            dataStore.insertOrMove(id, blockToInsert, blockUnderWhichToInsert)
        } else {
            throw BlockDoesNotExist(blockToInsert)
        }
    }

    fun remove(blockToRemove: Int) {
        dataStore.detachBlock(id, blockToRemove)
    }

    fun getName(): String {
        return block.properties["name"] ?: error("Note should have a name")
    }

    fun getContent(): List<Block> {
        return dataStore.getContent(id)
    }

    fun getBreadcrumb(): List<BreadcrumbEntry> {
        val result = mutableListOf<BreadcrumbEntry>()
        var currentBlock = dataStore.findBlock(id)!!
        while(true) {
            result.add(0, BreadcrumbEntry(currentBlock.id, currentBlock.properties["name"] ?: error("Expected a note")))
            val parent = dataStore.getParents(currentBlock.id).firstOrNull()
            if (parent != null) {
                currentBlock = parent
            } else {
                break
            }
        }
        return result
    }
}

class Neuronote(
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val dataStore: DataStore
) {
    fun createNote(name: String): Note {
        val noteBlock = createNoteBlock(name)
        return Note(noteBlock, dataStore)
    }

    fun createTextBlock(text: String): Block =
        Block(
            idGenerator.nextId(),
            null,
            BlockType.TEXT,
            timeProvider.now(),
            timeProvider.now(),
            mapOf("value" to text)
        ).also { dataStore.saveBlock(it) }

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
        ).also { dataStore.saveBlock(it) }

    private fun createNoteBlock(name: String) =
        Block(
            idGenerator.nextId(),
            null,
            BlockType.NOTE,
            timeProvider.now(),
            timeProvider.now(),
            mapOf(
                "name" to name
            )
        ).also { dataStore.saveBlock(it) }

    fun updateBlock(block: Block) = dataStore.saveBlock(block)
    fun findBlock(blockId: Int) = dataStore.findBlock(blockId)
}