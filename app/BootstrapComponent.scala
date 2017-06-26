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
import java.security.SecureRandom

import akka.actor.{ActorRef, Props}
import com.typesafe.config.Config
import core3.core.Component.{ActionDescriptor, ActionResult}
import core3.core.{Component, ComponentCompanion}
import core3.database.containers.core.LocalUser
import core3.database.containers.core.LocalUser.UserType
import core3.database.dals.DatabaseAbstractionLayer
import core3.workflows.WorkflowBase
import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * A component that allows the DB to be initialized from the CLI.
  *
  * @param db the DB to use
  * @param authConfig the auth config to be used for user credentials setup
  * @param workflows the list of configured workflows
  */
class BootstrapComponent(
  db: DatabaseAbstractionLayer,
  authConfig: Config,
  workflows: Vector[WorkflowBase]
)(implicit ec: ExecutionContext) extends Component {
  private val random = new SecureRandom()
  private val auditLogger = Logger("audit")

  //stats
  private var count_ExecuteAction: Long = 0
  private var count_Bootstrap: Long = 0

  override protected def shutdown(): Unit = {}

  override protected def handle_ExecuteAction(
    action: String,
    params: Option[Map[String, Option[String]]]
  ): Future[ActionResult] = {
    count_ExecuteAction += 1

    action.toLowerCase match {
      case "stats" =>
        Future.successful(
          ActionResult(
            wasSuccessful = true,
            message = None,
            data = Some(
              Json.obj(
                "counters" -> Json.obj(
                  "executeAction" -> count_ExecuteAction,
                  "bootstrap" -> count_Bootstrap
                )
              )
            )
          )
        )

      case "create" =>
        count_Bootstrap += 1

        params match {
          case Some(actualParams) =>
            (actualParams.get("username").flatten, actualParams.get("password").flatten, actualParams.get("type").flatten) match {
              case (Some(username), Some(password), Some(userTypeStr)) =>
                (for {
                  user <- Future {
                    val (hashedPassword, passwordSalt) = core3.security.hashPassword(password, authConfig, random)
                    val userType = UserType.fromString(userTypeStr)
                    val extraPermissions = userType match {
                      case UserType.Client => Vector("c3eu:view", "c3eu:edit", "c3eu:delete")
                      case UserType.Service => Vector("exec:asUser", "exec:asClient")
                    }

                    LocalUser(
                      username,
                      hashedPassword,
                      passwordSalt,
                      workflows.map(_.name) ++ extraPermissions,
                      userType,
                      metadata = Json.obj("first_name" -> username, "last_name" -> ""),
                      "bootstrap"
                    )
                  }
                  result <- db.createObject(user)
                } yield {
                  val message = s"Bootstrap::create > Object [$user] created."
                  auditLogger.info(message)
                  ActionResult(wasSuccessful = result, message = Some(message))
                }).recover {
                  case NonFatal(e) =>
                    val message = s"Bootstrap::create > Exception encountered while initializing the database: [${e.getMessage}]."
                    auditLogger.error(message)
                    ActionResult(wasSuccessful = false, message = Some(message))
                }

              case _ =>
                Future.successful(ActionResult(wasSuccessful = false, message = Some(s"One or more parameters are missing")))
            }

          case None => Future.successful(ActionResult(wasSuccessful = false, message = Some(s"No parameters supplied")))
        }

      case "reset" =>
        count_Bootstrap += 1

        params match {
          case Some(actualParams) =>
            (actualParams.get("username").flatten, actualParams.get("password").flatten) match {
              case (Some(username), Some(password)) =>
                (for {
                  user <- db.queryDatabase("LocalUser", "getByUserID", Map("userID" -> username)).map(_.head.asInstanceOf[LocalUser])
                  _ <- Future {
                    val (hashedPassword, passwordSalt) = core3.security.hashPassword(password, authConfig, random)

                    user.hashedPassword = hashedPassword
                    user.passwordSalt = passwordSalt
                  }
                  result <- db.updateObject(user)
                } yield {
                  val message = s"Bootstrap::reset > Object [$user] updated."
                  auditLogger.info(message)
                  ActionResult(wasSuccessful = result, message = Some(message))
                }).recover {
                  case NonFatal(e) =>
                    val message = s"Bootstrap::reset > Exception encountered while initializing the database: [${e.getMessage}]."
                    auditLogger.error(message)
                    ActionResult(wasSuccessful = false, message = Some(message))
                }

              case _ =>
                Future.successful(ActionResult(wasSuccessful = false, message = Some(s"One or more parameters are missing")))
            }

          case None => Future.successful(ActionResult(wasSuccessful = false, message = Some(s"No parameters supplied")))
        }
    }
  }
}

object BootstrapComponent extends ComponentCompanion {
  def props(db: DatabaseAbstractionLayer, authConfig: Config, workflows: Seq[WorkflowBase])(implicit ec: ExecutionContext): Props = Props(
    classOf[BootstrapComponent], db, authConfig, workflows, ec
  )

  override def getActionDescriptors: Vector[ActionDescriptor] = {
    Vector(
      ActionDescriptor("stats", "Retrieves the latest component stats", arguments = None),
      ActionDescriptor(
        "create",
        "Creates a user with the supplied parameters and FULL permissions!",
        arguments = Some(
          Map(
            "username" -> "(required) <username>",
            "password" -> "(required) <password>",
            "type" -> "(required) [Client|Service]"
          )
        )
      ),
      ActionDescriptor(
        "reset",
        "Resets the specified user's password.",
        arguments = Some(
          Map(
            "username" -> "(required) <username>",
            "password" -> "(required) <password>"
          )
        )
      )
    )
  }
}

/**
  * A wrapper class for [[BootstrapComponent]].
  *
  * @param actor the actor to be used
  */
class Bootstrap(private val actor: ActorRef) {
  /**
    * Retrieves the underlying [[akka.actor.ActorRef]].
    *
    * @return the actor ref
    */
  def getRef: ActorRef = actor
}
