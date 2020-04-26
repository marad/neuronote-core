package io.github.marad.neuronote.core

import java.time.Instant

data class BreadcrumbEntry(
    val blockId: BlockId,
    val name: String
)

data class Note(
    val id: BlockId,
    val parentId: BlockId?,
    val creationTime: Instant,
    val lastUpdateTime: Instant,
    val name: String,
    val content: List<Block>
)

sealed class Block(val type: BlockType) {
    abstract val id: BlockId
    abstract val creationTime: Instant
    abstract val lastUpdateTime: Instant
}
data class TextBlock(
    override val id: BlockId,
    override val creationTime: Instant,
    override val lastUpdateTime: Instant,
    val text: String
) : Block(BlockType.TEXT)

data class HeaderBlock(
    override val id: BlockId,
    override val creationTime: Instant,
    override val lastUpdateTime: Instant,
    val text: String,
    val level: HeaderLevel
) : Block(BlockType.HEADER)

data class NoteBlock(
    override val id: BlockId,
    val parentId: BlockId?,
    override val creationTime: Instant,
    override val lastUpdateTime: Instant,
    val name: String
) : Block(BlockType.NOTE)

enum class HeaderLevel {
    H1, H2, H3
}
