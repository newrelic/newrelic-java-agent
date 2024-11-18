package com.nr.agent.instrumentation.r2dbc;

import java.util.List;

import org.reactivestreams.Publisher;

import com.newrelic.api.agent.Trace;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class R2dbcTestUtils {
    @Trace(dispatcher = true)
    public static void basicRequests(Connection connection) {
//        Mono.from(connection.createStatement("INSERT INTO USERS(id, first_name, last_name, age) VALUES(1, 'Max', 'Power', 30)").execute()).block();
//        Mono.from(connection.createStatement("SELECT * FROM USERS WHERE last_name='Power'").execute()).block();
//        Mono.from(connection.createStatement("UPDATE USERS SET age = 36 WHERE last_name = 'Power'").execute()).block();
//        Mono.from(connection.createStatement("SELECT * FROM USERS WHERE last_name='Power'").execute()).block();
//        Mono.from(connection.createStatement("DELETE FROM USERS WHERE last_name = 'Power'").execute()).block();
//        Mono.from(connection.createStatement("SELECT * FROM USERS").execute()).block();
		Statement stmt = connection.createStatement("INSERT INTO USERS(id, first_name, last_name, age) VALUES(1, 'Max', 'Power', 30)");
		Publisher<? extends Result> publish = stmt.execute();
		Integer i = Flux.from(publish).flatMap(it -> it.getRowsUpdated()).blockLast();
		System.out.println("Inserted " + i + " row(s)");
		publish = connection.createStatement("SELECT * FROM USERS WHERE last_name='Power'").execute();
		Mono<List<User>> users = Flux.from(publish).flatMap(it -> it.map((row,rowMetadata) -> 
		new User(row.get("first_name", String.class),row.get("last_name", String.class),row.get("age", Integer.class)))).collectList();
		List<User> userList = users.block();
		System.out.println("Select returned " + userList.size() + " users");
		int count = 1;
		for(User user : userList) {

			System.out.println("user " + count + ": " + user);
			count++;
		}

		publish = connection.createStatement("UPDATE USERS SET age = 36 WHERE last_name = 'Power'").execute();
		i = Flux.from(publish).flatMap(it -> it.getRowsUpdated()).blockLast();
		System.out.println("Updated ages of " + i + " users with last name Power");

		publish = connection.createStatement("SELECT * FROM USERS WHERE last_name='Power'").execute();
		users = Flux.from(publish).flatMap(it -> it.map((row,rowMetadata) -> 
		new User(row.get("first_name", String.class),row.get("last_name", String.class),row.get("age", Integer.class)))).collectList();
		userList = users.block();
		System.out.println("Select returned " + userList.size() + " users");
		count = 1;
		for(User user : userList) {

			System.out.println("user " + count + ": " + user);
			count++;
		}



		publish = connection.createStatement("DELETE FROM USERS WHERE last_name = 'Power'").execute();
		i = Flux.from(publish).flatMap(it -> it.getRowsUpdated()).blockLast();
		System.out.println("Deleted " + i + " users with last name Power");

		publish = connection.createStatement("SELECT * FROM USERS WHERE last_name='Power'").execute();
		users = Flux.from(publish).flatMap(it -> it.map((row,rowMetadata) -> 
		new User(row.get("first_name", String.class),row.get("last_name", String.class),row.get("age", Integer.class)))).collectList();
		userList = users.block();
		System.out.println("Select returned " + userList.size() + " users");
		count = 1;
		for(User user : userList) {

			System.out.println("user " + count + ": " + user);
			count++;
		}
    }
    
	public static class User {
		String first;
		String last;
		int age;

		public User(String fn, String ln, int a) {
			first = fn;
			last = ln;
			age = a;
		}

		public String toString() {
			return "Name: " + first + " " + last + ", age: " + age;
		}
	}

}
