/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cassandra;

import static org.junit.Assert.*;

import org.junit.Test;

public class CQLParserTest {

    private static final CQLParser parser = new CQLParser();

    @Test
    public void testSelectParsing() {
        validateResult("SELECT * FROM users;", "SELECT", "users");
        validateResult("SELECT firstname, lastname FROM users WHERE birth_year = 1981 AND country = 'FR' ALLOW FILTERING;", "SELECT", "users");
        validateResult("select name, occupation FROM 'users' WHERE userid IN (199, 200, 207);", "SELECT", "users");
        validateResult("SELECT JSON name, occupation FROM users WHERE userid = 199;", "SELECT_JSON", "users");
        validateResult("SELECT name AS user_name, occupation AS user_occupation FROM users;", "SELECT", "users");
        validateResult("select time, value\n" + "        FROM events\n" + "        WHERE event_type = 'myEvent'\n"
                + "        AND time > '2011-02-03'\n" + "        AND time <= '2012-01-01'", "SELECT", "events");
        validateResult("SELECT COUNT(*) FROM \"users\";", "SELECT", "users");
        validateResult("SELECT COUNT(*) AS user_count FROM users;", "SELECT", "users");
        validateResult("SELECT entry_title, content FROM posts WHERE userid='john doe' AND blog_title='John''s Blog' AND posted_at >= '2012-01-01' AND posted_at < '2012-01-31'", "SELECT", "posts");
        validateResult("select * FROM posts WHERE userid='john doe' AND (blog_title, posted_at) IN (('John''s Blog', '2012-01-01), ('Extreme Chess', '2014-06-01'))", "SELECT", "posts");
        validateResult("SELECT Name, Occupation FROM People WHERE key IN (199, 200, 207);", "SELECT", "People");
        validateResult("SELECT FIRST 3 REVERSED 'time199'..'time100' FROM Events;", "SELECT", "Events");
        validateResult("select COUNT(*) FROM \"system.Migrations\";", "SELECT", "system.Migrations");
    }

    @Test
    public void testUpdateParsing() {
        validateResult("update  \"NerdMovies\" USING TTL 400\n"
                + "        SET director = 'Joss Whedon',\n"
                + "                main_actor = 'Nathan Fillion',\n"
                + "                year = 2005\n"
                + "        WHERE movie = 'Serenity';", "UPDATE", "NerdMovies");
        validateResult("UPDATE 'UserActions' SET total = total + 2 WHERE user = B70DE1D0-9908-4AE3-BE34-5573E5B09F14 AND action = 'click';", "UPDATE", "UserActions");
        validateResult("uPdaTe UserActionCounts SET total = total + 2 WHERE keyalias = 523;", "UPDATE", "UserActionCounts");
        validateResult("UPDATE \"counterks.page_view_counts\"\n" + "  SET counter_value = counter_value + 2\n"
                + "  WHERE url_name='www.datastax.com' AND page_name='home';", "UPDATE", "counterks.page_view_counts");
    }

    @Test
    public void testInsertParsing() {
        validateResult("INSERT INTO NerdMovies (KEY, 11924)\n"
                + "                VALUES ('Serenity', 'Nathan Fillion')\n"
                + "      USING CONSISTENCY LOCAL_QUORUM AND TTL 86400;", "INSERT", "NerdMovies");
        validateResult("INSERT INTO some.table (KEY, 1) VALUES ('Whatever');", "INSERT", "some.table");
        validateResult("INSERT INTO \"some.table\" (KEY, 1) VALUES ('Whatever');", "INSERT", "some.table");
        validateResult("insert INTO 'some.table' (KEY, 1) VALUES ('Whatever');", "INSERT", "some.table");
        validateResult("INSERT INTO cycling.cyclist_name (id,lastname,firstname)\n"
                + "  VALUES('c4b65263-fe58-83e8-f0e1c13d518f', 'RATTO', 'Risselda') IF NOT EXISTS;", "INSERT", "cycling.cyclist_name");
        validateResult("/* This is an INSERT query. yay*/ INSERT INTO users (lastname, age, city, email, firstname) VALUES ('Jones', 35, 'Austin', 'bob@example.com', 'Bob')", "INSERT", "users");
    }
    
