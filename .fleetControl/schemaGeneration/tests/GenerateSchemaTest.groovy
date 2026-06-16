#!/usr/bin/env groovy
/*
 * Unit tests for GenerateSchema.groovy.
 *
 * Covers schema-generation logic only: inferType, stripErb, extractComments,
 * makeProperty, buildProperties, generateSchema (integration).
 *
 * Diff/bump helpers (classifyChanges, recommendBump, applyBump, bumpVersion,
 * renderChange) live in SchemaDiff.groovy and have their own test file:
 *   tests/SchemaDiffTest.groovy
 *
 * Run:
 *   groovy tests/GenerateSchemaTest.groovy     (from .fleetControl/schemaGeneration/)
 *
 * Loads the script via GroovyShell with TEST_MODE=true so its main block
 * is skipped; helper methods are then invokable on the script object.
 */

@Grab('org.yaml:snakeyaml:2.2')

// ---------------------------------------------------------------------------
// Load GenerateSchema.groovy in test mode
// ---------------------------------------------------------------------------
File testFile = new File(getClass().protectionDomain.codeSource.location.toURI())
File scriptFile = new File(testFile.parentFile, '../GenerateSchema.groovy')
assert scriptFile.exists(), "Cannot find GenerateSchema.groovy at ${scriptFile}"

Binding binding = new Binding([TEST_MODE: true])
GroovyShell shell = new GroovyShell(binding)
Script script = shell.parse(scriptFile)
script.run()  // executes top-level definitions, but main is guarded

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
// inferType
// ---------------------------------------------------------------------------
println "\n--- inferType ---"
t.test('boolean')              { assert script.inferType(true) == 'boolean'; assert script.inferType(false) == 'boolean' }
t.test('integer')              { assert script.inferType(0) == 'integer'; assert script.inferType(42) == 'integer'; assert script.inferType(-1L) == 'integer' }
t.test('number (float)')       { assert script.inferType(0.5) == 'number'; assert script.inferType(1.0d) == 'number' }
t.test('string')               { assert script.inferType('hello') == 'string'; assert script.inferType('') == 'string' }
t.test('array')                { assert script.inferType([]) == 'array'; assert script.inferType([1, 2, 3]) == 'array' }
t.test('object (Map)')         { assert script.inferType([:]) == 'object'; assert script.inferType([k: 'v']) == 'object' }
t.test('null falls to string') { assert script.inferType(null) == 'string' }

// ---------------------------------------------------------------------------
// stripErb
// ---------------------------------------------------------------------------
println "\n--- stripErb ---"
t.test('single-line ERB') {
    assert script.stripErb('<%= license_key %>') == '<%= required %>'
}
t.test('string without ERB unchanged') {
    assert script.stripErb('plain string') == 'plain string'
}
t.test('multiple ERB blocks in one string') {
    assert script.stripErb('<%= a %> and <%= b %>') == '<%= required %> and <%= required %>'
}
t.test('multi-line ERB (DOTALL)') {
    assert script.stripErb('<%= foo\n   bar %>') == '<%= required %>'
}

