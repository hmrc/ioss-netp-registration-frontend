package services

import config.FrontendAppConfig
import models.audit.JsonAuditModel
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AuditService @Inject()(
                              appConfig: FrontendAppConfig,
                              auditConnector: AuditConnector
                            )(implicit ec: ExecutionContext) {

  def audit(dataSource: JsonAuditModel)(implicit hc: HeaderCarrier, request: Request[_]): Unit = {
    val event = toExtendedDataEvent(dataSource, request.path)
    auditConnector.sendExtendedEvent(event)
  }

  private def toExtendedDataEvent(auditModel: JsonAuditModel, path: String)(implicit hc: HeaderCarrier): ExtendedDataEvent =
    ExtendedDataEvent(
      auditSource = appConfig.appName,
      auditType   = auditModel.auditType,
      tags        = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(auditModel.transactionName, path),
      detail      = auditModel.detail
    )
}
