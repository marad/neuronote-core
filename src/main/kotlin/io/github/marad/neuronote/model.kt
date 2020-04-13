package io.github.marad.neuronote

import java.time.Instant

data class Block(
    val id: Int,
    val creationTime: Instant,
    val lastUpdateTime: Instant,
    val content: BlockContent
)


interface BlockContent

data class TextBlock(val text: String) : BlockContent
data class ListBlock(val text: String) : BlockContent
data class SpoilerBlock(val  text: String) : BlockContent
data class TitleBlock(val text: String) : BlockContent
data class QuoteBlock(val text: String) : BlockContent
data class EmphasizedBlock(val text: String): BlockContent

data class ImageBlock(val url: String) : BlockContent
data class NoteLinkBlock(val noteId: Int) : BlockContent

