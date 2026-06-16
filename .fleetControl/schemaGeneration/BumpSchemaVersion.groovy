#!/usr/bin/env groovy
/*
 * Agent Config Schema Version Bump — release-time entry point.
 *
 * Compares the schema at a prior git ref (typically the last agent release
 * tag) to the current on-disk schema, classifies the diff, and bumps the
 * version in .fleetControl/configurationDefinitions.yml accordingly.
 *
 * Usage:
 *   groovy BumpSchemaVersion.groovy --since=<ref>              # dry-run
 *   groovy BumpSchemaVersion.groovy --since=<ref> --ci         # write
 *
 * Exit codes:
 *   0 — no bump needed (no schema diff, or bootstrap case where the ref
 *       predates the .fleetControl/ system)
 *   1 — bump applied (with --ci) or recommended (without --ci)
 *   2 — generator failure (uncaught exception, missing args, etc.)
 *
 * The bump kind (major/minor/patch) is determined by the cumulative schema
 * diff between <ref> and HEAD. See SchemaDiff.groovy for classification rules.
 *
 * The schema file path is read from configurationDefinitions.yml's `schema`
 * field at both the historical ref and HEAD, so renames or relocations of
 * the schema file don't break the bump path.
 */

@Grab('org.yaml:snakeyaml:2.2')
import org.yaml.snakeyaml.Yaml
import groovy.json.JsonSlurper

// ---------------------------------------------------------------------------
// Paths
// ---------------------------------------------------------------------------
final File SCRIPT_DIR = new File(getClass().protectionDomain.codeSource.location.toURI()).parentFile ?: new File('.')
final File FLEET_CONTROL_DIR = SCRIPT_DIR.parentFile
final File REPO_ROOT = FLEET_CONTROL_DIR.parentFile
final File CONFIG_DEF_PATH = new File(FLEET_CONTROL_DIR, 'configurationDefinitions.yml')

// Path to configurationDefinitions.yml relative to repo root — used for `git show`.
final String CONFIG_DEF_REPO_PATH = '.fleetControl/configurationDefinitions.yml'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Run `git show <ref>:<path>` and return the file contents, or null if the
 * file doesn't exist at that ref. Bootstrapping pre-system tags relies on
 * this returning null.
 */
static String gitShow(File repoRoot, String ref, String path) {
    Process p = ['git', '-C', repoRoot.absolutePath, 'show', "${ref}:${path}"].execute()
    StringBuilder out = new StringBuilder()
    StringBuilder err = new StringBuilder()
    p.waitForProcessOutput(out, err)
    if (p.exitValue() != 0) {
        return null  // file doesn't exist at ref, or ref is invalid
    }
    return out.toString()
}

/**
 * Parse configurationDefinitions.yml text → [version, schemaPath].
 *
 * If `strict` is true, missing fields throw. If false, missing fields return
 * null in the corresponding slot — used for the historical parse so a tag
 * with an incomplete configurationDefinitions.yml (e.g., a placeholder that
 * predates the schema work) maps to "bootstrap, no bump" rather than a
 * generator failure.
 */
static List<String> parseDefinitions(String yamlText, String contextLabel, boolean strict = true) {
    Map<String, Object> data = (Map<String, Object>) new Yaml().load(yamlText)
    List defs = (List) (data?.get('configurationDefinitions') ?: [])
    if (defs.isEmpty()) {
        if (strict) throw new IllegalStateException("${contextLabel}: configurationDefinitions list is empty")
        return [null, null]
    }
    Map<String, Object> def0 = (Map<String, Object>) defs[0]
    String version = def0.get('version')?.toString()
    String schemaPath = def0.get('schema')?.toString()
    if (strict) {
        if (!version) throw new IllegalStateException("${contextLabel}: version missing")
        if (!schemaPath) throw new IllegalStateException("${contextLabel}: schema missing")
    }
    return [version, schemaPath]
}

/**
 * Resolve the schema path declared in configurationDefinitions.yml to a
 * repo-rooted path string suitable for `git show`. The yaml's `schema:`
 * field is relative to configurationDefinitions.yml's location.
 */
static String resolveSchemaRepoPath(String configDefRepoPath, String schemaRelative) {
    File configDir = new File(configDefRepoPath).parentFile
    File resolved = new File(configDir, schemaRelative).canonicalFile
    File cwd = new File('.').canonicalFile
    String rel = cwd.toPath().relativize(resolved.toPath()).toString()
    // git show expects forward slashes; normalize on Windows in case.
    return rel.replace('\\', '/')
}

