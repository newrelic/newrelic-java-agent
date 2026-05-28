#!/usr/bin/env groovy
/*
 * Fleet Control Config Schema Generator — Java Agent (Groovy option)
 *
 * Reads newrelic.yml from the working tree and writes JSON Schema Draft
 * 2020-12 to .fleetControl/schemas/config.json.
 *
 * Source resolution:
 *   1. NEWRELIC_YML env var, if set
 *   2. <repo-root>/newrelic-agent/src/main/resources/newrelic.yml
 *      (resolved relative to this script's location)
 *
 * Exit codes:
 *   0 — no schema changes (or first run)
 *   1 — schema changed (CI should commit the updated files)
 *
 * Run standalone:
 *   groovy generate-schema.groovy
 *
 * Override the source file:
 *   NEWRELIC_YML=/path/to/newrelic.yml groovy generate-schema.groovy
 *
 * ---------------------------------------------------------------------------
 * Why every object emits `additionalProperties: true`
 * ---------------------------------------------------------------------------
 * The generator sets `additionalProperties: true` on the root schema and on
 * every nested object node (see `makeProperty`, `buildProperties`, and
 * `generateSchema`). This is intentional and serves two purposes:
 *
 *   1. Forward compatibility. The agent ships new config keys in every
 *      release. A Fleet Control deployment may be validating against a
 *      schema that was generated from an older newrelic.yml — strict
 *      validation (the JSON Schema default when this is omitted is
 *      effectively `additionalProperties: true`, but `false` is what most
 *      tooling defaults to in practice) would reject any newer key,
 *      breaking users who upgrade the agent before the schema is republished.
 *
 *   2. Coverage gaps. Some keys are deliberately excluded (see EXCLUDE_KEYS)
 *      and a few yml entries have shapes the generator cannot represent
 *      faithfully (e.g. null-valued parents, dotted module names under
 *      class_transformer). Permitting unknown properties means a config
 *      that uses those still validates instead of being flagged as malformed.
 *
 * If a future requirement calls for strict validation (catch typos, reject
 * unknown keys), flip these to `false` — but doing so should be paired with
 * a release process that republishes the schema in lockstep with the agent.
 */

@Grab('org.yaml:snakeyaml:2.2')
@Grab('com.networknt:json-schema-validator:1.5.5')
@Grab('com.fasterxml.jackson.core:jackson-databind:2.18.1')
import org.yaml.snakeyaml.Yaml
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.fasterxml.jackson.databind.ObjectMapper

import java.util.regex.Pattern

// ---------------------------------------------------------------------------
// Paths — all resolved relative to this script. Script lives at
// <repo-root>/.fleetControl/schemaGeneration/ so the repo root is two
// levels up.
// ---------------------------------------------------------------------------
final File SCRIPT_DIR = new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile ?: new File('.')
final File FLEET_CONTROL_DIR = SCRIPT_DIR.parentFile
final File REPO_ROOT = FLEET_CONTROL_DIR.parentFile
final File SCHEMA_DIR = new File(FLEET_CONTROL_DIR, 'schemas')
final File SCHEMA_PATH = new File(SCHEMA_DIR, 'config.json')
final File CONFIG_DEF_PATH = new File(FLEET_CONTROL_DIR, 'configurationDefinitions.yml')
final File DEFAULT_YML_PATH = new File(REPO_ROOT, 'newrelic-agent/src/main/resources/newrelic.yml')

// ---------------------------------------------------------------------------
// Enum / special-value overrides
// ---------------------------------------------------------------------------
final Map<String, List<String>> ENUM_OVERRIDES = [
        'log_level':                                                 ['off', 'severe', 'warning', 'info', 'fine', 'finer', 'finest'],
        'transaction_tracer.record_sql':                             ['off', 'raw', 'obfuscated'],
        'attributes.http_attribute_mode':                            ['standard', 'legacy', 'both'],
        'security.mode':                                             ['IAST', 'RASP'],
        'distributed_tracing.sampler.remote_parent_sampled':         ['default', 'always_on', 'always_off'],
        'distributed_tracing.sampler.remote_parent_not_sampled':     ['default', 'always_on', 'always_off'],
]

