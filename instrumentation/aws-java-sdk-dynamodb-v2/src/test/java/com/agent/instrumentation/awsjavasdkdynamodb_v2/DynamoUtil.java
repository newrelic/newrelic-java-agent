/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdkdynamodb_v2;

public class DynamoUtil {

//    public static Table createTable(DynamoDB dynamoDB, String tableName) {
//        Table table = null;
//        try {
//            // dynamodb is schema-less, only specify the primary / second keys and their types
//            table = dynamoDB.createTable(tableName,
//                    Arrays.asList(
//                            new KeySchemaElement("artist", KeyType.HASH), // primary key
//                            new KeySchemaElement("year", KeyType.RANGE)), // partition (sort) key
//                    Arrays.asList(
//                            new AttributeDefinition("artist", ScalarAttributeType.S),
//                            new AttributeDefinition("year", ScalarAttributeType.N)),
//                    new ProvisionedThroughput(3L, 3L));
//
//            table.waitForActive();
//        } catch (Exception e) {
//        }
//        return table;
//    }
//
//    public static CreateTableRequest createTableRequest(String tableName) {
//        // dynamodb is schema-less, only specify the primary / second keys and their types
//        CreateTableRequest request = new CreateTableRequest()
//                .withTableName(tableName)
//                .withKeySchema(Arrays.asList(
//                        new KeySchemaElement("artist", KeyType.HASH),
//                        new KeySchemaElement("year", KeyType.RANGE)))
//                .withAttributeDefinitions(
//                        new AttributeDefinition("artist", ScalarAttributeType.S),
//                        new AttributeDefinition("year", ScalarAttributeType.N))
//                .withProvisionedThroughput(new ProvisionedThroughput(3L, 3L));
//
//        return request;
//    }
//
//    public static class Album {
//        public String Artist;
//        public Integer Year;
//        public String Album;
//        public String Genre;
//
//        public Album(String artist, Integer year, String album, String genre) {
//            Artist = artist;
//            Year = year;
//            Album = album;
//            Genre = genre;
//        }
//    }
//
//    public static Item createItemFromAlbum(Album album) {
//        return new Item()
//                .withPrimaryKey("artist", album.Artist, "year", album.Year)
//                .withString("Album", album.Album)
//                .withString("Genre", album.Genre);
//    }
//
//    public static Item createDefaultItem() {
//        return createItemFromAlbum(new Album("Miles Davis", 1959, "Kind of Blue", "Jazz"));
//    }
//
//    public static Map<String, AttributeValue> createItemKey() {
//        Map<String, AttributeValue> key = new HashMap<>();
//        key.put("artist", new AttributeValue("Miles Davis"));
//        key.put("year", new AttributeValue().withN("1959"));
//        return key;
//    }

}
