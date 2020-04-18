package io.github.marad.neuronote.infra

import io.github.marad.neuronote.core.Block
import io.github.marad.neuronote.core.DataStore

class InMemoryDataStore : DataStore {
    private val blocks = mutableMapOf<Int, Block>()
    private val content = mutableMapOf<ContentKey, ContentEntry>()

    override fun saveBlock(block: Block) {
        blocks[block.id] = block.copy()
    }

    override fun findBlock(blockId: Int): Block? {
        return blocks.getOrDefault(blockId, null)
    }

    override fun prepend(noteId: Int, blockToPrepend: Int) {
        val firstBlock = content.values.filter { it.parentBlockId == noteId }.minBy { it.rank }
        if (firstBlock == null) {
            append(noteId, blockToPrepend)
        } else {
            val key = ContentKey(noteId, blockToPrepend)
            content[key] = ContentEntry(noteId, blockToPrepend, firstBlock.rank-1)
        }
    }

    override fun append(noteId: Int, blockToAppend: Int) {
        val highestRank = content.values.maxBy { it.rank }?.rank ?: 0
        val key = ContentKey(noteId, blockToAppend)
        content[key] = ContentEntry(noteId, blockToAppend, highestRank + 1)
    }

    override fun insertOrMove(noteId: Int, blockToInsert: Int, blockUnderWhichToInsert: Int?) {
        if (blockUnderWhichToInsert == null) {
            prepend(noteId, blockToInsert)
        } else {
            val toInsertKey = ContentKey(noteId, blockToInsert)
            content.remove(toInsertKey)
            val upperBlockKey = ContentKey(noteId, blockUnderWhichToInsert)
            val upperBlock = content[upperBlockKey]

            if (upperBlock != null) {
                content.entries
                    .filter { it.value.parentBlockId == noteId && it.value.rank > upperBlock.rank }
                    .forEach {
                        it.value.rank += 1
                    }
            }

            val toInsert = ContentEntry(noteId, blockToInsert, (upperBlock?.rank ?: -1) + 1)
            content[toInsertKey] = toInsert
        }
    }

    override fun detachBlock(noteId: Int, blockToDetach: Int) {
        val toRemoveKey = ContentKey(noteId, blockToDetach)
        content.remove(toRemoveKey)
        val referenceCount = content.values.count { it.blockId == blockToDetach }
        if (referenceCount == 0) {
            blocks.remove(blockToDetach)
        }
    }

    override fun detachBlockFromAll(blockToDetach: Int) {
        content.keys.filter { it.blockId == blockToDetach }
            .forEach {
                content.remove(it)
            }
    }

    override fun getContent(noteId: Int): List<Block> {
        return content.values
            .filter { it.parentBlockId == noteId }
            .sortedBy { it.rank }.also { print(it) }
            .mapNotNull { blocks[it.blockId] }
    }

    override fun getParents(noteId: Int): List<Block> {
        return content.values
            .filter { it.blockId == noteId }
            .map { blocks[it.parentBlockId] ?: error("Block must exist to be attached in note") }
    }

    fun listBlocks(): List<Block> = blocks.values.map { it.copy() }

    private data class ContentKey(val parentBlockId: Int, val blockId: Int)
    private data class ContentEntry(val parentBlockId: Int, val blockId: Int, var rank: Int)
}