// ---------------------------------------------------------------------------
// Type override helpers — factory methods for common schema patterns.
// ---------------------------------------------------------------------------

/**
 * Creates a schema for keys that accept either a YAML array of strings OR a
 * comma-delimited string. Used for keys parsed via BaseConfig.getUniqueStrings().
 */
static Map<String, Object> stringArrayOrDelimited(List<String> defaultValue = []) {
    Map<String, Object> schema = [
            anyOf: [
                    [type: 'array', items: [type: 'string']] as Map<String, Object>,
                    [type: 'string']
            ]
    ] as Map<String, Object>
    if (defaultValue != null) {
        schema.put('default', defaultValue)
    }
    return schema
}

/**
 * Creates a schema for status code keys that accept an integer, array of integers,
 * or a comma-delimited string with optional range syntax (e.g., "400-499").
 */
static Map<String, Object> statusCodeArrayOrRange(List<Integer> defaultValue = []) {
    Map<String, Object> schema = [
            anyOf: [
                    [type: 'integer'] as Map<String, Object>,
                    [type: 'array', items: [type: 'integer']] as Map<String, Object>,
                    [type: 'string', description: 'Comma-separated integers or ranges (e.g., "400-499")'] as Map<String, Object>
            ]
    ] as Map<String, Object>
    if (defaultValue != null && !defaultValue.isEmpty()) {
        schema.put('default', defaultValue)
    }
    return schema
}

// ---------------------------------------------------------------------------
// Type overrides — when YAML default doesn't reflect the documented semantic.
// ---------------------------------------------------------------------------
final Map<String, Map<String, Object>> TYPE_OVERRIDES = [
        // --- Status code keys (integer, array, or range string) ---
        'error_collector.ignore_status_codes':   statusCodeArrayOrRange([404]),
        'error_collector.expected_status_codes': statusCodeArrayOrRange(),

        // --- Keys using getUniqueStrings (array or comma-delimited string) ---
        'attributes.include':                                      stringArrayOrDelimited(),
        'attributes.exclude':                                      stringArrayOrDelimited(),
        'transaction_tracer.attributes.include':                   stringArrayOrDelimited(),
        'transaction_tracer.attributes.exclude':                   stringArrayOrDelimited(),
        'transaction_events.attributes.include':                   stringArrayOrDelimited(),
        'transaction_events.attributes.exclude':                   stringArrayOrDelimited(),
        'span_events.attributes.include':                          stringArrayOrDelimited(),
        'span_events.attributes.exclude':                          stringArrayOrDelimited(),
        'browser_monitoring.disabled_auto_pages':                  stringArrayOrDelimited(),
        'browser_monitoring.attributes.include':                   stringArrayOrDelimited(),
        'browser_monitoring.attributes.exclude':                   stringArrayOrDelimited(),
        'application_logging.forwarding.context_data.include':     stringArrayOrDelimited(),
        'application_logging.forwarding.context_data.exclude':     stringArrayOrDelimited(),
        'application_logging.forwarding.labels.exclude':           stringArrayOrDelimited(),
        'class_transformer.classloader_excludes':                  stringArrayOrDelimited(),

        // --- labels is a map of name→value pairs ---
        // Its only YAML example is commented out, so SnakeYAML parses it as null
        // and the generator would otherwise emit type: string. Per the YAML
        // comments: max 64 labels, names/values up to 255 chars.
        'labels': [
                type:                 'object',
                additionalProperties: [type: 'string', maxLength: 255] as Map<String, Object>,
                maxProperties:        64,
        ] as Map<String, Object>,
]

