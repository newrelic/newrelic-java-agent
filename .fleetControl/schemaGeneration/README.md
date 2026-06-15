# Agent Config Schema Generator

This directory contains the Groovy scripts that turn `reference-newrelic.yml`
into a JSON Schema (`../schemas/config.json`) and manage version bumps in
`../configurationDefinitions.yml` for Fleet Control.

## Files

| File | Description |
|------|-------------|
| `GenerateSchema.groovy` | Per-push regenerator. Reads `reference-newrelic.yml`, writes `config.json`. Never touches `configurationDefinitions.yml`. |
| `BumpSchemaVersion.groovy` | Release-time version bumper. Compares the schema at a prior git ref to the current schema and writes a new version into `configurationDefinitions.yml`. |
| `SchemaDiff.groovy` | Shared library (no main). Holds the diff classification (`classifyChanges`), bump arithmetic (`recommendBump`, `applyBump`, `bumpVersion`), and schema loading (`loadExisting`). Loaded by both top-level scripts via `GroovyShell.parse`. |
| `reference-newrelic.yml` | Source of truth for the schema. Mirrors the agent's `newrelic-agent/src/main/resources/newrelic.yml`. |
| `tests/GenerateSchemaTest.groovy` | Tests for the generator (inferType, makeProperty, buildProperties, generateSchema). |
| `tests/SchemaDiffTest.groovy` | Tests for the shared library (classifyChanges, recommendBump, applyBump, bumpVersion). |
| `../schemas/config.json` | Generated JSON Schema (Draft 2020-12). |
| `../configurationDefinitions.yml` | Fleet Control metadata, including the schema's semver version. Bumped only at release time. |

## How the generator works

`GenerateSchema.groovy`:

1. Reads `.fleetControl/schemaGeneration/reference-newrelic.yml`.
2. Parses YAML structure and extracts adjacent `#` comments as property descriptions.
3. Applies the `TYPE_OVERRIDES`, `ENUM_OVERRIDES`, `EXCLUDE_KEYS`, and `EXCLUDE_KEY_PATTERNS` configured in the script.
4. Validates the result against the JSON Schema Draft 2020-12 meta-schema.
5. Compares the new schema against the existing on-disk `config.json` and prints a summary of any changes.
6. Writes the regenerated `config.json` to disk.

The generator does **not** touch `configurationDefinitions.yml` — version
bumps live in a separate flow (next section).

## How versioning works

Schema regeneration runs **per push** on feature branches via
`.github/workflows/Agent-Config-Schema.yml`. It writes `config.json` and
nothing else. Reviewers see schema diffs in PRs.

Version bumps run **per release** via `.github/workflows/Agent-Config-Schema-Bump.yml`,
which fires on the success of `Test Suite - Release`. The bump workflow:

1. Finds the latest `v*` tag on `main`.
2. Reads the historical `configurationDefinitions.yml` from that tag — that
   version is the **starter version** for the bump.
3. Reads the historical schema using the path declared in that file's
   `schema:` field.
4. Compares the historical schema to the current `config.json` on `main`,
   classifies the cumulative diff, and applies the recommended bump kind
   (major/minor/patch).
5. Opens a PR titled `chore: bump agent config schema version` for team
   review. Branch protection blocks the bot from pushing to `main` directly.

If the latest release tag predates the schema (the `.fleetControl/`
directory or the `schema:` field in `configurationDefinitions.yml`),
`BumpSchemaVersion.groovy` exits 0 with a bootstrap message and no PR is
opened. The first release that includes the schema ships at whatever
version is currently in `configurationDefinitions.yml`.

### Release ordering — merge the bump PR before cutting the tag

The bump PR is a separate review/merge step from the agent's `vX.Y.Z`
release tag. The order matters:

- **If the bump PR merges to main BEFORE the release tag is cut:** the
  tag includes the bumped `configurationDefinitions.yml`. Consumers
  querying the schema version at that tag see the new value.
- **If the release tag is cut BEFORE the bump PR merges:** the tag's
  `configurationDefinitions.yml` still says the pre-bump version, even
  though the schema itself (`config.json`) at that tag reflects the new
  keys. Consumers see a mismatch. The next release will correctly compute
  its bump from this tag's metadata, but the tag itself ships
  mismatched.

**Recommended release sequence:**

1. Trigger `Test Suite - Release` (manually).
2. Wait for `Agent-Config-Schema-Bump` to open its PR (or report that
   no bump is needed).
3. Review and merge the bump PR if one was opened.
4. Cut the release tag from the post-merge main.

## Quick start

```bash
# Regenerate schema (from repo root)
groovy .fleetControl/schemaGeneration/GenerateSchema.groovy

# Force-regenerate without comparing
groovy .fleetControl/schemaGeneration/GenerateSchema.groovy --force

# Dry-run a release-time bump against a tag
groovy .fleetControl/schemaGeneration/BumpSchemaVersion.groovy --since=v9.3.0

# Apply a release-time bump (writes configurationDefinitions.yml)
groovy .fleetControl/schemaGeneration/BumpSchemaVersion.groovy --since=v9.3.0 --ci

# Or run the gradle task (regen only)
./gradlew :generateAgentConfigSchema
```

## Adding new configuration keys

When new keys are added to `reference-newrelic.yml`, the generator will
automatically detect and include them. **Special handling is required for
certain key types**, configured via override maps in `GenerateSchema.groovy`.

