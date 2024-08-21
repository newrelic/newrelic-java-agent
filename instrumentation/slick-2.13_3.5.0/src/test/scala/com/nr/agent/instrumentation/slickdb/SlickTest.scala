/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.slickdb

import com.newrelic.agent.introspec.DatastoreHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.MetricsHelper;
import com.newrelic.agent.introspec.TransactionEvent;
import com.newrelic.api.agent.Trace;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit._
import org.junit.runner.RunWith;

import com.typesafe.config.ConfigFactory;

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global;
//import slick.driver.H2Driver.api._

//import collection.JavaConversions._

// Copied from slick-3.0.0 module
@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("slick", "org.h2"))
class SlickTest {

  @Before
  def initData() {
    // set up data in h2
    val stmt :Statement = SlickTest.CONNECTION.createStatement();
    stmt.execute("CREATE TABLE IF NOT EXISTS USER(id int primary key, first_name varchar(255), last_name varchar(255))");
    stmt.execute("TRUNCATE TABLE USER");
    stmt.execute("INSERT INTO USER(id, first_name, last_name) VALUES(1, 'Fakus', 'Namus')");
    stmt.execute("INSERT INTO USER(id, first_name, last_name) VALUES(2, 'Some', 'Guy')");
    stmt.execute("INSERT INTO USER(id, first_name, last_name) VALUES(3, 'Whatsher', 'Name')");
    stmt.close();
  }

  /*
   * Our slick tests rely on h2 jdbc to assert correctly. If this test fails nothing else will work.
   */
  @Test
  @Ignore
  def testJdbc() {
    jdbcTx()
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val helper :DatastoreHelper = new DatastoreHelper("JDBC");
    helper.assertAggregateMetrics();
    helper.assertUnscopedOperationMetricCount("select", 1);
  }

  @Trace(dispatcher = true)
  def jdbcTx() :Unit = {
    val stmt :Statement = SlickTest.CONNECTION.createStatement();
    stmt.execute("select * from USER");
    stmt.close()
  }

  @Test
  @Ignore
  def testResult() {
    // it would be cool to use asserts in a callback instead of awaiting
    // but that would keep the transaction from finishing
    Await.ready(slickResult(), 20 seconds)
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    awaitFinishedTx(introspector);
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val helper :DatastoreHelper = new DatastoreHelper("JDBC");
    helper.assertAggregateMetrics();
    helper.assertUnscopedOperationMetricCount("select", 1);
  }

  @Test
  @Ignore
  def testCrud() {
    slickInsert();
    slickUpdate();
    slickDelete();
    val res :String = Await.result(slickResult(), 20 seconds)
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    awaitFinishedTx(introspector, 4);
    Assert.assertEquals(4, introspector.getFinishedTransactionCount())

    val helper :DatastoreHelper = new DatastoreHelper("JDBC");
    helper.assertAggregateMetrics();
    helper.assertUnscopedOperationMetricCount("insert", 1); // C
    helper.assertUnscopedOperationMetricCount("select", 1); // R
    helper.assertUnscopedOperationMetricCount("update", 1); // U
    helper.assertUnscopedOperationMetricCount("delete", 1); // D
  }

  val slickdb = Database.forURL(SlickTest.DB_CONNECTION, driver=SlickTest.DB_DRIVER)
  val users = TableQuery[Users]

  @Trace(dispatcher = true)
  def slickResult() :Future[String] = {
    slickdb.run(users.result).map(units => {
      var res :String = ""
      units.foreach {
        case (id, first_name, last_name) =>
          res += " * " + id + ": " + first_name + " " + last_name + "\n"
      }
      "Got results: \n"+res
    })
  }

  @Trace(dispatcher = true)
  def slickInsert() :Future[String] = {
    slickdb.run(users.map(u => (u.id, u.first_name, u.last_name)) += (4, "John", "JacobJingle")).map(rowsInserted => {
      "Table now has "+rowsInserted+" users"
    })
  }

  @Trace(dispatcher = true)
  def slickUpdate() :Future[String] = {
    slickdb.run(users.filter(_.id === 1).map(u => (u.first_name)).update(("Fred"))).map(result => {
      "result: "+result
    })
  }

  @Trace(dispatcher = true)
  def slickDelete() :Future[String] = {
    // people.filter(p => p.name === "M. Odersky").delete
    slickdb.run(users.filter(_.id === 2).delete).map(result => {
      "result: "+result
    })
  }

  // introspector does not handle async tx finishing very well so we're sleeping as a workaround
  private def awaitFinishedTx(introspector :Introspector, expectedTxCount: Int = 1) {
    while(introspector.getFinishedTransactionCount() <= expectedTxCount-1) {
      Thread.sleep(100)
    }
    Thread.sleep(100)
  }

}

class Users(tag: Tag) extends Table[(Int, String, String)] (tag, "user") {
  def id = column[Int]("id", O.PrimaryKey)
  def first_name = column[String]("first_name")
  def last_name = column[String]("last_name")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (id, first_name, last_name)
}

object SlickTest {
  val DB_DRIVER     :String = "org.h2.Driver";
  val DB_CONNECTION :String = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
  val DB_USER       :String = "";
  val DB_PASSWORD   :String = "";
  val CONNECTION    :Connection = getDBConnection();

  @BeforeClass
  def setup() {
    // set up h2
    Assert.assertNotNull("Unable to get h2 connection.", CONNECTION)
    CONNECTION.setAutoCommit(true);
  }

  @AfterClass
  def teardown() {
    // tear down h2
    if(null != CONNECTION) {
      CONNECTION.close();
    }
  }

  def getDBConnection() :Connection = {
    var dbConnection :Connection = null
    try {
      Class.forName(DB_DRIVER);
      dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
      return dbConnection;
    } catch {
      case e :Exception => {
        e.printStackTrace();
      };
    }
    return dbConnection;
  }
}