// ---------------------------------------------------------------------------
// Keys to exclude from the generated schema.
//
// Use this for settings the Fleet Control UI shouldn't surface — typically
// because they are derived at runtime, structurally awkward to represent in
// JSON Schema, or only relevant for internal/debug scenarios.
//
// Both full dotted paths and parent paths work — e.g. 'security.agent'
// excludes only that subtree, while 'class_transformer' excludes everything
// under it.
//
// To exclude a setting, add its dotted path to this set. Example:
//   'transaction_tracer.log_sql',
// ---------------------------------------------------------------------------
final Set<String> EXCLUDE_KEYS = [
        // The agent derives both URIs from license_key by default; explicit
        // overrides are an advanced regional/private-cloud concern that
        // doesn't belong in the standard config UI.
        'metric_ingest_uri',
        'event_ingest_uri',

        // All children are commented out in newrelic.yml so SnakeYAML parses
        // this as null. Including it would emit a misleading {type: "string"}
        // entry. Add a TYPE_OVERRIDE below if you need to surface it.
        'obfuscate_jvm_props',
] as Set<String>

// ---------------------------------------------------------------------------
// Key patterns to exclude — regex patterns for keys that should be excluded.
// Used for instrumentation module toggles under class_transformer that have
// dotted package names (e.g., com.newrelic.instrumentation.servlet-user).
// ---------------------------------------------------------------------------
final List<Pattern> EXCLUDE_KEY_PATTERNS = [
        // Instrumentation module toggles under class_transformer with dotted names.
        // Any key under class_transformer that contains a dot is an instrumentation
        // module name (e.g., com.newrelic.instrumentation.servlet-user, org.example.mymodule).
        // These are dynamically named and shouldn't be exposed in Fleet Control UI.
        // Pattern matches: class_transformer.<anything>.<anything>...
        Pattern.compile(/^class_transformer\.[^.]+\..+/),
] as List<Pattern>

// ---------------------------------------------------------------------------
// Helpers — all static; none reference instance state.
// Map-typed parameters use <String, Object> consistently so IntelliJ's static
// checker can resolve getAt/putAt without "cannot be applied" warnings.
// ---------------------------------------------------------------------------

/** Replace ERB placeholders with a descriptive placeholder string. (?s) = DOTALL */
static String stripErb(String value) {
    return value.replaceAll(/(?s)<%=.*?%>/, '<%= required %>')
}

/** Map a value to a JSON Schema type string. Returns "string" for null. */
static String inferType(Object value) {
    if (value instanceof Boolean) return 'boolean'
    if (value instanceof Integer || value instanceof Long) return 'integer'
    if (value instanceof Number) return 'number'
    if (value instanceof List) return 'array'
    if (value instanceof Map) return 'object'
    return 'string'
}

static Map<String, Object> itemsTypeFromList(List<Object> value) {
    if (!value) return [type: 'string'] as Map<String, Object>
    return [type: inferType(value[0])] as Map<String, Object>
}

/** Build a JSON Schema property node for a single leaf. */
static Map<String, Object> makeProperty(String keyPath, Object value, String description,
                                        Map<String, List<String>> enumOverrides,
                                        Map<String, Map<String, Object>> typeOverrides) {
    String flatKey = keyPath.tokenize('.').last()
    Map<String, Object> prop

    if (typeOverrides.containsKey(keyPath)) {
        prop = new LinkedHashMap<String, Object>(typeOverrides.get(keyPath))
    } else if (enumOverrides.containsKey(keyPath) || enumOverrides.containsKey(flatKey)) {
        List<String> enumVals = enumOverrides.get(keyPath) ?: enumOverrides.get(flatKey)
        prop = [type: 'string', enum: enumVals] as Map<String, Object>
        if (value != null && value in enumVals) prop.put('default', value)
    } else {
        String jsonType = inferType(value)
        prop = [type: jsonType] as Map<String, Object>

        if (jsonType == 'array') {
            prop.put('items', itemsTypeFromList((value instanceof List) ? (List<Object>) value : [] as List<Object>))
            if (value instanceof List && value) prop.put('default', value)
        } else if (jsonType == 'object') {
            prop.put('additionalProperties', true)
        } else if (value != null) {
            if (value instanceof String && ((String) value).contains('<%=')) {
                prop.put('default', stripErb((String) value))
            } else {
                prop.put('default', value)
            }
        }
    }

    if (description) prop.put('description', description.trim())
    return prop
}

