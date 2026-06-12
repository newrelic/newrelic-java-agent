#!/usr/bin/env groovy
/*
 * Unit tests for SchemaDiff.groovy.
 *
 * Covers diff classification and version-bump arithmetic:
 * classifyChanges, classifyLeaf (via classifyChanges), renderChange,
 * recommendBump, applyBump, bumpVersion.
 *
 * Run:
 *   groovy tests/SchemaDiffTest.groovy     (from .fleetControl/schemaGeneration/)
 *
 * SchemaDiff.groovy has no main, so loading via GroovyShell.parse is enough
 * to make its top-level functions available on the returned Script instance.
 */

@Grab('org.yaml:snakeyaml:2.2')
import org.yaml.snakeyaml.Yaml

// ---------------------------------------------------------------------------
// Load SchemaDiff.groovy
// ---------------------------------------------------------------------------
File testFile = new File(getClass().protectionDomain.codeSource.location.toURI())
File scriptFile = new File(testFile.parentFile, '../SchemaDiff.groovy')
assert scriptFile.exists(), "Cannot find SchemaDiff.groovy at ${scriptFile}"

GroovyShell shell = new GroovyShell()
Script lib = shell.parse(scriptFile)
lib.run()

// ---------------------------------------------------------------------------
// Tiny test runner
// ---------------------------------------------------------------------------
class TestRunner {
    int passed = 0
    int failed = 0
    List<String> failures = []

    void test(String name, Closure body) {
        try {
            body()
            println "  PASS  ${name}"
            passed++
        } catch (Throwable e) {
            println "  FAIL  ${name}"
            println "        ${e.message ?: e.class.simpleName}"
            failed++
            failures << name
        }
    }

    int summary() {
        println "\n${passed} passed, ${failed} failed"
        return failed > 0 ? 1 : 0
    }
}

TestRunner t = new TestRunner()

// ---------------------------------------------------------------------------
// classifyChanges
// ---------------------------------------------------------------------------
println "\n--- classifyChanges ---"

Closure objNode = { Map props, List required = null, Object additional = true ->
    Map node = [type: 'object', properties: props, additionalProperties: additional]
    if (required != null) node.required = required
    return node
}

Closure byKind = { List<Map> changes ->
    changes.collectEntries { [(it.kind): it] }
}

t.test('no changes returns empty list') {
    Map s = objNode([foo: [type: 'string', default: 'x']])
    assert lib.classifyChanges(s, s) == []
}
t.test('added property is additive') {
    Map old = objNode([:])
    Map ne  = objNode([foo: [type: 'string']])
    List<Map> ch = lib.classifyChanges(old, ne)
    assert ch.size() == 1
    assert ch[0].path == 'foo'
    assert ch[0].kind == 'added'
    assert ch[0].severity == 'additive'
}
t.test('removed property is breaking') {
    Map old = objNode([foo: [type: 'string']])
    Map ne  = objNode([:])
    List<Map> ch = lib.classifyChanges(old, ne)
    assert ch[0].kind == 'removed'
    assert ch[0].severity == 'breaking'
}
t.test('type change is breaking') {
    Map old = objNode([foo: [type: 'string']])
    Map ne  = objNode([foo: [type: 'integer']])
    Map ch = byKind(lib.classifyChanges(old, ne))
    assert ch.type_changed.severity == 'breaking'
    assert ch.type_changed.detail.contains('string')
    assert ch.type_changed.detail.contains('integer')
}
t.test('required added is breaking') {
    Map old = objNode([foo: [type: 'string']], [])
    Map ne  = objNode([foo: [type: 'string']], ['foo'])
    Map ch = byKind(lib.classifyChanges(old, ne))
    assert ch.required_added.severity == 'breaking'
    assert ch.required_added.path == 'foo'
}
t.test('required removed is additive') {
    Map old = objNode([foo: [type: 'string']], ['foo'])
    Map ne  = objNode([foo: [type: 'string']], [])
    Map ch = byKind(lib.classifyChanges(old, ne))
    assert ch.required_removed.severity == 'additive'
}
t.test('additionalProperties tightened is breaking') {
    Map old = objNode([:], null, true)
    Map ne  = objNode([:], null, false)
    Map ch = byKind(lib.classifyChanges(old, ne))
    assert ch.additional_properties_tightened.severity == 'breaking'
}
t.test('additionalProperties implicit-true matches explicit-true (no change)') {
    Map old = [type: 'object', properties: [:]]
    Map ne  = [type: 'object', properties: [:], additionalProperties: true]
    assert lib.classifyChanges(old, ne) == []
}
t.test('enum value removed is breaking') {
    Map old = objNode([x: [type: 'string', enum: ['a', 'b', 'c']]])
    Map ne  = objNode([x: [type: 'string', enum: ['a', 'c']]])
    List<Map> ch = lib.classifyChanges(old, ne)
    Map removed = ch.find { it.kind == 'enum_value_removed' }
    assert removed.severity == 'breaking'
    assert removed.detail.contains("'b'")
}
t.test('enum value added is additive') {
    Map old = objNode([x: [type: 'string', enum: ['a']]])
    Map ne  = objNode([x: [type: 'string', enum: ['a', 'b']]])
    Map added = lib.classifyChanges(old, ne).find { it.kind == 'enum_value_added' }
    assert added.severity == 'additive'
}
t.test('enum newly introduced is breaking') {
    Map old = objNode([x: [type: 'string']])
    Map ne  = objNode([x: [type: 'string', enum: ['a', 'b']]])
    Map ch = byKind(lib.classifyChanges(old, ne))
    assert ch.enum_introduced.severity == 'breaking'
}
t.test('enum removed entirely is additive') {
    Map old = objNode([x: [type: 'string', enum: ['a', 'b']]])
    Map ne  = objNode([x: [type: 'string']])
    Map ch = byKind(lib.classifyChanges(old, ne))
    assert ch.enum_removed_entirely.severity == 'additive'
}
t.test('default changed is additive') {
    Map old = objNode([x: [type: 'string', default: 'a']])
    Map ne  = objNode([x: [type: 'string', default: 'b']])
    Map ch = byKind(lib.classifyChanges(old, ne))
    assert ch.default_changed.severity == 'additive'
    assert ch.default_changed.detail.contains('a')
    assert ch.default_changed.detail.contains('b')
}
t.test('description changed is cosmetic') {
    Map old = objNode([x: [type: 'string', description: 'old']])
    Map ne  = objNode([x: [type: 'string', description: 'new']])
    Map ch = byKind(lib.classifyChanges(old, ne))
    assert ch.description_changed.severity == 'cosmetic'
}
t.test('nested object recurses') {
    Map old = objNode([outer: objNode([inner: [type: 'string']])])
    Map ne  = objNode([outer: objNode([inner: [type: 'integer']])])
    List<Map> ch = lib.classifyChanges(old, ne)
    assert ch.size() == 1
    assert ch[0].kind == 'type_changed'
    assert ch[0].path == 'outer.inner'
}
t.test('renderChange formatting') {
    assert lib.renderChange([path: 'foo.bar', kind: 'added',
                             severity: 'additive', detail: 'new property']) ==
           '+ foo.bar: new property'
    assert lib.renderChange([path: 'foo', kind: 'removed',
                             severity: 'breaking', detail: '']) == '- foo'
    assert lib.renderChange([path: 'foo', kind: 'type_changed',
                             severity: 'breaking', detail: 'type x → y']) ==
           '~ foo: type x → y'
}

