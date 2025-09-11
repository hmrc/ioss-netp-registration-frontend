/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.saveAndComeBack

import connectors.{RegistrationConnector, SaveForLaterConnector}
import controllers.actions.AuthenticatedControllerComponents
import pages.{ClientBusinessNamePage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, ContinueRegistrationSelectionPage, JourneyRecoveryPage, QuestionPage, Waypoints}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import controllers.saveAndComeBack.ContinueRegistrationController
import forms.{ContinueRegistrationFormProvider, ContinueRegistrationSelectionFormProvider}
import logging.Logging
import models.{ContinueRegistration, SavedUserAnswers, UserAnswers}
import play.api.data.Form
import play.mvc.Results.ok
import uk.gov.hmrc.govukfrontend.views.Aliases.{RadioItem, Text}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationSelectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class ContinueRegistrationSelectionController @Inject()(
                                                         override val messagesApi: MessagesApi,
                                                         cc: AuthenticatedControllerComponents,
                                                         saveForLaterConnector: SaveForLaterConnector,
                                                         registrationConnector: RegistrationConnector,
                                                         formProvider: ContinueRegistrationSelectionFormProvider,
                                                         view: ContinueRegistrationSelectionView
                                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetOptionalData.async {
    implicit request =>

      val preparedForm = request.userAnswers.get($className$Page) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      
      saveForLaterConnector.getAllByIntermediary(request.intermediaryNumber.get).flatMap { //TODO - SCG- Don't use a .get

        case Right(seqSavedUserAnswers) if seqSavedUserAnswers.size == 1 => {
          val savedUserAnswers = seqSavedUserAnswers.head
          val updatedUserAnswers = UserAnswers(
            request.userId,
            savedUserAnswers.journeyId,
            data = savedUserAnswers.data,
            vatInfo = None,
            lastUpdated = savedUserAnswers.lastUpdated) //TODO- SCG- Should this be userAnswers
          for {
            _ <- cc.sessionRepository.set(updatedUserAnswers)
          } yield Redirect(controllers.saveAndComeBack.routes.ContinueRegistrationController.onPageLoad())
        }

        case Right(seqSavedUserAnswers) =>
          // view(form = ???, waypoints)
          fetchOutcomesFailFast(seqSavedUserAnswers).map { outcome =>

          val outcomeButtons: Seq[RadioItem] = outcome.map { (one, two ,three) =>
                RadioItem(
                  content = Text(one),
                  value = Some(three), // TODO - SCG - it is the value field for onSubmit
                  id = Some(three)
                )
            }

            Ok(view(outcomeButtons, form, waypoints))
          }

        case Left(error) =>
          println("\n\n\nSCG Not yet implemented 3")
          logger.warn(s"Failed to get the registration: $error")
          Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
      }
  }


  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetOptionalData {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Redirect(JourneyRecoveryPage.route(waypoints).url),

        value =>
          println("\n\n On Submit Value")
          println(value)
          Redirect(ContinueRegistrationSelectionPage.route(waypoints).url)
      )
  }

  def determineTaxReference(userAnswers: UserAnswers): (String, String, String) = {

    val ukVatNumber = userAnswers.get(ClientVatNumberPage).getOrElse("Error")

    userAnswers.vatInfo match {
      case Some(vatCustomerInfo) =>
        if (vatCustomerInfo.organisationName.isDefined) {
          (vatCustomerInfo.organisationName.get, "VAT", ukVatNumber)
        }
        else {
          (vatCustomerInfo.individualName.get, "VAT", ukVatNumber)
        }
      case _ =>
        val listOfPages: List[QuestionPage[String]] = List(ClientTaxReferencePage, ClientUtrNumberPage, ClientsNinoNumberPage)
        val companyName: String = userAnswers.get(ClientBusinessNamePage).head.name

        val resultTuple = for {
          customPage <- listOfPages.view
          taxNumber <- userAnswers.get(customPage)
        } yield {
          customPage match {
            case ClientTaxReferencePage => (companyName, "tax reference", taxNumber.toString)
            case ClientUtrNumberPage => (companyName, "UTR", taxNumber.toString)
            case ClientsNinoNumberPage => (companyName, "NINO", taxNumber.toString)
          }
        }

        resultTuple.head
    }
  }

  def fetchOutcomesFailFast(
                             seqItems: Seq[SavedUserAnswers]
                           )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[(String, String, String)]] = {

    val futures: Seq[Future[(String, String, String)]] = seqItems.map { savedUserAnswers =>
      val tempUserAnswers = UserAnswers(savedUserAnswers.journeyId, savedUserAnswers.journeyId, savedUserAnswers.data)

      tempUserAnswers.get(ClientVatNumberPage) match {
        case None =>
          // decide: either fail, or provide a default; here we fail:
          Future.failed(new NoSuchElementException(s"Missing value for item ${savedUserAnswers}"))

        case Some(vatNum) =>
          registrationConnector.getVatCustomerInfo(vatNum).flatMap {
            case Right(vatInfo) =>
              val updatedTempUserAnswers = tempUserAnswers.copy(vatInfo = Some(vatInfo))
                Future.successful(determineTaxReference(updatedTempUserAnswers))
            case Left(err) =>
              Future.failed(new RuntimeException(s"Connector returned error: $err"))
          }
      }
    }

    Future.sequence(futures) // will fail if any future fails
  }
}