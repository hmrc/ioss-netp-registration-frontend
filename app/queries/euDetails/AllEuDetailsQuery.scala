package queries.euDetails

import models.euDetails.EuDetails
import play.api.libs.json.JsPath
import queries.{Gettable, Settable}

object AllEuDetailsQuery extends Gettable[List[EuDetails]] with Settable[List[EuDetails]] {

  override def path: JsPath = JsPath \ "euDetails"
}
