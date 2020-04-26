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
}

interface IdGenerator {
    fun nextId(): BlockId
}

interface TimeProvider {
    fun now(): Instant
}

interface DataStore {
    fun saveBlock(block: DbBlock)
    fun findBlock(blockId: Int): DbBlock?
    fun getParents(blockId: Int): List<DbBlock>
    fun isParent(blockId: Int, potentialParent: Int): Boolean

    fun prepend(noteId: Int, blockToPrepend: Int)
    fun append(noteId: Int, blockToAppend: Int)
    fun insertOrMove(noteId: Int, blockToInsert: Int, blockUnderWhichToInsert: Int?)
    fun detachBlock(noteId: Int, blockToDetach: Int)
    fun detachBlockFromAll(blockToDetach: Int)
    fun getContent(noteId: Int): List<DbBlock> // wszystkie bloki zadanej notatki
}

data class BlockDoesNotExist(val blockid: Int): RuntimeException("Block $blockid does not exist")
data class NoteDoesNotExist(val noteId: Int): RuntimeException("Note $noteId does not exist")

class NoteAgg(val noteBlock: DbBlock,
              private val dataStore: DataStore,
              private val idGenerator: IdGenerator,
              private val timeProvider: TimeProvider
) {
    val id = noteBlock.id

    init {
        assert(noteBlock.type == BlockType.NOTE) { "This is not a block note" }
    }

//    fun append(blockToAppend: Int) {
//        val block = dataStore.findBlock(blockToAppend)
//        if (block != null) {
//            if (block.type == BlockType.NOTE) {
//                dataStore.detachBlockFromAll(block.id)
//            }
//            dataStore.append(id, blockToAppend)
//        } else {
//            throw BlockDoesNotExist(blockToAppend)
//        }
//    }
//
//    fun insertOrMove(blockToInsert: Int, blockUnderWhichToInsert: Int?) {
//        val block = dataStore.findBlock(blockToInsert)
//        if (block != null) {
//            if (block.type == BlockType.NOTE) {
//                dataStore.detachBlockFromAll(block.id)
//            }
//            dataStore.insertOrMove(id, blockToInsert, blockUnderWhichToInsert)
//        } else {
//            throw BlockDoesNotExist(blockToInsert)
//        }
//    }

//    fun remove(blockToRemove: Int) {
//        dataStore.detachBlock(id, blockToRemove)
//    }
//
//    fun getName(): String {
//        return noteBlock.properties["name"] ?: error("Note should have a name")
//    }
//
//    fun getContent(): List<DbBlock> {
//        return dataStore.getContent(id)
//    }
//
//    fun getBreadcrumb(): List<BreadcrumbEntry> {
//        val result = mutableListOf<BreadcrumbEntry>()
//        var currentBlock = dataStore.findBlock(id)!!
//        while(true) {
//            result.add(0, BreadcrumbEntry(currentBlock.id, currentBlock.properties["name"] ?: error("Expected a note")))
//            val parent = dataStore.getParents(currentBlock.id).firstOrNull()
//            if (parent != null) {
//                currentBlock = parent
//            } else {
//                break
//            }
//        }
//        return result
//    }

//    fun insertTextBlock(text: String, blockUnderWhichToInsert: Int?): DbBlock =
//        createTextBlock(text).also {
//            dataStore.saveBlock(it)
//            dataStore.insertOrMove(id, it.id, blockUnderWhichToInsert)
//        }

//    fun appendTextBlock(text: String): DbBlock =
//        createTextBlock(text).also {
//            dataStore.saveBlock(it)
//            dataStore.append(id, it.id)
//        }

//    fun findBlock(blockId: Int) = dataStore.findBlock(blockId) ?: throw BlockDoesNotExist(blockId)
//    fun updateBlock(block: DbBlock) = dataStore.saveBlock(block)

//    private fun createTextBlock(text: String) =
//        DbBlock(
//            idGenerator.nextId(),
//            null,
//            BlockType.TEXT,
//            timeProvider.now(),
//            timeProvider.now(),
//            mapOf("value" to text)
//        )
}

