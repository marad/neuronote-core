import infra.MockTimeProvider
import io.github.marad.neuronote.core.BlockDoesNotExist
import io.github.marad.neuronote.core.BreadcrumbEntry
import io.github.marad.neuronote.core.HeaderLevel
import io.github.marad.neuronote.core.Neuronote
import io.github.marad.neuronote.infra.InMemoryDataStore
import io.github.marad.neuronote.infra.InMemoryIdGenerator
import spock.lang.Specification

import java.time.Instant


class NoteApiSpec extends Specification {
    private InMemoryIdGenerator idGenerator = new InMemoryIdGenerator()
    private MockTimeProvider timeProvider = new MockTimeProvider(Instant.MIN)

    def "should append blocks to note"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
            def title = api.createHeaderBlock("Some title", HeaderLevel.H1)
            def content = api.createTextBlock("some text")
        when:
            note.append(title.id)
            note.append(content.id)
        then:
            note.getContent() == [title, content]
    }

    def "should throw error when trying to append non-existing block"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
        when:
            note.append(1234)
        then:
            def exception = thrown(BlockDoesNotExist)
            exception.blockid == 1234
    }

    def "should insert blocks into note"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
            def title = api.createHeaderBlock("Some title", HeaderLevel.H1)
            def content = api.createTextBlock("some text")
            note.append(title.id)
            note.append(content.id)
        when:
            def toInsert = api.createTextBlock("inserted")
            note.insertOrMove(toInsert.id, title.id)
        then:
            note.getContent() == [title, toInsert, content]
    }

    def "should throw error when inserting or moving non existing block"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
        when:
            note.insertOrMove(1234, null)
        then:
            def exception = thrown(BlockDoesNotExist)
            exception.blockid == 1234
    }

    def "should move block inside note"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
            def title = api.createHeaderBlock("Some title", HeaderLevel.H1)
            def toMove = api.createTextBlock("to move")
            def content = api.createTextBlock("some text")
            note.append(title.id)
            note.append(content.id)
            note.append(toMove.id)
        when:
            note.insertOrMove(toMove.id, title.id)
        then:
            note.getContent() == [title, toMove, content]
    }

    def "should move block to beginning of the note"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
            def title = api.createHeaderBlock("Some title", HeaderLevel.H1)
            def toMove = api.createTextBlock("to move")
            def content = api.createTextBlock("some text")
            note.append(title.id)
            note.append(content.id)
            note.append(toMove.id)
        when:
            note.insertOrMove(toMove.id, null)
        then:
            note.getContent() == [toMove, title, content]
    }

    def "should move block to end of the note"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
            def title = api.createHeaderBlock("Some title", HeaderLevel.H1)
            def toMove = api.createTextBlock("to move")
            def content = api.createTextBlock("some text")
            note.append(title.id)
            note.append(toMove.id)
            note.append(content.id)
        when:
            note.insertOrMove(toMove.id, content.id)
        then:
            note.getContent() == [title, content, toMove]
    }

    def "should detach block from note"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
            def content = api.createTextBlock("some text")
            note.append(content.id)
        when:
            note.remove(content.id)
        then:
            note.getContent() == []
    }

    def "should not throw error when trying to remove non existing block"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
        when:
            note.remove(1234)
        then:
            notThrown(BlockDoesNotExist)
    }

    def "should remove block if it's not referenced by other notes"() {
        given:
            InMemoryDataStore blockRepository = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, blockRepository)
            def note = api.createNote("Some Note")
            def content = api.createTextBlock("some text")
            note.append(content.id)
        when:
            note.remove(content.id)
        then:
            blockRepository.listBlocks().count { it.id == content.id } == 0
    }

    def "should NOT remove block if it IS referenced by other notes"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def note = api.createNote("Some Note")
            def otherNote = api.createNote("Other note")
            def content = api.createTextBlock("some text")
            note.append(content.id)
            otherNote.append(content.id)
        when:
            note.remove(content.id)
        then:
            dataStore.listBlocks().count { it.id == content.id } == 1
    }

    def "should generate breadcrumb"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def parentNote = api.createNote("Parent Note")
            def intermediateNote = api.createNote("Intermediate Note")
            def childNote = api.createNote("Child note")
            parentNote.append(intermediateNote.id)
            intermediateNote.append(childNote.id)
        when:
            def breadcrumb = childNote.getBreadcrumb()
        then:
            breadcrumb == [
                    new BreadcrumbEntry(parentNote.id, parentNote.getName()),
                    new BreadcrumbEntry(intermediateNote.id, intermediateNote.getName()),
                    new BreadcrumbEntry(childNote.id, childNote.getName()),
            ]
    }

    def "note can be appended only to one other note"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def sourceNote = api.createNote("Source Note")
            def targetNote = api.createNote("Target Note")
            def noteToMove = api.createNote("Note to move")
            sourceNote.append(noteToMove.id)
        when:
            targetNote.append(noteToMove.id)
        then:
            dataStore.getParents(noteToMove.id).collect { it.id } == [targetNote.id]
    }

    def "note can be inserted into only one other note"() {
        given:
            InMemoryDataStore dataStore = new InMemoryDataStore()
            def api = new Neuronote(idGenerator, timeProvider, dataStore)
            def sourceNote = api.createNote("Source Note")
            def targetNote = api.createNote("Target Note")
            def noteToMove = api.createNote("Note to move")
            sourceNote.append(noteToMove.id)
        when:
            targetNote.insertOrMove(noteToMove.id, null)
        then:
            dataStore.getParents(noteToMove.id).collect { it.id } == [targetNote.id]
    }
}