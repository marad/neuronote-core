import infra.MockTimeProvider
import io.github.marad.neuronote.core.BlockType
import io.github.marad.neuronote.core.HeaderLevel
import io.github.marad.neuronote.core.Neuronote
import io.github.marad.neuronote.infra.InMemoryBlockRepository
import io.github.marad.neuronote.infra.InMemoryIdGenerator
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant


class BlockApi extends Specification {
    private InMemoryIdGenerator idGenerator = new InMemoryIdGenerator()
    private MockTimeProvider timeProvider = new MockTimeProvider(Instant.MIN)

    @Unroll
    def "should create and save block in the repository"() {
        given:
            InMemoryBlockRepository blockRepository = new InMemoryBlockRepository()
            def api = new Neuronote(idGenerator, timeProvider, blockRepository)
        when:
            def result = func(api)
        then:
            result.properties == resultProperties
            result.type == expectedBlockType
            result.creationTime == timeProvider.mockedTime
            result.lastUpdateTime == timeProvider.mockedTime
        and:
            blockRepository.listBlocks().first() == result
        where:
            resultProperties                 | expectedBlockType | func
            ["value": "test"]                | BlockType.TEXT    | { it -> it.createTextBlock("test") }
            ["value": "test", "level": "H1"] | BlockType.HEADER  | { it -> it.createHeaderBlock("test", HeaderLevel.H1) }
            ["name": "note name"]            | BlockType.NOTE    | { it -> it.createNoteBlock("note name") }
    }

    def "should update existing block"() {
        given:
            InMemoryBlockRepository blockRepository = new InMemoryBlockRepository()
            def api = new Neuronote(idGenerator, timeProvider, blockRepository)
            def block = api.createTextBlock("test")
        when:
            def updatedBlock = block.copy(block.id, block.parentId, block.type, block.creationTime, block.lastUpdateTime, ["value": "updated"])
            api.updateBlock(updatedBlock)
        then:
            blockRepository.listBlocks().first() == updatedBlock
    }

    def "should find existing block"() {
        given:
            InMemoryBlockRepository blockRepository = new InMemoryBlockRepository()
            def api = new Neuronote(idGenerator, timeProvider, blockRepository)
            def block = api.createTextBlock("test")
        when:
            def result = api.findBlock(block.id)
        then:
            result == block
    }

    def "should return null for non-existing block"() {
        given:
            InMemoryBlockRepository blockRepository = new InMemoryBlockRepository()
            def api = new Neuronote(idGenerator, timeProvider, blockRepository)
        when:
            def result = api.findBlock(123)
        then:
            result == null
    }

    def "should validate created block"() {
        // TODO
    }
}