class Neuronote(
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
    private val dataStore: DataStore
) {
    fun createNote(name: String, parentNoteId: BlockId? = null): Note {
        val noteBlock = createNoteBlock(name, parentNoteId).toDomain() as NoteBlock
        return noteBlock.toNote(emptyList())
    }

    fun findNote(noteId: BlockId): Note {
        val block = dataStore.findBlock(noteId)
        if (block == null || block.type != BlockType.NOTE) {
            throw NoteDoesNotExist(noteId)
        } else {
            val content = dataStore.getContent(noteId).map { it.toDomain() }
            return (block.toDomain() as NoteBlock).toNote(content)
        }
    }

    fun updateBlock(block: Block) {
        dataStore.saveBlock(block.toDb())
    }

    fun getBreadcrumb(noteId: BlockId): List<BreadcrumbEntry> {
        val result = mutableListOf<BreadcrumbEntry>()
        var currentBlock = dataStore.findBlock(noteId)!!
        while(true) {
            result.add(0, BreadcrumbEntry(currentBlock.id, currentBlock.properties["value"] ?: ""))
            val parent = dataStore.getParents(currentBlock.id).firstOrNull()
            if (parent != null) {
                currentBlock = parent
            } else {
                break
            }
        }
        return result
    }

    fun append(noteId: BlockId, blockToAppend: Int) {
        val block = dataStore.findBlock(blockToAppend)
        if (block != null) {
            if (block.type == BlockType.NOTE) {
                dataStore.detachBlockFromAll(block.id)
            }
            dataStore.append(noteId, blockToAppend)
        } else {
            throw BlockDoesNotExist(blockToAppend)
        }
    }

    fun insertOrMove(noteId: BlockId, blockToInsert: Int, blockUnderWhichToInsert: Int?) {
        val block = dataStore.findBlock(blockToInsert)
        if (block != null) {
            if (block.type == BlockType.NOTE) {
                dataStore.detachBlockFromAll(block.id)
            }
            dataStore.insertOrMove(noteId, blockToInsert, blockUnderWhichToInsert)
        } else {
            throw BlockDoesNotExist(blockToInsert)
        }
    }


    fun insertTextBlock(noteId: BlockId, text: String, blockUnderWhichToInsert: Int?): Block =
        createDbTextBlock(text).also {
            dataStore.saveBlock(it)
            dataStore.insertOrMove(noteId, it.id, blockUnderWhichToInsert)
        }.toDomain()

    fun appendTextBlock(noteId: BlockId, text: String): Block =
        createDbTextBlock(text).also {
            dataStore.saveBlock(it)
            dataStore.append(noteId, it.id)
        }.toDomain()

    fun remove(noteId: BlockId, blockToRemove: BlockId) {
        dataStore.detachBlock(noteId, blockToRemove)
    }

    fun transformBlock(blockId: BlockId, targetType: BlockType): Block {
        val block = dataStore.findBlock(blockId) ?: throw BlockDoesNotExist(blockId)
        val updated = block.copy(type = targetType)
        dataStore.saveBlock(updated)
        return updated.toDomain()
    }

    private fun createDbTextBlock(text: String): DbBlock =
        DbBlock(
            idGenerator.nextId(),
            null,
            BlockType.TEXT,
            timeProvider.now(),
            timeProvider.now(),
            mapOf("value" to text)
        ).also { dataStore.saveBlock(it) }

    private fun createNoteBlock(name: String, parentBlockId: BlockId?) =
        DbBlock(
            idGenerator.nextId(),
            parentBlockId,
            BlockType.NOTE,
            timeProvider.now(),
            timeProvider.now(),
            mapOf(
                "value" to name
            )
        ).also { dataStore.saveBlock(it) }
}