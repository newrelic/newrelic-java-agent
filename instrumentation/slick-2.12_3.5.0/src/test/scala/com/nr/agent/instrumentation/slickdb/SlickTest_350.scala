/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.slickdb

import com.newrelic.agent.introspec.InstrumentationTestConfig
import com.newrelic.agent.introspec.InstrumentationTestRunner
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.api.agent.Trace;

import scala.language.postfixOps

import org.junit._
import org.junit.runner.RunWith;

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global;
import slick.jdbc.H2Profile.api._

// Copied from slick-3.0.0 module
@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("slick", "org.h2"))
class SlickTest_350 {
  import SlickTest_350.slickdb
  import SlickTest_350.users

  @Test
  def testCrud() {
    slickInsert();
    slickUpdate();
    slickDelete();
    Await.result(slickResult(), 20 seconds)
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    awaitFinishedTx(introspector, 4);
    val txnNames = introspector.getTransactionNames()
    txnNames.forEach(name => {
      val metrics = introspector.getMetricsForTransaction(name)
      Assert.assertTrue(metrics.containsKey("ORM/Slick/slickQuery"))
    })
  }

  @Test
  def testNoTxn(): Unit = {
    //Await.result(runConcurrentQueries, 10.seconds)
    try {
      Await.result(runConcurrentQueries, 10.seconds)
    } catch {
      case _: Throwable => Assert.fail("Futures timed out running concurrent queries.")
    }

  }


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

  def testQuery(id: Int) = {
    users.filter(_.id === id)
  }

  def runConcurrentQueries = Future.traverse(1 to 50) { x =>
    val whichId = (x % 3) + 1
    slickdb.run(testQuery(whichId).result).map { v => println(s"Query Result $x: " + v) }
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

object SlickTest_350 {
  val DB_DRIVER: String = "org.h2.Driver";
  val DB_CONNECTION: String = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

  val slickdb = Database.forURL(DB_CONNECTION, DB_DRIVER)
  val users = TableQuery[Users]

  @BeforeClass
  def setup() {
    // set up h2
    Assert.assertNotNull("Unable to create h2 db.", slickdb)
    Assert.assertNotNull("Unable to create user table.", users)
    Await.result(initData(), 10.seconds) //make sure we don't enter the test suite until the init task finishes
  }

  @AfterClass
  def teardown() {
    // tear down h2
    if (null != slickdb) {
      slickdb.close();
    }
  }

  def initData() = {
    val setup = DBIO.seq(
      // Create and populate the tables
      users.schema.create,
      users ++= Seq(
        (1, "Fakus", "Namus"),
        (2, "Some", "Guy"),
        (3, "Whatser", "Name")
      ))
    slickdb.run(setup)
  }

}