// ---------------------------------------------------------------------------
// extractComments
// ---------------------------------------------------------------------------
println "\n--- extractComments ---"
t.test('single comment attached to key') {
    Map<String, String> c = (Map<String, String>) script.extractComments('# my comment\nfoo: 1\n')
    assert c['foo'] == 'my comment'
}
t.test('multi-line comment joined') {
    Map<String, String> c = (Map<String, String>) script.extractComments('# line one\n# line two\nfoo: 1\n')
    assert c['foo'] == 'line one line two'
}
t.test('blank line resets pending (regression: bug #2)') {
    Map<String, String> c = (Map<String, String>) script.extractComments('# stale comment\n\nfoo: 1\n')
    assert !c.containsKey('foo')
}
t.test("commented-out config doesn't bleed (regression: bug #3)") {
    String yaml = '# log_file_path:\n\n# real description\nai_monitoring:\n  enabled: false\n'
    Map<String, String> c = (Map<String, String>) script.extractComments(yaml)
    assert c['ai_monitoring'] == 'real description'
}
t.test('nested keys use full path') {
    String yaml = '# parent doc\nouter:\n  # child doc\n  inner: 1\n'
    Map<String, String> c = (Map<String, String>) script.extractComments(yaml)
    assert c['outer'] == 'parent doc'
    assert c['outer.inner'] == 'child doc'
}
t.test('deep nesting + sibling keys retain full path (regression: groovy List.pop pops front, not back)') {
    String yaml = '''\
common:
  # license desc
  license_key: x

  # agent desc
  agent_enabled: true

  # kafka desc
  kafka:
    # metrics desc
    metrics:
      # debug desc
      debug:
        enabled: false
'''
    Map<String, String> c = (Map<String, String>) script.extractComments(yaml)
    assert c['common.license_key'] == 'license desc'
    assert c['common.agent_enabled'] == 'agent desc'
    assert c['common.kafka'] == 'kafka desc'
    assert c['common.kafka.metrics'] == 'metrics desc'
    assert c['common.kafka.metrics.debug'] == 'debug desc'
}

// ---------------------------------------------------------------------------
// makeProperty (helpers take override maps as params, so we pass test-locals)
// ---------------------------------------------------------------------------
println "\n--- makeProperty ---"
Map testEnums = ['log_level': ['off', 'info', 'debug']]
Map testTypes = [
    'error_collector.ignore_status_codes': [type: 'array', items: [type: 'integer'], default: [404]],
]

t.test('boolean with default') {
    Map p = script.makeProperty('enabled', true, 'Enable the thing', testEnums, testTypes)
    assert p.type == 'boolean'
    assert p.default == true
    assert p.description == 'Enable the thing'
}
t.test('integer with default, no description') {
    Map p = script.makeProperty('count', 42, '', testEnums, testTypes)
    assert p.type == 'integer'
    assert p.default == 42
    assert !p.containsKey('description')
}
t.test('array with default (regression: bug #10)') {
    Map p = script.makeProperty('ignore_classes', ['FooException'], '', testEnums, testTypes)
    assert p.type == 'array'
    assert p.default == ['FooException']
    assert p.items == [type: 'string']
}
t.test('array items type inferred from first element') {
    Map p = script.makeProperty('codes', [404, 500], '', testEnums, testTypes)
    assert p.items == [type: 'integer']
    assert p.default == [404, 500]
}
t.test('empty array emits anyOf (array or comma-delimited string)') {
    Map p = script.makeProperty('empty', [], '', testEnums, testTypes)
    assert p.anyOf == [
        [type: 'array', items: [type: 'string']],
        [type: 'string'],
    ]
    assert p.default == []
}
t.test('enum override by full path') {
    Map p = script.makeProperty('log_level', 'info', '', testEnums, testTypes)
    assert p.type == 'string'
    assert p.enum == ['off', 'info', 'debug']
    assert p.default == 'info'
}
t.test('type override takes precedence (regression: bug #4)') {
    Map p = script.makeProperty('error_collector.ignore_status_codes', 404, 'doc',
                                testEnums, testTypes)
    assert p.type == 'array'
    assert p.items == [type: 'integer']
    assert p.default == [404]
}
t.test('ERB default stripped') {
    Map p = script.makeProperty('license_key', '<%= license_key %>', '', testEnums, testTypes)
    assert p.default == '<%= required %>'
}

// ---------------------------------------------------------------------------
// buildProperties
// ---------------------------------------------------------------------------
println "\n--- buildProperties ---"
Set testExcludes = ['class_transformer', 'obfuscate_jvm_props', 'security.agent'] as Set