// ---------------------------------------------------------------------------
// YAML comment extraction — line-by-line scanner.
// Blank lines end docblocks; only the most recent contiguous # block
// attaches to the next key.
// ---------------------------------------------------------------------------

/** Tracked indent-stack entry — typed so IntelliJ resolves field access. */
@groovy.transform.TupleConstructor
class IndentEntry {
    int indent
    String key
}

static Map<String, String> extractComments(String rawText) {
    Map<String, String> comments = [:]
    List<String> pending = []
    List<IndentEntry> indentStack = []
    Pattern keyRe = ~/^([a-zA-Z_][\w\-]*)\s*:/

    rawText.split('\n').each { String line ->
        String stripped = line.replaceAll(/^\s+/, '')
        int indent = line.length() - stripped.length()

        if (!stripped) {
            pending.clear()
            return
        }

        if (stripped.startsWith('#')) {
            pending.add(stripped.substring(1).trim())
            return
        }

        def m = keyRe.matcher(stripped)
        if (m.find() && !stripped.startsWith('-')) {
            String key = m.group(1)
            while (!indentStack.isEmpty() && indentStack.last().indent >= indent) {
                indentStack.removeLast()
            }
            indentStack.add(new IndentEntry(indent, key))
            String keyPath = indentStack.collect { it.key }.join('.')
            if (pending) {
                comments.put(keyPath, pending.join(' '))
            }
            pending.clear()
        } else {
            pending.clear()
        }
    }
    return comments
}

// ---------------------------------------------------------------------------
// Recursively build JSON Schema properties from the parsed YAML tree
// ---------------------------------------------------------------------------
static Map<String, Object> buildProperties(Map<String, Object> data, Map<String, String> comments, String prefix,
                                           Set<String> excludeKeys,
                                           List<Pattern> excludeKeyPatterns,
                                           Map<String, List<String>> enumOverrides,
                                           Map<String, Map<String, Object>> typeOverrides) {
    Map<String, Object> properties = [:]

    data.each { String key, Object value ->
        String keyPath = prefix ? "${prefix}.${key}" : key
        String flatKey = keyPath.startsWith('common.') ? keyPath.substring('common.'.length()) : keyPath

        // Skip excluded keys (full-path match or ancestor match)
        if (excludeKeys.any { String ex -> flatKey == ex || flatKey.startsWith(ex + '.') }) {
            return
        }

        // Skip keys matching exclusion patterns (e.g., instrumentation module names)
        if (excludeKeyPatterns.any { Pattern p -> p.matcher(flatKey).matches() }) {
            return
        }

        String desc = comments.get("common.${flatKey}".toString()) ?: comments.get(flatKey) ?: ''

        Map<String, Object> prop
        if (value instanceof Map && !((Map) value).isEmpty()) {
            Map<String, Object> nested = buildProperties((Map<String, Object>) value, comments, keyPath,
                    excludeKeys, excludeKeyPatterns, enumOverrides, typeOverrides)
            prop = [
                    type:                 'object',
                    properties:           nested,
                    additionalProperties: true,
            ] as Map<String, Object>
            if (desc) prop.put('description', desc.trim())
        } else {
            prop = makeProperty(flatKey, value, desc, enumOverrides, typeOverrides)
        }
        properties.put(key, prop)
    }
    return properties
}

// ---------------------------------------------------------------------------
// Schema diff classification — distinguishes breaking from additive changes.
// Change records are plain Maps:
//   [path: String, kind: String, severity: 'breaking' | 'additive', detail: String]
// NOTE: Map values are accessed via .get('properties') because Groovy 5
// resolves .properties / ['properties'] to the bean accessor, not the key.
// ---------------------------------------------------------------------------

