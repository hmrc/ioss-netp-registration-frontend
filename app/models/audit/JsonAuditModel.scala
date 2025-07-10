package models.audit

import play.api.libs.json.JsValue

trait JsonAuditModel {
  val auditType: String
  val transactionName: String
  val detail: JsValue
}
