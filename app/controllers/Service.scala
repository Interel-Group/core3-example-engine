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
package controllers

import javax.inject.{Inject, Singleton}

import akka.util.Timeout
import core3.config.StaticConfig
import core3.core.ComponentManager
import core3.database.dals.DatabaseAbstractionLayer
import core3.http.controllers.local.ServiceController
import core3.http.handlers
import core3.http.responses.GenericResult
import core3.security.{LocalAuthUserToken, UserTokenBase}
import core3.workflows.{WorkflowBase, WorkflowEngine, WorkflowRequest}
import play.api.{Environment, Logger}
import play.api.cache.CacheApi
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class Service @Inject()(db: DatabaseAbstractionLayer, cache: CacheApi, engine: WorkflowEngine, workflows: Seq[WorkflowBase], manager: ComponentManager)
  (implicit environment: Environment, ec: ExecutionContext)
  extends ServiceController(cache, StaticConfig.get.getConfig("security.authentication.clients.LocalEngineExample"), db) {
  private val auditLogger = Logger("core3-example-audit")
  implicit private val timeout = Timeout(StaticConfig.get.getInt("manager.requestTimeout").seconds)

  def users() = UserAwareAction(
    "exec:asUser",
    (request: Request[AnyContent], user: UserTokenBase) => {
      request.body.asJson match {
        case Some(httpRequest) =>
          (for {
            workflowRequest <- Future {
              WorkflowRequest(httpRequest)
            }
            workflowResult <- engine.executeWorkflow(
              workflowRequest.workflowName,
              workflowRequest.rawParams,
              user,
              workflowRequest.returnOutputData
            )
          } yield {
            Ok(workflowResult.asJson)
          }).recover {
            case e =>
              val message = s"Exception encountered while processing request: [${e.getMessage}]."
              auditLogger.error(s"controllers.Service::asUser > $message")
              InternalServerError(GenericResult(wasSuccessful = false, Some(message)).asJson)
          }

        case None =>
          val message =
            request.contentType match {
              case Some(contentType) => s"Unexpected request content type received: [$contentType]."
              case None => s"Failed to determine request content type."
            }

          auditLogger.error(s"controllers.UsersService::asUser > $message")
          Future.successful(BadRequest(GenericResult(wasSuccessful = false, Some(message)).asJson))
      }
    }
  )

  def clients() = ClientAwareAction(
    "exec:asClient",
    (request: Request[AnyContent], clientID: String) => {
      request.body.asJson match {
        case Some(httpRequest) =>
          (for {
            workflowRequest <- Future {
              WorkflowRequest(httpRequest)
            }
            workflowResult <- engine.executeWorkflow(
              workflowRequest.workflowName,
              workflowRequest.rawParams,
              LocalAuthUserToken(
                userID = clientID,
                permissions = workflows.map(_.name), //Warning: allows the client to execute any workflow
                profile = Json.obj(),
                sessionToken = "None"
              ),
              workflowRequest.returnOutputData
            )
          } yield {
            Ok(workflowResult.asJson)
          }).recover {
            case e =>
              val message = s"Exception encountered while processing request: [${e.getMessage}]."
              auditLogger.error(s"controllers.Service::asClient > $message")
              InternalServerError(GenericResult(wasSuccessful = false, Some(message)).asJson)
          }

        case None =>
          val message =
            request.contentType match {
              case Some(contentType) => s"Unexpected request content type received: [$contentType]."
              case None => s"Failed to determine request content type."
            }

          auditLogger.error(s"controllers.UsersService::asClient > $message")
          Future.successful(BadRequest(GenericResult(wasSuccessful = false, Some(message)).asJson))
      }
    }
  )

  def manage() = ClientAwareAction(
    "exec:manage",
    (request: Request[AnyContent], _) => {
      handlers.JSON.management(manager.getRef, auditLogger, request)
    }
  )
}
