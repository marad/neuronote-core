package io.github.marad.neuronote.core

fun DbBlock.toDomain(): Block =
    when(type) {
        BlockType.TEXT -> TextBlock(id, creationTime, lastUpdateTime, properties["value"] ?: "")
        BlockType.NOTE -> NoteBlock(id, parentId, creationTime, lastUpdateTime, properties["value"] ?: "")
        BlockType.HEADER -> HeaderBlock(id, creationTime, lastUpdateTime, properties["value"] ?: "",
            properties["level"]?.let { HeaderLevel.valueOf(it)} ?: HeaderLevel.H1)
        else -> TextBlock(id, creationTime, lastUpdateTime, properties["value"] ?: "")
    }

fun Block.toDb(): DbBlock =
    when(this) {
        is TextBlock -> toDb()
        is NoteBlock -> toDb()
        is HeaderBlock -> toDb()
    }

fun TextBlock.toDb(): DbBlock = DbBlock(id, null, type, creationTime, lastUpdateTime, mapOf("value" to text))
fun NoteBlock.toDb(): DbBlock = DbBlock(id, parentId, type, creationTime, lastUpdateTime, mapOf("value" to name))
fun NoteBlock.toNote(content: List<Block>): Note =
    Note(id, parentId, creationTime, lastUpdateTime, name, content)
fun HeaderBlock.toDb(): DbBlock = DbBlock(id, null, type, creationTime, lastUpdateTime, mapOf(
    "value" to text,
    "level" to level.name
))