static String renderChange(Map<String, Object> c) {
    String kind = (String) c.get('kind')
    String sym = kind == 'added' ? '+' : (kind == 'removed' ? '-' : '~')
    String detail = (String) c.get('detail')
    String path = (String) c.get('path')
    return detail ? "${sym} ${path}: ${detail}" : "${sym} ${path}"
}

static List<Map<String, Object>> classifyChanges(Map<String, Object> oldS, Map<String, Object> newS, String path = '') {
    List<Map<String, Object>> changes = []

    // required: gained → breaking, lost → additive
    Set<String> oldReq = ((List<String>) (oldS.get('required') ?: [])) as Set<String>
    Set<String> newReq = ((List<String>) (newS.get('required') ?: [])) as Set<String>
    (newReq - oldReq).sort().each { String k ->
        changes.add([path: path ? "${path}.${k}".toString() : k,
                     kind: 'required_added', severity: 'breaking',
                     detail: 'now required'] as Map<String, Object>)
    }
    (oldReq - newReq).sort().each { String k ->
        changes.add([path: path ? "${path}.${k}".toString() : k,
                     kind: 'required_removed', severity: 'additive',
                     detail: 'no longer required'] as Map<String, Object>)
    }

    // additionalProperties: implicit `true` is the JSON Schema default
    Object oldAP = oldS.containsKey('additionalProperties') ? oldS.get('additionalProperties') : true
    Object newAP = newS.containsKey('additionalProperties') ? newS.get('additionalProperties') : true
    if (oldAP == true && newAP == false) {
        changes.add([path: path ?: '<root>',
                     kind: 'additional_properties_tightened', severity: 'breaking',
                     detail: 'additionalProperties: true → false'] as Map<String, Object>)
    } else if (oldAP == false && newAP == true) {
        changes.add([path: path ?: '<root>',
                     kind: 'additional_properties_loosened', severity: 'additive',
                     detail: 'additionalProperties: false → true'] as Map<String, Object>)
    }

    // Walk properties (.get because of the Groovy 5 bean-accessor quirk)
    Map<String, Object> oldProps = (Map<String, Object>) (oldS.get('properties') ?: [:])
    Map<String, Object> newProps = (Map<String, Object>) (newS.get('properties') ?: [:])
    (oldProps.keySet() + newProps.keySet()).sort().each { String key ->
        String childPath = path ? "${path}.${key}".toString() : key
        if (!oldProps.containsKey(key)) {
            changes.add([path: childPath, kind: 'added', severity: 'additive',
                         detail: 'new property'] as Map<String, Object>)
        } else if (!newProps.containsKey(key)) {
            changes.add([path: childPath, kind: 'removed', severity: 'breaking',
                         detail: 'property removed'] as Map<String, Object>)
        } else {
            Map<String, Object> op = (Map<String, Object>) oldProps.get(key)
            Map<String, Object> np = (Map<String, Object>) newProps.get(key)
            if (op.get('type') == 'object' && np.get('type') == 'object') {
                changes.addAll(classifyChanges(op, np, childPath))
            } else {
                changes.addAll(classifyLeaf(op, np, childPath))
            }
        }
    }
    return changes
}

