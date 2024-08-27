# flyway-core-8.0.0

This instrumentation module will migration report events to New Relic during Flyway migrations initiated via code, including when running
properly configured Spring/Spring Boot applications.

The following Flyway migration events are reported:
- Event.AFTER_EACH_MIGRATE
- Event.AFTER_EACH_MIGRATE_ERROR
- Event.AFTER_EACH_UNDO
- Event.AFTER_EACH_UNDO_ERROR

The custom event type name is `FlywayMigration`, with the following attributes:
- migrationSuccess: `true` if the migration was successful
- migrationFilePhysicalLocation: Path/filename of the migration script
- migrationChecksum: Calculated checksum of the migration
- migrationVersion: Version portion of the migration script filename
- migrationScriptName: Name portion of the migration script filename
- migrationEvent: The underlying Flyway migration event