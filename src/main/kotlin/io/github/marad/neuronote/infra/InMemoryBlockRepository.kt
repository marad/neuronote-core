package io.github.marad.neuronote.infra

import io.github.marad.neuronote.core.Block
import io.github.marad.neuronote.core.BlockRepository

class InMemoryBlockRepository : BlockRepository {
    private val  blocks = HashMap<Int, Block>()
    override fun save(block: Block) {
        blocks[block.id] = block.copy()
    }

    override fun find(blockId: Int): Block? {
        return blocks.getOrDefault(blockId, null)
    }

    fun listBlocks(): List<Block> = blocks.values.map { it.copy() }
}