static List<Map<String, Object>> classifyLeaf(Map<String, Object> op, Map<String, Object> np, String path) {
    List<Map<String, Object>> changes = []

    if (op.get('type') != np.get('type')) {
        changes.add([path: path, kind: 'type_changed', severity: 'breaking',
                     detail: "type ${op.get('type')} → ${np.get('type')}".toString()] as Map<String, Object>)
    }

    Object oe = op.get('enum')
    Object ne = np.get('enum')
    if (oe == null && ne != null) {
        changes.add([path: path, kind: 'enum_introduced', severity: 'breaking',
                     detail: "newly constrained to enum ${ne}".toString()] as Map<String, Object>)
    } else if (oe != null && ne == null) {
        changes.add([path: path, kind: 'enum_removed_entirely', severity: 'additive',
                     detail: 'enum constraint removed'] as Map<String, Object>)
    } else if (oe && ne && (oe as Set) != (ne as Set)) {
        ((oe as Set) - (ne as Set)).sort().each { v ->
            changes.add([path: path, kind: 'enum_value_removed', severity: 'breaking',
                         detail: "enum value '${v}' removed".toString()] as Map<String, Object>)
        }
        ((ne as Set) - (oe as Set)).sort().each { v ->
            changes.add([path: path, kind: 'enum_value_added', severity: 'additive',
                         detail: "enum value '${v}' added".toString()] as Map<String, Object>)
        }
    }

    if (op.get('default') != np.get('default')) {
        changes.add([path: path, kind: 'default_changed', severity: 'additive',
                     detail: "default ${op.get('default')} → ${np.get('default')}".toString()] as Map<String, Object>)
    }

    if (op.get('description') != np.get('description')) {
        changes.add([path: path, kind: 'description_changed', severity: 'cosmetic',
                     detail: 'description updated'] as Map<String, Object>)
    }

    return changes
}

// ---------------------------------------------------------------------------
// Semver bump — derived from the highest-severity change in the diff
// ---------------------------------------------------------------------------

/** Translate a list of Change records into a semver bump kind. */
static String recommendBump(List<Map<String, Object>> changes) {
    if (changes.any { it.get('severity') == 'breaking' }) return 'major'
    if (changes.any { it.get('severity') == 'additive' }) return 'minor'
    if (changes.any { it.get('severity') == 'cosmetic' }) return 'patch'
    return 'none'
}

/** Apply a bump kind to a semver string. 'none' returns the input unchanged. */
static String applyBump(String version, String bump) {
    if (bump == 'none') return version
    List<String> parts = version.tokenize('.')
    if (parts.size() != 3 || !parts.every { it.isInteger() }) {
        throw new IllegalArgumentException("version '${version}' is not semver MAJOR.MINOR.PATCH")
    }
    int major = parts[0] as int
    int minor = parts[1] as int
    int patch = parts[2] as int
    switch (bump) {
        case 'major': return "${major + 1}.0.0".toString()
        case 'minor': return "${major}.${minor + 1}.0".toString()
        case 'patch': return "${major}.${minor}.${patch + 1}".toString()
        default: throw new IllegalArgumentException("unknown bump kind '${bump}'")
    }
}

/**
 * Read configurationDefinitions.yml, compute the new version, optionally
 * write it back. Returns [oldVersion, newVersion]. Mutates the file only
 * when `write` is true.
 *
 * Reading parses with SnakeYAML for structural validation; writing does
 * a targeted regex replacement of just the `version:` line so the rest of
 * the file is preserved byte-for-byte. Same approach in every generator
 * → trivial cross-language parity.
 */
static List<String> bumpVersion(File yamlPath, String bump, boolean write) {
    String text = yamlPath.text
    Map<String, Object> data = (Map<String, Object>) new Yaml().load(text)
    List defs = (List) (data?.get('configurationDefinitions') ?: [])
    if (defs.isEmpty() || !((Map) defs[0]).containsKey('version')) {
        throw new IllegalStateException(
                "${yamlPath}: configurationDefinitions[0].version not found"
        )
    }
    String oldVersion = ((Map) defs[0]).get('version').toString()
    String newVersion = applyBump(oldVersion, bump)
    if (write && newVersion != oldVersion) {
        Pattern versionLine = Pattern.compile(/(?m)^(\s*version:\s*)(\S+)(\s*)$/)
        StringBuffer sb = new StringBuffer()
        def matcher = versionLine.matcher(text)
        int matches = 0
        while (matcher.find()) {
            matcher.appendReplacement(sb,
                    java.util.regex.Matcher.quoteReplacement(
                            "${matcher.group(1)}${newVersion}${matcher.group(3)}"
                    )
            )
            matches++
        }
        matcher.appendTail(sb)
        if (matches != 1) {
            throw new IllegalStateException(
                    "${yamlPath}: expected exactly 1 'version:' line, found ${matches}"
            )
        }
        yamlPath.text = sb.toString()
    }
    return [oldVersion, newVersion]
}

