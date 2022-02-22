package example;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "io.r2dbc.h2")
public class H2Test {

    public Connection connection;

    @Before
    public void setup() {
        ConnectionFactory connectionFactory = ConnectionFactories.get("r2dbc:h2:file:///~/example2");
        connection = Mono.from(connectionFactory.create()).block();
        connection.getMetadata().getDatabaseProductName();
        connection.getMetadata().getDatabaseVersion();
        System.out.println(connection.getMetadata().getDatabaseProductName());
        System.out.println(connection.getMetadata().getDatabaseVersion());
        Mono.from(connection.createStatement("CREATE TABLE IF NOT EXISTS CARS(id int primary key, first_name varchar(255), last_name varchar(255))").execute()).block();
        Mono.from(connection.createStatement("TRUNCATE TABLE CARS").execute()).block();
        Mono.from(connection.createStatement("INSERT INTO CARS(id, first_name, last_name) VALUES(1, 'Fakus', 'Namus')").execute()).block();
        Mono.from(connection.createStatement("INSERT INTO CARS(id, first_name, last_name) VALUES(2, 'Some', 'Guy')").execute()).block();
        Mono.from(connection.createStatement("INSERT INTO CARS(id, first_name, last_name) VALUES(3, 'Whatsher', 'Name')").execute()).block();
    }

    @Test
    public void testSelect() {
        //Given
        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        //When
        Result result = basicSelect();

        //Then
        assertEquals(1, introspector.getFinishedTransactionCount(1000));
        assertEquals(1, introspector.getTransactionNames().size());
    }


    @Trace(dispatcher = true)
    private Result basicSelect() {
        return Mono.from(connection.createStatement("SELECT * FROM CARS").execute()).block();
    }
}