// ---------------------------------------------------------------------------
// Main — wrapped in try/catch so uncaught exceptions exit 2
// ---------------------------------------------------------------------------
if (binding.variables.get('TEST_MODE') != true) {
    try {
        // Parse CLI flags
        String sinceRef = null
        boolean ciMode = false
        List<String> rawArgs = (binding.variables.get('args') ?: []) as List<String>
        rawArgs.each { String arg ->
            if (arg.startsWith('--since=')) {
                sinceRef = arg.substring('--since='.length())
            } else if (arg == '--ci') {
                ciMode = true
            } else {
                System.err.println "Unknown flag: ${arg}"
                System.err.println "Usage: groovy BumpSchemaVersion.groovy --since=<ref> [--ci]"
                System.exit(2)
            }
        }
        if (!sinceRef) {
            System.err.println "Missing required flag: --since=<ref>"
            System.err.println "Usage: groovy BumpSchemaVersion.groovy --since=<ref> [--ci]"
            System.exit(2)
        }

        // Load the SchemaDiff library
        File schemaDiffFile = new File(SCRIPT_DIR, 'SchemaDiff.groovy')
        GroovyShell shell = new GroovyShell()
        Script lib = shell.parse(schemaDiffFile)
        lib.run()

        println "Comparing schema at ${sinceRef} to current HEAD"

        // Step 1: fetch historical configurationDefinitions.yml
        String historicalDefsText = gitShow(REPO_ROOT, sinceRef, CONFIG_DEF_REPO_PATH)
        if (historicalDefsText == null) {
            // Bootstrap case: the ref predates the .fleetControl/ system.
            // Treat as first release of the schema — no bump, no PR.
            println "configurationDefinitions.yml does not exist at ${sinceRef}."
            println "Treating this as the first release that includes the schema; no bump applied."
            System.exit(0)
        }

        def (String starterVersion, String historicalSchemaRel) = parseDefinitions(
                historicalDefsText, "configurationDefinitions.yml@${sinceRef}", false)
        if (!starterVersion || !historicalSchemaRel) {
            // Bootstrap: file existed at the ref but didn't have the fields we need.
            // Common when a placeholder configurationDefinitions.yml was committed
            // before the schema work was complete.
            println "configurationDefinitions.yml at ${sinceRef} is incomplete (missing version or schema field)."
            println "Treating this as the first release that includes the schema; no bump applied."
            System.exit(0)
        }
        println "Starter version (from ${sinceRef}): ${starterVersion}"

        // Step 2: fetch historical schema using its declared path
        String historicalSchemaPath = resolveSchemaRepoPath(CONFIG_DEF_REPO_PATH, historicalSchemaRel)
        String historicalSchemaText = gitShow(REPO_ROOT, sinceRef, historicalSchemaPath)
        if (historicalSchemaText == null) {
            // configurationDefinitions.yml existed but pointed at a schema file
            // that doesn't. Treat as bootstrap — same as missing definitions.
            println "Schema file at ${historicalSchemaPath} does not exist at ${sinceRef}."
            println "Treating this as the first release that includes the schema; no bump applied."
            System.exit(0)
        }
        Map<String, Object> historicalSchema = (Map<String, Object>) new JsonSlurper().parseText(historicalSchemaText)

        // Step 3: read current schema using its declared path (handles renames)
        if (!CONFIG_DEF_PATH.exists()) {
            throw new FileNotFoundException("Current configurationDefinitions.yml not found at ${CONFIG_DEF_PATH}")
        }
        def (String currentVersion, String currentSchemaRel) = parseDefinitions(
                CONFIG_DEF_PATH.text, "configurationDefinitions.yml (current)")
        File currentSchemaFile = new File(CONFIG_DEF_PATH.parentFile, currentSchemaRel).canonicalFile
        if (!currentSchemaFile.exists()) {
            throw new FileNotFoundException("Current schema not found at ${currentSchemaFile}")
        }
        Map<String, Object> currentSchema = (Map<String, Object>) new JsonSlurper().parse(currentSchemaFile)

        // Step 4: classify diff and recommend a bump
        List<Map<String, Object>> changes = lib.classifyChanges(historicalSchema, currentSchema)
        if (changes) {
            List<Map<String, Object>> breaking = changes.findAll { it.get('severity') == 'breaking' }
            List<Map<String, Object>> additive = changes.findAll { it.get('severity') == 'additive' }
            List<Map<String, Object>> cosmetic = changes.findAll { it.get('severity') == 'cosmetic' }
            println "\nSchema changes since ${sinceRef} (${changes.size()}):"
            if (breaking) {
                println "  BREAKING (${breaking.size()}):"
                breaking.each { Map<String, Object> ch -> println "    ${lib.renderChange(ch)}" }
            }
            if (additive) {
                println "  ADDITIVE (${additive.size()}):"
                additive.each { Map<String, Object> ch -> println "    ${lib.renderChange(ch)}" }
            }
            if (cosmetic) {
                println "  COSMETIC (${cosmetic.size()}):"
                cosmetic.each { Map<String, Object> ch -> println "    ${lib.renderChange(ch)}" }
            }
        } else {
            println "\nNo schema changes since ${sinceRef}."
        }

        String bumpKind = lib.recommendBump(changes)
        if (bumpKind == 'none') {
            println "\nRecommended bump: none — no version change needed."
            System.exit(0)
        }

        String newVersion = lib.applyBump(starterVersion, bumpKind)
        println "\nRecommended bump: ${bumpKind} (${starterVersion} → ${newVersion})"

        if (!ciMode) {
            println "(dry-run; pass --ci to apply)"
            System.exit(1)
        }

        // Apply the bump. Note: bumpVersion reads the version from the file,
        // applies the kind, and writes the result. Since the on-disk version
        // may have drifted from `starterVersion` (e.g., if a prior bump landed
        // but no release was cut), we explicitly write `newVersion` rather
        // than re-bumping the on-disk value.
        if (currentVersion != newVersion) {
            String text = CONFIG_DEF_PATH.text
            java.util.regex.Pattern versionLine = java.util.regex.Pattern.compile(/(?m)^(\s*version:\s*)(\S+)(\s*)$/)
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
                        "${CONFIG_DEF_PATH}: expected exactly 1 'version:' line, found ${matches}"
                )
            }
            CONFIG_DEF_PATH.text = sb.toString()
            println "Wrote: ${CONFIG_DEF_PATH} (version: ${currentVersion} → ${newVersion})"
        } else {
            println "On-disk version already matches the bumped value (${newVersion}); no write needed."
        }

        System.exit(1)
    } catch (Throwable t) {
        System.err.println "Bump failed: ${t.class.name}: ${t.message}"
        t.printStackTrace(System.err)
        System.exit(2)
    }
}