// ---------------------------------------------------------------------------
// I/O
// ---------------------------------------------------------------------------
static String loadNewrelicYml(File defaultPath) {
    String envPath = System.getenv('NEWRELIC_YML')
    File source = envPath ? new File(envPath) : defaultPath
    if (!source.exists()) {
        throw new FileNotFoundException(
                "newrelic.yml not found at ${source.absolutePath}. " +
                        "Set NEWRELIC_YML to override the source path."
        )
    }
    println "Reading: ${source.absolutePath}"
    return source.getText('UTF-8')
}

static Map<String, Object> loadExisting(File path) {
    if (!path.exists()) return [:] as Map<String, Object>
    try {
        return (Map<String, Object>) new JsonSlurper().parse(path)
    } catch (ignored) {
        return [:] as Map<String, Object>
    }
}

static void writeSchema(Map<String, Object> schema, File path) {
    path.parentFile.mkdirs()
    path.text = JsonOutput.prettyPrint(JsonOutput.toJson(schema)) + '\n'
}

/**
 * Validate the generated schema against the JSON Schema 2020-12 meta-schema.
 * networknt's validator ships the standard meta-schemas as JAR resources, so
 * the canonical https://json-schema.org/draft/2020-12/schema URI resolves
 * locally without a network round-trip. Hard-fails (exit 2) on any error —
 * a structurally invalid schema should never be written.
 */
static void validateMetaSchema(Map<String, Object> schema) {
    try {
        def factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        def metaSchema = factory.getSchema(URI.create('https://json-schema.org/draft/2020-12/schema'))
        def schemaNode = new ObjectMapper().readTree(JsonOutput.toJson(schema))
        def errors = metaSchema.validate(schemaNode)
        if (errors.isEmpty()) {
            println 'Meta-schema validation passed (Draft 2020-12)'
            return
        }
        System.err.println 'Meta-schema validation FAILED:'
        errors.each { System.err.println "  ${it}" }
        System.exit(2)
    } catch (Throwable t) {
        // Validator-itself failure (network resolution, classloading, etc.) is
        // not the same as schema-is-invalid. Soft-warn so the generator still
        // produces output for offline devs; CI catches it via Recipe 1/3.
        System.err.println "  meta-schema check skipped: ${t.class.simpleName}: ${t.message}"
    }
}

// ---------------------------------------------------------------------------
// Schema generation
// ---------------------------------------------------------------------------
static Map<String, Object> generateSchema(String rawText, Set<String> excludeKeys,
                                          List<Pattern> excludeKeyPatterns,
                                          Map<String, List<String>> enumOverrides,
                                          Map<String, Map<String, Object>> typeOverrides) {
    String safeText = rawText.replaceAll(/(?s)<%=.*?%>/, '"__ERB_PLACEHOLDER__"')
    Map<String, Object> data = (Map<String, Object>) new Yaml().load(safeText)
    Map<String, Object> common = (Map<String, Object>) (data?.get('common') ?: [:])

    Map<String, String> comments = extractComments(rawText)
    Map<String, Object> properties = buildProperties(common, comments, 'common',
            excludeKeys, excludeKeyPatterns, enumOverrides, typeOverrides)

    // Override license_key (ERB placeholder → required string)
    if (properties.containsKey('license_key')) {
        properties.put('license_key', [
                type:        'string',
                description: 'New Relic license key associated with your account. ' +
                        "Binds the agent's data to your account in the New Relic UI.",
                minLength:   1,
        ] as Map<String, Object>)
    }

    return [
            $schema:              'https://json-schema.org/draft/2020-12/schema',
            title:                'New Relic Java Agent Configuration',
            description:          'Fleet Control configuration schema for the New Relic Java agent. ' +
                    'Generated from newrelic-agent/src/main/resources/newrelic.yml.',
            type:                 'object',
            properties:           properties,
            required:             ['license_key', 'app_name'],
            additionalProperties: true,
    ] as Map<String, Object>
}

