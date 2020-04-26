package io.github.marad.neuronote.core

import java.lang.RuntimeException
import java.time.Instant

enum class BlockType(internal val requiredProperties: List<String>) {
    TEXT(listOf("value")),
    HEADER(listOf("value", "level")),
    NOTE(listOf("value")),
//    NOTE_LINK(listOf("noteId")),
}

data class DbBlock (
    val id: Int,
    val parentId: Int?,
    val type: BlockType,
    val creationTime: Instant,
    val lastUpdateTime: Instant,
    val properties: Map<String, String>
) {
//    init { validate() }
//    private fun validate() {
//        val missingProperties = type.requiredProperties.subtract(properties.keys)
//        if (missingProperties.isNotEmpty()) {
//            throw BlockMissingRequiredProperties(type, missingProperties)
//        }
//    }
}

data class BlockMissingRequiredProperties(val blockType: BlockType, val missingProperties: Set<String>)
    : RuntimeException("Block of type $blockType has missing properties: $missingProperties")


