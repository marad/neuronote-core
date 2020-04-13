package io.github.marad.neuronote.core

import java.lang.RuntimeException
import java.time.Instant

enum class BlockType(val requiredProperties: List<String>) {
    TEXT(listOf("value")),
    HEADER(listOf("value", "level")),
    NOTE(listOf("name"))
//    list,
//    spoiler,
//    image,
//    link,
//    checkbox,
//    divider,
//    code
}

data class Block (
    val id: Int,
    val parentId: Int?,
    val type: BlockType,
    val creationTime: Instant,
    val lastUpdateTime: Instant,
    val properties: Map<String, String>
) {
    init { validate() }

    private fun validate() {
        val missingProperties = properties.keys.subtract(type.requiredProperties)
        if (missingProperties.isNotEmpty()) {
            throw BlockMissingRequiredProperties(type, missingProperties)
        }
    }
}

data class BlockMissingRequiredProperties(val blockType: BlockType, val missingProperties: Set<String>)
    : RuntimeException("Block of type $blockType has missing properties: $missingProperties")

enum class HeaderLevel {
    H1, H2, H3
}
