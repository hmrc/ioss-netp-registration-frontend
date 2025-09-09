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
import controllers.GetClientCompanyName
import controllers.actions.*
import forms.ContinueRegistrationFormProvider
import models.requests.DataRequest
import models.{ClientBusinessName, ContinueRegistration, UserAnswers}
import pages.{ClientBusinessNamePage, ClientHasUtrNumberPage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, JourneyRecoveryPage, QuestionPage, SavedProgressPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.saveAndComeBack.ContinueRegistrationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import play.api.libs.json.Format.GenericFormat

class ContinueRegistrationController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                cc: AuthenticatedControllerComponents,
                                                formProvider: ContinueRegistrationFormProvider,
                                                saveForLaterConnector: SaveForLaterConnector,
                                                view: ContinueRegistrationView,
                                                registrationConnector: RegistrationConnector,
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetClientCompanyName {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[ContinueRegistration] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      request.userAnswers.get(ClientVatNumberPage) match
        case Some(clientVatNumber) =>
          registrationConnector.getVatCustomerInfo(clientVatNumber).flatMap {
          case Left(error)
          => ???
          //TODO - SCG - implement the failure of the call
          case Right(value)
          =>
            println("THIS IS WHAT WE ARE LOOKING AT \n\n")
            println(clientVatNumber)
            println(value)
            val updatedAnswers = request.userAnswers.copy(vatInfo = Some(value))
            cc.sessionRepository.set(updatedAnswers)

            val (clientCompanyName, taxType, taxNumber) = determineTaxReference(updatedAnswers)

              request.userAnswers.get(SavedProgressPage).map { _ =>
                Ok(view(clientCompanyName, taxNumber, form, waypoints)).toFuture
              }.getOrElse {
                Redirect(controllers.routes.IndexController.onPageLoad()).toFuture // TODO- SCG- Change the index
              }
        }

        case None =>
              val (clientCompanyName, taxType, taxNumber) = determineTaxReference(request.userAnswers)

                request.userAnswers.get(SavedProgressPage).map { _ =>
                  Ok(view(clientCompanyName, taxNumber, form, waypoints)).toFuture
                }.getOrElse {
                  Redirect(controllers.routes.IndexController.onPageLoad()).toFuture // TODO- SCG- Change the index
                }
  }

 // request.userAnswers.get(ClientVatNumberPage).getOrElse("")

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.identifyAndGetData.async {
    implicit request =>

      val (clientCompanyName, taxType, taxNumber) = determineTaxReference(request.userAnswers)

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(clientCompanyName, taxNumber, formWithErrors, waypoints)).toFuture,

        value1 =>

                println(s" \n\nwill this submit ${request.userAnswers.data}")
                println(request.userAnswers.data)
                println(s"\n\n What about this? ")
                println(request.userAnswers)
                (value1, request.userAnswers.get(SavedProgressPage)) match {
                  case (ContinueRegistration.Continue, Some(url)) =>
                    println("calling")
                    val completeUrl = s"http://localhost:10181$url"
                    println(s"\n$url")
                    println(s"\n$completeUrl")
                    Redirect(Call(GET, completeUrl)).toFuture

                  case (ContinueRegistration.Delete, _) =>
                    for {
                      _ <- cc.sessionRepository.clear(request.userId)
                      _ <- saveForLaterConnector.delete()
                    } yield Redirect(controllers.routes.IndexController.onPageLoad())

                  case _ =>
                    Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
                }
      )

  }

  def determineTaxReference(userAnswers: UserAnswers): (String, String, String) = {

    val ukVatNumber = userAnswers.get(ClientVatNumberPage).getOrElse("Error")

    userAnswers.vatInfo match {
      case Some(vatCustomerInfo) =>
        if(vatCustomerInfo.organisationName.isDefined) {
          (vatCustomerInfo.organisationName.get, "VAT", ukVatNumber)}
        else {
          (vatCustomerInfo.individualName.get, "VAT", ukVatNumber)
        }
      case _ =>
          println("are we getting here 10am? ")
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
  }
// Yes, Yes- UK VAT INFO (vatCustomerInfo)
// No, Yes- UK VAT INFO (vatCustomerInfo)
// No, No - Which country based in, national tax number, client-business-name (ClientTaxReferencePage)
// Yes, No - client-business-name, Unique Taxpayer Reference? *
// * Yes- What is UTR (ClientHasUtrNumberPage)
// * No - What is NINO (ClientsNinoNumberPage)

// Option 1 - VAT INFO
// Option 2 - National Tax Number - Client Business Name
// Option 3 - Client Business Name - UTR
// Option 3 - Client Business Name - NINO