// ---------------------------------------------------------------------------
// recommendBump + applyBump
// ---------------------------------------------------------------------------
println "\n--- recommendBump ---"
t.test('any breaking → major') {
    List changes = [
        [severity: 'cosmetic'],
        [severity: 'additive'],
        [severity: 'breaking'],
    ]
    assert lib.recommendBump(changes) == 'major'
}
t.test('additive without breaking → minor') {
    List changes = [[severity: 'cosmetic'], [severity: 'additive']]
    assert lib.recommendBump(changes) == 'minor'
}
t.test('cosmetic only → patch') {
    List changes = [[severity: 'cosmetic']]
    assert lib.recommendBump(changes) == 'patch'
}
t.test('empty changes → none') {
    assert lib.recommendBump([]) == 'none'
}

println "\n--- applyBump ---"
t.test('major resets minor and patch') {
    assert lib.applyBump('1.2.3', 'major') == '2.0.0'
}
t.test('minor resets patch only') {
    assert lib.applyBump('1.2.3', 'minor') == '1.3.0'
}
t.test('patch increments patch only') {
    assert lib.applyBump('1.2.3', 'patch') == '1.2.4'
}
t.test('none returns input unchanged') {
    assert lib.applyBump('1.2.3', 'none') == '1.2.3'
}
t.test('non-semver throws') {
    try {
        lib.applyBump('not-semver', 'major')
        assert false : 'expected throw'
    } catch (IllegalArgumentException ignored) {
        // expected
    }
}

// ---------------------------------------------------------------------------
// bumpVersion (file I/O)
// ---------------------------------------------------------------------------
println "\n--- bumpVersion ---"

Closure withTempYaml = { String content, Closure body ->
    File f = File.createTempFile('config-def-', '.yml')
    try {
        f.text = content
        body(f)
    } finally {
        f.delete()
    }
}

String fixtureYaml = '''\
configurationDefinitions:
  - platform: KUBERNETESCLUSTER
    description: Test agent configuration
    type: agent-config
    version: 1.2.3
    schema: ./schemas/config.json
    format: yml
'''

t.test('reads current version, returns old/new pair') {
    withTempYaml(fixtureYaml) { File f ->
        def (String oldV, String newV) = lib.bumpVersion(f, 'minor', false)
        assert oldV == '1.2.3'
        assert newV == '1.3.0'
    }
}
t.test('default (write=false) does NOT touch the file') {
    withTempYaml(fixtureYaml) { File f ->
        String before = f.text
        lib.bumpVersion(f, 'major', false)
        assert f.text == before
    }
}
t.test('--ci (write=true) writes the new version') {
    withTempYaml(fixtureYaml) { File f ->
        lib.bumpVersion(f, 'major', true)
        Map data = (Map) new Yaml().load(f.text)
        assert data.configurationDefinitions[0].version == '2.0.0'
    }
}
t.test("'none' bump leaves file untouched even with write=true") {
    withTempYaml(fixtureYaml) { File f ->
        String before = f.text
        def (String oldV, String newV) = lib.bumpVersion(f, 'none', true)
        assert oldV == newV
        assert f.text == before
    }
}
t.test('missing version field throws') {
    withTempYaml('configurationDefinitions:\n  - platform: foo\n') { File f ->
        try {
            lib.bumpVersion(f, 'major', false)
            assert false : 'expected throw'
        } catch (IllegalStateException ignored) {
            // expected
        }
    }
}

// ---------------------------------------------------------------------------
System.exit(t.summary())