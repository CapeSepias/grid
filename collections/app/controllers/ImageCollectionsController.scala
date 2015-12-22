package controllers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.DateTime
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.Controller

import com.gu.mediaservice.lib.collections.CollectionsManager
import com.gu.mediaservice.lib.aws.{NoItemFound, DynamoDB}
import com.gu.mediaservice.lib.argo.ArgoHelpers
import com.gu.mediaservice.model.{ActionData, Collection}

import lib.{Notifications, Config, ControllerHelper}


object ImageCollectionsController extends Controller with ArgoHelpers {

  import ControllerHelper.getUserFromReq
  import CollectionsManager.onlyLatest
  import Config.{awsCredentials, dynamoRegion, imageCollectionsTable}

  val Authenticated = ControllerHelper.Authenticated
  val dynamo = new DynamoDB(awsCredentials, dynamoRegion, imageCollectionsTable)

  def getCollections(id: String) = Authenticated.async { req =>
    dynamo.listGet[Collection](id, "collections").map { dynamoEntry =>
      respond(onlyLatest(dynamoEntry))
    } recover {
      case NoItemFound => respondNotFound("No collections found")
    }
  }

  def addCollection(id: String) = Authenticated.async(parse.json) { req =>
    (req.body \ "data").asOpt[List[String]].map { path =>
      val collection = Collection(path, ActionData(getUserFromReq(req), DateTime.now()))
      dynamo.listAdd(id, "collections", collection)
        .map(publish(id))
        .map(cols => respond(collection))
    } getOrElse Future.successful(respondError(BadRequest, "invalid-form-data", "Invalid form data"))
  }

  def publish(id: String)(collections: List[Collection]): List[Collection] = {
    val message = Json.obj(
      "id" -> id,
      "data" -> Json.toJson(onlyLatest(collections))
    )

    Notifications.publish(message, "set-image-collections")
    collections
  }
}



