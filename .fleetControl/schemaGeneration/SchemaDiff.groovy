#!/usr/bin/env groovy
/*
 * Schema diff & version bump library.
 *
 * Shared helpers used by both:
 *   - GenerateSchema.groovy       (per-push regen)
 *   - BumpSchemaVersion.groovy    (release-time bump)
 *
 * No main. Loaded via GroovyShell.parse() from the consuming scripts:
 *
 *     File libFile = new File(SCRIPT_DIR, 'SchemaDiff.groovy')
 *     GroovyShell shell = new GroovyShell()
 *     Script lib = shell.parse(libFile)
 *     lib.run()  // executes top-level definitions
 *     lib.classifyChanges(oldSchema, newSchema)
 *     lib.recommendBump(changes)
 *     lib.applyBump(version, kind)
 *     lib.bumpVersion(yamlFile, kind, write)
 *     lib.loadExisting(jsonFile)
 *     lib.renderChange(changeRecord)
 */

@Grab('org.yaml:snakeyaml:2.2')
import org.yaml.snakeyaml.Yaml
import groovy.json.JsonSlurper

import java.util.regex.Pattern

// ---------------------------------------------------------------------------
// I/O — loading existing schema JSON
// ---------------------------------------------------------------------------

static Map<String, Object> loadExisting(File path) {
    if (!path.exists()) return [:] as Map<String, Object>
    try {
        return (Map<String, Object>) new JsonSlurper().parse(path)
    } catch (ignored) {
        return [:] as Map<String, Object>
    }
}

// ---------------------------------------------------------------------------
// Schema diff classification — distinguishes breaking from additive changes.
// Change records are plain Maps:
//   [path: String, kind: String, severity: 'breaking' | 'additive' | 'cosmetic', detail: String]
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