t.test('excludes full-path keys (regression: bug #1)') {
    Map data = [
        class_transformer:    [foo: true],
        obfuscate_jvm_props:  null,
        agent_enabled:        true,
    ]
    Map props = script.buildProperties(data, [:], 'common',
                                        testExcludes, [], testEnums, testTypes)
    assert !props.containsKey('class_transformer')
    assert !props.containsKey('obfuscate_jvm_props')
    assert props.containsKey('agent_enabled')
}
// Groovy 5 quirk: Map.properties / Map['properties'] both invoke the Java
// bean accessor, NOT a key lookup. Use .get('properties') to navigate past
// any field literally named 'properties'.
t.test('excludes ancestor paths') {
    Map data = [
        security: [
            enabled: true,
            agent:   [enabled: false],
        ],
    ]
    Map props = script.buildProperties(data, [:], 'common',
                                        testExcludes, [], testEnums, testTypes)
    Map sec = (Map) props.security.get('properties')
    assert sec.containsKey('enabled')
    assert !sec.containsKey('agent')
}
t.test('descriptions attach to objects and leaves') {
    Map data = [outer: [inner: true]]
    Map comments = [
        'common.outer':       'outer description',
        'common.outer.inner': 'inner description',
    ]
    Map props = script.buildProperties(data, comments, 'common',
                                        testExcludes, [], testEnums, testTypes)
    assert props.outer.description == 'outer description'
    assert props.outer.get('properties').inner.description == 'inner description'
}

// ---------------------------------------------------------------------------
// Integration: generateSchema against an inline YAML fixture
// ---------------------------------------------------------------------------
println "\n--- generateSchema (integration) ---"
String fixture = '''\
common:
  # The license key.
  license_key: '<%= license_key %>'

  # The application name.
  app_name: My App

  # Logging configuration.
  log_level: info

  # Stale comment that should NOT bleed into the next key.

  # Real description for error_collector.
  error_collector:
    enabled: true
    ignore_classes:
      - FooException
    ignore_status_codes: 404

  # Should be dropped (in EXCLUDE_KEYS).
  class_transformer:
    com.foo: false
'''

Set fixtureExcludes = ['class_transformer'] as Set
List fixtureExcludePatterns = []
Map fixtureEnums = ['log_level': ['off', 'info', 'debug']]
Map fixtureTypes = [
    'error_collector.ignore_status_codes': [type: 'array', items: [type: 'integer'], default: [404]],
]
Map schema = script.generateSchema(fixture, fixtureExcludes, fixtureExcludePatterns, fixtureEnums, fixtureTypes)

// Groovy 5 quirk: see note above — use .get('properties') everywhere.
Map schemaProps = (Map) schema.get('properties')

t.test('top-level required is [license_key, app_name]') {
    assert schema.required == ['license_key', 'app_name']
}
t.test('license_key overridden to plain required string') {
    Map lk = (Map) schemaProps.license_key
    assert lk.type == 'string'
    assert lk.minLength == 1
    assert !lk.containsKey('default')
}
t.test('log_level emitted as enum with default') {
    Map ll = (Map) schemaProps.log_level
    assert ll.default == 'info'
    assert ll.enum.contains('info')
}
t.test('class_transformer excluded') {
    assert !schemaProps.containsKey('class_transformer')
}
t.test('error_collector description has no upstream leak') {
    Map ec = (Map) schemaProps.error_collector
    assert ec.description?.contains('Real description')
    assert !(ec.description?.contains('Stale comment'))
}
t.test('ignore_classes has array default') {
    Map ec = (Map) schemaProps.error_collector
    Map ic = (Map) ec.get('properties').ignore_classes
    assert ic.type == 'array'
    assert ic.default == ['FooException']
}
t.test('ignore_status_codes uses type override') {
    Map ec = (Map) schemaProps.error_collector
    Map isc = (Map) ec.get('properties').ignore_status_codes
    assert isc.type == 'array'
    assert isc.items == [type: 'integer']
    assert isc.default == [404]
}

// ---------------------------------------------------------------------------
System.exit(t.summary())