### Array-or-string keys (getUniqueStrings)

Many agent config keys accept either a YAML array OR a comma-delimited
string:

```yaml
# Both formats are equivalent:
attributes:
  include: [one, two, three]
```

or...

```yaml
attributes:
  include: one, two, three
```

These keys are parsed via `BaseConfig.getUniqueStrings()` in the agent
code. For the JSON Schema to correctly represent this flexibility, they
must be added to `TYPE_OVERRIDES` in `GenerateSchema.groovy`:

```groovy
'new_feature.include': stringArrayOrDelimited(),
'new_feature.exclude': stringArrayOrDelimited(),
```

**Why is this necessary?**

The script infers types from YAML values:
- `include: []` → inferred as `array` (correct, but missing string option)
- `include:` (null) → inferred as `string` (incorrect)
- `include: "value"` → inferred as `string` (incorrect)

Adding to `TYPE_OVERRIDES` ensures the schema uses `anyOf` to accept both
formats:

```json
{
  "anyOf": [
    { "type": "array", "items": { "type": "string" } },
    { "type": "string" }
  ]
}
```

### Status code keys

Keys that accept integers, arrays of integers, or range strings (e.g.,
`"400-499"`) should use:

```groovy
'error_collector.new_status_codes': statusCodeArrayOrRange([404]),
```

### Enum keys

Keys with a fixed set of allowed values should be added to `ENUM_OVERRIDES`:

```groovy
'new_feature.mode': ['option1', 'option2', 'option3'],
```

## Excluding keys

### Static exclusions

Add keys to `EXCLUDE_KEYS` to completely exclude them from the schema:

```groovy
final Set<String> EXCLUDE_KEYS = [
    'internal_only_setting',
    'parent_key',  // excludes parent_key and all children
]
```

### Pattern-based exclusions

Add regex patterns to `EXCLUDE_KEY_PATTERNS` for dynamic exclusions:

```groovy
final List<Pattern> EXCLUDE_KEY_PATTERNS = [
    // Exclude instrumentation module toggles with dotted names
    Pattern.compile(/^class_transformer\.[^.]+\..+/),
]
```

This is used to exclude instrumentation module keys like
`class_transformer.com.newrelic.instrumentation.servlet-user` while
keeping normal keys like `class_transformer.classloader_excludes`.

## Checklist for new config keys

When adding new configuration keys to `reference-newrelic.yml`:

1. **Run the generator** to pick up the new key automatically.
2. **Check the inferred type** in the generated schema.
3. **If the key uses `getUniqueStrings()`** → add to `TYPE_OVERRIDES` with
   `stringArrayOrDelimited()`.
4. **If the key has enum values** → add to `ENUM_OVERRIDES`.
5. **If the key should be hidden** → add to `EXCLUDE_KEYS` or
   `EXCLUDE_KEY_PATTERNS`.
6. **Run the generator again** to apply overrides.
7. **Verify the schema** looks correct.

The version doesn't bump on per-PR pushes. The next release will pick up
your changes when `Agent-Config-Schema-Bump.yml` fires after
`Test Suite - Release` succeeds.

## CLI options

### `GenerateSchema.groovy`

| Option | Description |
|--------|-------------|
| `--force` | Overwrite schema without comparing to existing; always exits 0. |

### `BumpSchemaVersion.groovy`

| Option | Description |
|--------|-------------|
| `--since=<ref>` | Required. Compare current schema to the schema at `<ref>` and recommend a bump. |
| `--ci` | Write the bumped version to `configurationDefinitions.yml`. Without this, the script just prints the recommendation. |

## Exit codes

### `GenerateSchema.groovy`

| Code | Meaning |
|------|---------|
| 0 | No schema changes (or `--force` mode). |
| 1 | Schema regenerated and on-disk differed (CI should commit). |
| 2 | Generator failure (uncaught exception). |

### `BumpSchemaVersion.groovy`

| Code | Meaning |
|------|---------|
| 0 | No bump needed (no schema diff, or bootstrap case where `<ref>` predates the schema). |
| 1 | Bump applied (`--ci`) or recommended (without `--ci`). |
| 2 | Bump failure (uncaught exception, missing args). |

## Version bumping rules

`BumpSchemaVersion.groovy` classifies each schema change and the bump
kind is the highest severity across all changes:

| Change type | Severity | Bump |
|-------------|----------|------|
| Property removed | Breaking | Major |
| Type changed | Breaking | Major |
| Enum value removed | Breaking | Major |
| Enum newly introduced | Breaking | Major |
| Required field added | Breaking | Major |
| `additionalProperties` tightened (true → false) | Breaking | Major |
| Property added | Additive | Minor |
| Enum value added | Additive | Minor |
| Enum removed entirely | Additive | Minor |
| Required field removed | Additive | Minor |
| Default changed | Additive | Minor |
| `additionalProperties` loosened (false → true) | Additive | Minor |
| Description changed | Cosmetic | Patch |

## Running the tests

```bash
# Schema-generation tests (inferType, makeProperty, buildProperties, generateSchema)
groovy .fleetControl/schemaGeneration/tests/GenerateSchemaTest.groovy

# Diff/bump tests (classifyChanges, recommendBump, applyBump, bumpVersion)
groovy .fleetControl/schemaGeneration/tests/SchemaDiffTest.groovy
```