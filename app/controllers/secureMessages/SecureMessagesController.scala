/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.secureMessages

import config.FrontendAppConfig
import connectors.{RegistrationConnector, SecureMessageConnector}
import controllers.GetClientCompanyName
import controllers.actions.*
import logging.Logging
import pages.Waypoints
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.Table
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.secureMessages.SecureMessagesView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SecureMessagesController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       val controllerComponents: MessagesControllerComponents,
                                       registrationConnector: RegistrationConnector,
                                       secureMessageConnector: SecureMessageConnector,
                                       netpValidationFilterProvider: NetpValidationFilterProvider,
                                       frontendAppConfig: FrontendAppConfig,
                                       view: SecureMessagesView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetClientCompanyName with Logging {

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (cc.clientIdentify andThen netpValidationFilterProvider.apply()).async {
    implicit request =>

      val netpEnrolment = frontendAppConfig.netpEnrolment
      val iossNumber = request.enrolments.enrolments
        .find(_.key == frontendAppConfig.netpEnrolment)
        .flatMap(_.identifiers.headOption.map(_.value))
        .getOrElse("")

      registrationConnector.displayRegistrationNetp(iossNumber).flatMap {
        case Right(registrationWrapper) =>

          val clientCompanyName =
            registrationWrapper.vatInfo.flatMap(_.organisationName)
              .orElse(registrationWrapper.vatInfo.flatMap(_.individualName))
              .orElse(registrationWrapper.etmpDisplayRegistration.otherAddress.flatMap(_.tradingName))
              .getOrElse("")

          secureMessageConnector.getMessages(taxIdentifiers = Some(netpEnrolment)).flatMap {
            case Right(secureMessage) =>

              val unreadMessages: Seq[Boolean] = secureMessage.items.map(_.unreadMessages)
              val messageSubject: Seq[String] = secureMessage.items.map(_.subject)
              val messageValidFrom: Seq[String] = secureMessage.items.map(_.validFrom)
              val messageId: Seq[String] = secureMessage.items.map(_.id)

              val messageTable = buildMessagesTable(messageSubject, messageValidFrom, unreadMessages, messageId)

              Ok(view(clientCompanyName, iossNumber, messageTable)).toFuture

            case Left(errors) =>
              val message: String = s"Received an unexpected error when trying to retrieve secure messages: $errors."
              val exception: Exception = new Exception(message)
              logger.error(exception.getMessage, exception)
              throw exception
          }

        case Left(error) =>
          val message = s"Failed to retrieve registration for IOSS number $iossNumber: $error"
          logger.error(message)
          Future.failed(new Exception(message))

      }
  }

  private def buildMessagesTable(
                                  subject: Seq[String],
                                  validFrom: Seq[String],
                                  unreadMessages: Seq[Boolean],
                                  messageId: Seq[String]
                                )(implicit messages: Messages): Table = {

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

    val rows: Seq[Seq[TableRow]] = {

      val combinedList = subject.lazyZip(validFrom).lazyZip(unreadMessages).lazyZip(messageId).toList

      val sortedList = combinedList.sortBy { case (_, dateStr, _, _) =>
        LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
      }(Ordering[LocalDate].reverse)

      sortedList.map { case (sub, dateStr, unreadMessage, messageId) =>
        val formattedDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).format(dateFormatter)

        val messageLink = routes.IndividualSecureMessageController.onPageLoad(messageId).url

        val displayCorrectMessage =
          if (unreadMessage) {
            HtmlContent(messages("redDot") ++ messages("secureMessages.subject.unread", sub, messageLink))
          } else  {
            HtmlContent(messages("secureMessages.subject.read", sub, messageLink))
          }

        Seq(
          TableRow(content = displayCorrectMessage),
          TableRow(content = Text(formattedDate))
        )
      }
    }

    Table(
      rows,
      head = Some(Seq(
        HeadCell(
          content = Text(messages("secureMessages.table.headContent.column1"))
        ),
        HeadCell(
          content = Text(messages("secureMessages.table.headContent.column2")),
          classes = "govuk-!-width-one-quarter"
        )
      ))
    )
  }
}