    @Test
    public void testDeleteParsing() {
        validateResult("DELETE col1, col2, col3 FROM Planeteers USING CONSISTENCY ONE WHERE KEY = 'Captain';", "DELETE", "Planeteers");
        validateResult("DELETE FROM MastersOfTheUniverse WHERE KEY IN ('Man-At-Arms', 'Teela');", "DELETE", "MastersOfTheUniverse");
        validateResult("DELETE FROM 'MastersOfTheUniverse' WHERE KEY IN ('Man-At-Arms', 'Teela');", "DELETE", "MastersOfTheUniverse");
        validateResult("delete FROM \"MastersOfTheUniverse\" WHERE KEY IN ('Man-At-Arms', 'Teela');", "DELETE", "MastersOfTheUniverse");
        validateResult("deleTE FROM cycling.cyclist_name WHERE firstname IN ('Alex', 'Marianne');", "DELETE", "cycling.cyclist_name");
        validateResult("DELETE id FROM cyclist_id WHERE lastname = 'WELTEN' and firstname = 'Bram' IF EXISTS;", "DELETE", "cyclist_id");
        validateResult("DELETE firstname, lastname\n" + "  FROM cycling.cyclist_name\n"
                + "  USING TIMESTAMP 1318452291034\n" + "  WHERE lastname = 'VOS';", "DELETE", "cycling.cyclist_name");
    }

    @Test
    public void testBatchParsing() {
        validateResult("BEGIN BATCH USING CONSISTENCY QUORUM AND TTL 8640000\n"
                + "  INSERT INTO users (KEY, password, name) VALUES ('user2', 'ch@ngem3b', 'second user')\n"
                + "  UPDATE users SET password = 'ps22dhds' WHERE KEY = 'user2'\n"
                + "  INSERT INTO users (KEY, password) VALUES ('user3', 'ch@ngem3c')\n"
                + "  DELETE name FROM users WHERE key = 'user2'\n"
                + "  INSERT INTO users (KEY, password, name) VALUES ('user4', 'ch@ngem3c', 'Andrew')\n" + "APPLY BATCH;", "BATCH", null);
        validateResult("BEGIN UNLOGGED BATCH UPDATE users SET test = 'ok' APPLY BATCH;", "BATCH", null);
        validateResult("BEGIN COUNTER  BATCH UPDATE users SET test = 'ok' APPLY BATCH;", "BATCH", null);
        validateResult("BEGIN COUNTER  batCH UPDATE \"users\" SET test = 'ok' APPLY BATCH;", "BATCH", null);
        validateResult("BEGIN COUNTER  batCH UPDATE 'users' SET test = 'ok' APPLY BATCH;", "BATCH", null);
    }

    @Test
    public void testTableColumnFamilyParsing() {
        validateResult("CREATE COLUMNFAMILY Fish (KEY blob PRIMARY KEY);", "CREATE_COLUMNFAMILY", "Fish");
        validateResult("CREATE columnfamilY FastFoodEatings (user text PRIMARY KEY)\n"
                + "        WITH comparator=timestamp AND default_validation=int;", "CREATE_COLUMNFAMILY", "FastFoodEatings");
        validateResult("CREATE COLUMNFAMILY MonkeyTypes (\n" + "                KEY uuid PRIMARY KEY,\n"
                + "                species text,\n" + "                alias text,\n"
                + "                population varint\n" + "        ) WITH comment='Important biological records'\n"
                + "        AND read_repair_chance = 1.0;", "CREATE_COLUMNFAMILY", "MonkeyTypes");
        validateResult("CREATE COLUMNFAMILY 'Fish' (KEY blob PRIMARY KEY);", "CREATE_COLUMNFAMILY", "Fish");
        validateResult("CREATE COLUMNFAMILY \"Beer\" (KEY blob PRIMARY KEY);", "CREATE_COLUMNFAMILY", "Beer");
        validateResult("CREATE TABLE timeseries (\n" + "  event_type text,\n" + "  insertion_time timestamp,\n"
                + "  event blob,\n" + "  PRIMARY KEY (event_type, insertion_time)\n" + ")\n"
                + "WITH CLUSTERING ORDER BY (insertion_time DESC);", "CREATE_TABLE", "timeseries");
        validateResult("CREATE table if NOT exists 'Fish' (KEY blob PRIMARY KEY);", "CREATE_TABLE", "Fish");
        validateResult("DROP COLUMNFAMILY Beer", "DROP_COLUMNFAMILY", "Beer");
        validateResult("DROP COLUMNFAMILY \'Beer\'", "DROP_COLUMNFAMILY", "Beer");
        validateResult("DROP Table IF EXISTS \"Beer\"", "DROP_TABLE", "Beer");
        validateResult("DROP Table \"Stuff\"", "DROP_TABLE", "Stuff");
        validateResult("ALTER COLUMNFAMILY addamsFamily ALTER lastKnownLocation TYPE uuid;", "ALTER_COLUMNFAMILY", "addamsFamily");
        validateResult("ALTER COLUMNFAMILY \'addamsFamily\' ADD gravesite varchar;", "ALTER_COLUMNFAMILY", "addamsFamily");
        validateResult("ALTER TABLE mykeyspace.mytable \n"
                + "  WITH compaction = {'class': 'SizeTieredCompactionStrategy', 'cold_reads_to_omit': 0.05};", "ALTER_TABLE", "mykeyspace.mytable");
    }