// ---------------------------------------------------------------------------
// Main — skipped when the script is loaded by tests with TEST_MODE=true
// ---------------------------------------------------------------------------
if (binding.variables.get('TEST_MODE') != true) {
    // Parse CLI flags
    boolean ciMode = false
    boolean forceMode = false
    String overrideBump = null
    List<String> rawArgs = (binding.variables.get('args') ?: []) as List<String>
    rawArgs.each { String arg ->
        if (arg == '--ci') {
            ciMode = true
        } else if (arg == '--force') {
            forceMode = true
        } else if (arg.startsWith('--bump=')) {
            overrideBump = arg.substring('--bump='.length())
            if (!(overrideBump in ['major', 'minor', 'patch', 'none'])) {
                System.err.println "Invalid --bump value: ${overrideBump}"
                System.exit(2)
            }
        }
    }

    String rawText = loadNewrelicYml(DEFAULT_YML_PATH)
    Map<String, Object> newSchema = generateSchema(rawText, EXCLUDE_KEYS, EXCLUDE_KEY_PATTERNS, ENUM_OVERRIDES, TYPE_OVERRIDES)

    validateMetaSchema(newSchema)

    // In force mode, skip comparison and just write the schema
    if (forceMode) {
        writeSchema(newSchema, SCHEMA_PATH)
        println "Wrote:   ${SCHEMA_PATH} (force mode — no comparison)"
        System.exit(0)
    }

    Map<String, Object> oldSchema = loadExisting(SCHEMA_PATH)

    writeSchema(newSchema, SCHEMA_PATH)
    println "Wrote:   ${SCHEMA_PATH}"

    if (oldSchema.isEmpty()) {
        println '\nFirst run — schema created.'
        System.exit(0)
    }

    List<Map<String, Object>> changes = classifyChanges(oldSchema, newSchema)

    if (changes) {
        List<Map<String, Object>> breaking = changes.findAll { it.get('severity') == 'breaking' }
        List<Map<String, Object>> additive = changes.findAll { it.get('severity') == 'additive' }
        List<Map<String, Object>> cosmetic = changes.findAll { it.get('severity') == 'cosmetic' }
        println "\nSchema changes (${changes.size()}):"
        if (breaking) {
            println "  BREAKING (${breaking.size()}):"
            breaking.each { Map<String, Object> ch -> println "    ${renderChange(ch)}" }
        }
        if (additive) {
            println "  ADDITIVE (${additive.size()}):"
            additive.each { Map<String, Object> ch -> println "    ${renderChange(ch)}" }
        }
        if (cosmetic) {
            println "  COSMETIC (${cosmetic.size()}):"
            cosmetic.each { Map<String, Object> ch -> println "    ${renderChange(ch)}" }
        }
    } else {
        println '\nNo schema changes.'
    }

    String autoBump = recommendBump(changes)
    String chosen = overrideBump ?: autoBump
    def (String oldV, String newV) = bumpVersion(CONFIG_DEF_PATH, chosen, ciMode)
    if (chosen == 'none' || newV == oldV) {
        println "\nRecommended bump: none (${oldV} unchanged)"
    } else if (overrideBump && overrideBump != autoBump) {
        println "\nRecommended bump: ${autoBump} → overridden to ${chosen} (${oldV} → ${newV})"
    } else {
        println "\nRecommended bump: ${chosen} (${oldV} → ${newV})"
    }
    if (ciMode && newV != oldV) {
        println "Wrote:   ${CONFIG_DEF_PATH}"
    }

    System.exit(changes ? 1 : 0)
}
