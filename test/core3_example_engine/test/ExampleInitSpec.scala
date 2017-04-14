/**
  * Copyright 2017 Interel
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package core3_example_engine.test


import java.security.SecureRandom

import core3.config.StaticConfig
import core3.database.containers.core
import core3.database.containers.core.UserType
import core3.workflows.definitions
import org.scalatest.{Matchers, fixture}
import play.api.libs.json.Json

import scala.concurrent.Future

class ExampleInitSpec extends fixture.AsyncFlatSpec with Matchers {
  case class FixtureParam()
  def withFixture(test: OneArgAsyncTest) = {
    val fixture = FixtureParam()
    withFixture(test.toNoArgAsyncTest(fixture))
  }

  "A ExampleInitSpec" should "successfully initialize an example database" in {
    _ =>
      val adminWorkflows = Seq(
        definitions.SystemCreateGroup,
        definitions.SystemCreateLocalUser,
        definitions.SystemDeleteGroup,
        definitions.SystemDeleteLocalUser,
        definitions.SystemQueryGroups,
        definitions.SystemQueryLocalUsers,
        definitions.SystemQueryTransactionLogs,
        definitions.SystemUpdateGroup,
        definitions.SystemUpdateLocalUserMetadata,
        definitions.SystemUpdateLocalUserPassword,
        definitions.SystemUpdateLocalUserPermissions
      )

      val userWorkflows = Seq(
        definitions.SystemQueryGroups,
        definitions.SystemQueryLocalUsers,
        definitions.SystemQueryTransactionLogs
      )

      val db = core3.test.fixtures.Database.createRedisInstance()

      val config = StaticConfig.get.getConfig("testing.security.example")
      val random = new SecureRandom()

      val (hashedPasswordForExampleAdmin, saltForExampleAdmin) = core3.security.hashPassword("some-test-password!", config, random)
      val testExampleAdmin = core.LocalUser(
        "test-admin",
        hashedPasswordForExampleAdmin,
        saltForExampleAdmin,
        Seq("c3eu:view", "c3eu:edit", "c3eu:delete") ++ adminWorkflows.map(_.name),
        UserType.Client,
        Json.obj("first_name" -> "Test", "last_name" -> "Admin"),
        "test-user-0"
      )

      val (hashedPasswordForExampleUser, saltForExampleUser) = core3.security.hashPassword("some-test-password@", config, random)
      val testExampleUser = core.LocalUser(
        "test-user",
        hashedPasswordForExampleUser,
        saltForExampleUser,
        Seq("c3eu:view") ++ userWorkflows.map(_.name),
        UserType.Client,
        Json.obj("first_name" -> "Test", "last_name" -> "User"),
        "test-user-0"
      )

      val (hashedPasswordForExampleClient, saltForExampleClient) = core3.security.hashPassword("some-test-password#", config, random)
      val testExampleClient = core.LocalUser(
        "test-client",
        hashedPasswordForExampleClient,
        saltForExampleClient,
        Seq("exec:asUser", "exec:asClient"),
        UserType.Service,
        Json.obj("first_name" -> "Test", "last_name" -> "Client"),
        "test-user-0"
      )

      Future.sequence(
        Seq(
          db.clearDatabaseStructure("TransactionLog"),
          db.clearDatabaseStructure("LocalUser"),
          db.clearDatabaseStructure("Group"),
          db.buildDatabaseStructure("TransactionLog"),
          db.buildDatabaseStructure("LocalUser"),
          db.buildDatabaseStructure("Group"),
          db.verifyDatabaseStructure("TransactionLog"),
          db.verifyDatabaseStructure("LocalUser"),
          db.verifyDatabaseStructure("Group"),
          db.createObject(testExampleAdmin),
          db.createObject(testExampleUser),
          db.createObject(testExampleClient)
        )
      ).flatMap {
        _ =>
          for {
            dbExampleAdmin <- db.getObject("LocalUser", testExampleAdmin.id)
            dbExampleUser <- db.getObject("LocalUser", testExampleUser.id)
            dbExampleClient <- db.getObject("LocalUser", testExampleClient.id)
          } yield {
            dbExampleAdmin should be(testExampleAdmin)
            dbExampleUser should be(testExampleUser)
            dbExampleClient should be(testExampleClient)
          }
      }
  }
}