    @Test
    public void testKeyspaceParsing() {
        validateResult("CREATE KEYSPACE Excelsior\n"
                + "           WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};", "CREATE_KEYSPACE", "Excelsior");
        validateResult("CREATE KEYSPACE IF NOT EXISTS my_keyspace;", "CREATE_KEYSPACE", "my_keyspace");
        validateResult("CREATE KEYSPACE IF NOT EXISTS \"my_keyspace\";", "CREATE_KEYSPACE", "my_keyspace");
        validateResult("ALTER KEYSPACE \'cool\';", "ALTER_KEYSPACE", "cool");
        validateResult("DROP KEYSPACE myApp;", "DROP_KEYSPACE", "myApp");
        validateResult("DROP KEYSPACE IF EXISTS myApp;", "DROP_KEYSPACE", "myApp");
    }

    @Test
    public void testUseParsing() {
        validateResult("USE testKeyspace", "USE", "testKeyspace");
        validateResult("UsE \"something\"", "USE", "something");
        validateResult("use 'somethingelse'", "USE", "somethingelse");
    }

    @Test
    public void testTruncateParsing() {
        validateResult("TRUNCATE superImportantData;", "TRUNCATE", "superImportantData");
        validateResult("TRUNCATE TABLE superImportantData;", "TRUNCATE", "superImportantData");
        validateResult("TRUNCATE COLUMNFAMILY superImportantData;", "TRUNCATE", "superImportantData");
        validateResult("TRUNCATE 'superImportantData';", "TRUNCATE", "superImportantData");
        validateResult("TRUNCATE TABLE \"superImportantData\";", "TRUNCATE", "superImportantData");
        validateResult("TRUNCATE 'mykeyspace.mytable'", "TRUNCATE", "mykeyspace.mytable");
    }

    @Test
    public void testTypeParsing() {
        validateResult("CREATE TYPE address (\n" + "                street_name text,\n"
                + "                street_number int,\n" + "        city text,\n" + "        state text,\n"
                + "        zip int\n" + "        )", "CREATE_TYPE", "address");
        validateResult("CREATE TYPE work_and_home_addresses (\n" + "                home_address address,\n"
                + "                work_address address\n" + "        )", "CREATE_TYPE", "work_and_home_addresses");
        validateResult("ALTER TYPE address ALTER zip TYPE varint", "ALTER_TYPE", "address");
        validateResult("CREATE TYPE IF not Exists coolType (field_one text);", "CREATE_TYPE", "coolType");
        validateResult("CREATE TYPE IF not Exists \"coolType\" (field_one text);", "CREATE_TYPE", "coolType");
        validateResult("DROP TYPE IF Exists 'coolType'", "DROP_TYPE", "coolType");
        validateResult("DROP TYPE myType", "DROP_TYPE", "myType");
    }

    @Test
    public void testCreateIndexParsing() {
        validateResult("CREATE INDEX userIndex ON NerdMovies (user);", "CREATE_INDEX", "NerdMovies");
        validateResult("CREATE INDEX ON Mutants (abilityId);", "CREATE_INDEX", "Mutants");
        validateResult("CREATE INDEX ON 'users' (keys(favs));", "CREATE_INDEX", "users");
        validateResult("CREATE cuSTOM INDEX ON users (email) USING 'path.to.the.IndexClass';", "CREATE_CUSTOM_INDEX", "users");
        validateResult("CREATE custom INDEX if NOT exists ON users (email) USING 'path.to.the.IndexClass';", "CREATE_CUSTOM_INDEX", "users");
        validateResult("CREATE INDEX if NOT exists userIndex ON users (email) USING 'path.to.the.IndexClass';", "CREATE_INDEX", "users");
        validateResult("CREATE INDEX ON \"mykeyspace.users\" (state);", "CREATE_INDEX", "mykeyspace.users");
    }

    @Test
    public void testDropIndexParsing() {
        validateResult("DROP INDEX cf_col_idx;", "DROP_INDEX", "cf_col_idx");
        validateResult("DROP INDEX userkeyspace.address_index;", "DROP_INDEX", "userkeyspace.address_index");
        validateResult("DROP INDEX if exists 'userkeyspace.address_index';", "DROP_INDEX", "userkeyspace.address_index");
        validateResult("DROP INDEX if EXISTS \"address_index;\"", "DROP_INDEX", "address_index");
    }

