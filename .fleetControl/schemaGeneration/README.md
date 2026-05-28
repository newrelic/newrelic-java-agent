# Fleet Control Schema Generator

This directory contains the Groovy script that generates a JSON Schema from the Java Agent's `newrelic.yml` configuration file for use with Fleet Control.

## Quick Start

```bash
# Generate schema (from repo root)
groovy .fleetControl/schemaGeneration/generate-schema.groovy

# Generate schema and apply version bump (CI mode)
groovy .fleetControl/schemaGeneration/generate-schema.groovy --ci
```

## Files

| File | Description |
|------|-------------|
| `generate-schema.groovy` | Main generator script |
| `../schemas/config.json` | Generated JSON Schema (Draft 2020-12) |
| `../configurationDefinitions.yml` | Version metadata (bumped on schema changes) |

## How It Works

1. Reads `newrelic-agent/src/main/resources/newrelic.yml`
2. Parses YAML structure and extracts comments for descriptions
3. Applies type overrides and enum constraints
4. Validates against JSON Schema Draft 2020-12 meta-schema
5. Compares with existing schema to detect changes
6. Writes updated schema and optionally bumps version

## Adding New Configuration Keys

When new keys are added to `newrelic.yml`, the script will automatically detect and include them. However, **special handling is required for certain key types**.

### Array-or-String Keys (getUniqueStrings)

Many agent config keys accept either a YAML array OR a comma-delimited string:

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

These keys are parsed via `BaseConfig.getUniqueStrings()` in the agent code. For the JSON Schema to correctly represent this flexibility, they must be added to `TYPE_OVERRIDES` in the script:

```groovy
'new_feature.include': stringArrayOrDelimited(),
'new_feature.exclude': stringArrayOrDelimited(),
```

**Why is this necessary?**

The script infers types from YAML values:
- `include: []` → inferred as `array` (correct, but missing string option)
- `include:` (null) → inferred as `string` (incorrect)
- `include: "value"` → inferred as `string` (incorrect)

Adding to `TYPE_OVERRIDES` ensures the schema uses `anyOf` to accept both formats:

```json
{
  "anyOf": [
    { "type": "array", "items": { "type": "string" } },
    { "type": "string" }
  ]
}
```

### Status Code Keys

Keys that accept integers, arrays of integers, or range strings (e.g., `"400-499"`) should use:

```groovy
'error_collector.new_status_codes': statusCodeArrayOrRange([404]),
```

### Enum Keys

Keys with a fixed set of allowed values should be added to `ENUM_OVERRIDES`:

```groovy
'new_feature.mode': ['option1', 'option2', 'option3'],
```

## Excluding Keys

### Static Exclusions

Add keys to `EXCLUDE_KEYS` to completely exclude them from the schema:

```groovy
final Set<String> EXCLUDE_KEYS = [
    'internal_only_setting',
    'parent_key',  // Excludes parent_key and all children
]
```

### Pattern-Based Exclusions

Add regex patterns to `EXCLUDE_KEY_PATTERNS` for dynamic exclusions:

```groovy
final List<Pattern> EXCLUDE_KEY_PATTERNS = [
    // Exclude instrumentation module toggles with dotted names
    Pattern.compile(/^class_transformer\.[^.]+\..+/),
]
```

This is used to exclude instrumentation module keys like `class_transformer.com.newrelic.instrumentation.servlet-user` while keeping normal keys like `class_transformer.classloader_excludes`.

## Checklist for New Config Keys

When adding new configuration keys to `newrelic.yml`:

1. **Run the generator** to pick up the new key automatically
2. **Check the inferred type** in the generated schema
3. **If the key uses `getUniqueStrings()`** → Add to `TYPE_OVERRIDES` with `stringArrayOrDelimited()`
4. **If the key has enum values** → Add to `ENUM_OVERRIDES`
5. **If the key should be hidden** → Add to `EXCLUDE_KEYS` or `EXCLUDE_KEY_PATTERNS`
6. **Run the generator again** to apply overrides
7. **Verify the schema** looks correct

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | No schema changes (or first run, or `--force` mode) |
| 1 | Schema changed (CI should commit updated files) |
| 2 | Error (invalid arguments, meta-schema validation failed) |

## CLI Options

| Option | Description |
|--------|-------------|
| `--force` | Overwrite schema without comparing to existing; always exits 0 |
| `--ci` | Apply version bump to `configurationDefinitions.yml` |
| `--bump=major\|minor\|patch\|none` | Override automatic version bump |

## Version Bumping

The script automatically recommends a version bump based on change severity:

| Change Type | Severity | Bump |
|-------------|----------|------|
| Property removed | Breaking | Major |
| Type changed | Breaking | Major |
| Enum value removed | Breaking | Major |
| Required field added | Breaking | Major |
| Property added | Additive | Minor |
| Enum value added | Additive | Minor |
| Default changed | Additive | Minor |
| Description changed | Cosmetic | Patch |