    @Test
    public void testCreateFunctionParsing() {
        validateResult("CREATE OR REPLACE FUNCTION somefunction\n"
                + "    ( somearg int, anotherarg text, complexarg frozen<someUDT>, listarg list<bigint> )\n"
                + "    RETURNS NULL ON NULL INPUT\n" + "    RETURNS text\n" + "    LANGUAGE java\n" + "    AS $$\n"
                + "       // some Java code\n" + "    $$;", "CREATE_OR_REPLACE_FUNCTION", "somefunction");
        validateResult("CREATE FUNCTION akeyspace.fname IF NOT EXISTS\n" + "    ( someArg int )\n"
                + "    CALLED ON NULL INPUT\n" + "    RETURNS text\n" + "    LANGUAGE java\n" + "    AS $$\n"
                + "       // some Java code\n" + "    $$;", "CREATE_FUNCTION", "akeyspace.fname");
        validateResult("CREATE FUNCTION 'akeyspace.fname' IF NOT EXISTS\n" + "    ( someArg int )\n"
                + "    CALLED ON NULL INPUT\n" + "    RETURNS text\n" + "    LANGUAGE java\n" + "    AS $$\n"
                + "       // some Java code\n" + "    $$;", "CREATE_FUNCTION", "akeyspace.fname");
    }

    @Test
    public void testDropFunctionParsing() {
        validateResult("DROP FUNCTION myfunction;", "DROP_FUNCTION", "myfunction");
        validateResult("DROP FUNCTION mykeyspace.afunction;", "DROP_FUNCTION", "mykeyspace.afunction");
        validateResult("DROP FUNCTION afunction ( text );", "DROP_FUNCTION", "afunction");
        validateResult("DROP FUNCTION IF EXISTS 'afunction' ( text );", "DROP_FUNCTION", "afunction");
        validateResult("DROP FUNCTION IF EXISTS \"keyspace.afunction\" ( text );", "DROP_FUNCTION", "keyspace.afunction");
    }

    @Test
    public void testCreateAggregateParsing() {
        validateResult("CREATE AGGREGATE myaggregate ( val text )\n" + "  SFUNC myaggregate_state\n" + "  STYPE text\n"
                + "  FINALFUNC myaggregate_final\n" + "  INITCOND 'foo';", "CREATE_AGGREGATE", "myaggregate");
        validateResult("CREATE OR replace AGGREGATE IF NOT EXISTS 'keyspace.aggregatename;", "CREATE_OR_REPLACE_AGGREGATE", "keyspace.aggregatename");
        validateResult("CREATE AGGREGATE IF NOT EXISTS \"keyspace.aggregatename\" SFUNC something_state;", "CREATE_AGGREGATE", "keyspace.aggregatename");
    }

    @Test
    public void testDropAggregateParsing() {
        validateResult("DROP AGGREGATE myAggregate;", "DROP_AGGREGATE", "myAggregate");
        validateResult("DROP AGGREGATE IF EXISTS 'myKeyspace.anAggregate';", "DROP_AGGREGATE", "myKeyspace.anAggregate");
        validateResult("DROP AGGREGATE IF EXISTS \"someAggregate\" ( int );", "DROP_AGGREGATE", "someAggregate");
        validateResult("DROP AGGREGATE someAggregate ( text );", "DROP_AGGREGATE", "someAggregate");
    }

    @Test
    public void testTriggerParsing() {
        validateResult("CREATE TRIGGER myTrigger ON myTable USING 'org.apache.cassandra.triggers.InvertedIndex';", "CREATE_TRIGGER", "myTable");
        validateResult("CREATE TRIGGER IF NOT exists 'myTrigger' ON myTable USING 'org.apache.cassandra.triggers.InvertedIndex';", "CREATE_TRIGGER", "myTable");
        validateResult("CREATE TRIGGER ON coolTable USING 'test';", "CREATE_TRIGGER", "coolTable");
        validateResult("CREATE TRIGGER IF NOT EXISTS ON coolTable USING 'test';", "CREATE_TRIGGER", "coolTable");
        validateResult("DROP TRIGGER myTrigger ON myTable;", "DROP_TRIGGER", "myTable");
        validateResult("DROP TRIGGER IF EXISTS myTrigger ON 'myTable';", "DROP_TRIGGER", "myTable");
        validateResult("DROP TRIGGER ON someTable;", "DROP_TRIGGER", "someTable");
        validateResult("DROP TRIGGER IF EXISTS ON \"somekeyspace.someTable\";", "DROP_TRIGGER", "somekeyspace.someTable");
    }

    private void validateResult(String rawQuery, String expectedOperation, String expectedTableName) {
        CQLParser.OperationAndTableName operationAndTableName = parser.getOperationAndTableName(rawQuery);
        assertNotNull(operationAndTableName);
        assertEquals(expectedOperation, operationAndTableName.operation);
        if (expectedTableName == null) {
            assertNull(operationAndTableName.tableName);
        } else {
            assertEquals(expectedTableName, operationAndTableName.tableName);
        }
    }
}
