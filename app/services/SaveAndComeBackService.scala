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

package services

import connectors.{RegistrationConnector, SaveForLaterConnector}
import logging.Logging
import models.domain.VatCustomerInfo
import models.requests.{DataRequest, OptionalDataRequest}
import models.responses.VatCustomerNotFound
import models.saveAndComeBack.*
import models.{SavedUserAnswers, UserAnswers}
import pages.{ClientBusinessNamePage, ClientTaxReferencePage, ClientUtrNumberPage, ClientVatNumberPage, ClientsNinoNumberPage, ContinueRegistrationSelectionPage, QuestionPage, UkVatNumberNotFoundPage, VatApiDownPage, Waypoints}
import play.api.mvc.Call
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class SaveAndComeBackService @Inject()(
                                        clock: Clock,
                                        registrationConnector: RegistrationConnector,
                                        coreRegistrationValidationService: CoreRegistrationValidationService,
                                        saveForLaterConnector: SaveForLaterConnector
                                      )(implicit ec: ExecutionContext) extends Logging {

  def determineTaxReference(userAnswers: UserAnswers): TaxReferenceInformation = {

    userAnswers.vatInfo match {
      case Some(vatCustomerInfo) =>

        val ukVatNumber = userAnswers.get(ClientVatNumberPage).getOrElse {
          val exception = new IllegalStateException("User answers must include VAT number if vatCustomerInfo present")
          logger.error(exception.getMessage, exception)
          throw exception
        }

        if (vatCustomerInfo.organisationName.isDefined) {
          TaxReferenceInformation(vatCustomerInfo.organisationName.get, "VAT reference", ukVatNumber, userAnswers.journeyId)
        }
        else {
          TaxReferenceInformation(vatCustomerInfo.individualName.get, "VAT reference", ukVatNumber, userAnswers.journeyId)
        }
      case _ =>
        val listOfPages: List[QuestionPage[String]] = List(ClientTaxReferencePage, ClientUtrNumberPage, ClientsNinoNumberPage)

        val companyName: String = userAnswers.get(ClientBusinessNamePage).map(_.name).getOrElse {
          val exception = new IllegalStateException("User answers must include company name if Vat Customer Info was not provided")
          logger.error(exception.getMessage, exception)
          throw exception
        }

        val resultTuple = for {
          customPage <- listOfPages.view
          taxNumber <- userAnswers.get(customPage)
        } yield {
          customPage match {
            case ClientTaxReferencePage => TaxReferenceInformation(companyName, "tax reference", taxNumber, userAnswers.journeyId)
            case ClientUtrNumberPage => TaxReferenceInformation(companyName, "tax reference", taxNumber, userAnswers.journeyId)
            case ClientsNinoNumberPage => TaxReferenceInformation(companyName, "National Insurance Number", taxNumber, userAnswers.journeyId)
          }
        }

        resultTuple.head
    }
  }

  def getVatTaxInfo(ukVatNumber: String, waypoints: Waypoints)(implicit request: DataRequest[_], hc: HeaderCarrier): Future[Either[Call, VatCustomerInfo]] = {
    registrationConnector.getVatCustomerInfo(ukVatNumber).map {
      case Right(value) =>
        Right(value)
      case Left(error) =>
        val message: String = s"Received an unexpected error when trying to retrieve Tax information" +
          s"for the Vat Number: $ukVatNumber,\nWith Errors: $error"
        val exception: Exception = new Exception(message)
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }


  def getSavedContinueRegistrationJourneys(
                                            userAnswers: UserAnswers,
                                            intermediaryNum: String
                                          )(implicit hc: HeaderCarrier): Future[ContinueRegistrationSelection] = {

    userAnswers.get(ContinueRegistrationSelectionPage) match {

      case Some(registration: String) =>
        Future.successful(SingleRegistration(registration))

      case None =>
        saveForLaterConnector.getAllByIntermediary(intermediaryNum).map {
          case Right(seqSavedUserAnswers) if seqSavedUserAnswers.isEmpty =>
            NoRegistrations

          case Right(seqSavedUserAnswers) if seqSavedUserAnswers.size == 1 =>
            SingleRegistration(seqSavedUserAnswers.head.journeyId)

          case Right(seqSavedUserAnswers) =>
            MultipleRegistrations(seqSavedUserAnswers)

          case Left(error) =>
            val message: String = s"Received an unexpected error when trying to retrieve uncompleted " +
              s"registrations for the intermediary ID: $intermediaryNum. \nWith Errors: $error"
            val exception: Exception = new Exception(message)
            logger.error(exception.getMessage, exception)
            throw exception
        }
    }
  }
  

  def createTaxReferenceInfoForSavedUserAnswers(
                                                 seqItems: Seq[SavedUserAnswers]
                                               )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[TaxReferenceInformation]] = {

    val futures: Seq[Future[TaxReferenceInformation]] = seqItems.map { savedUserAnswers =>
      val tempUserAnswers = UserAnswers(savedUserAnswers.journeyId, savedUserAnswers.journeyId, savedUserAnswers.data)

      tempUserAnswers.get(ClientVatNumberPage) match {
        case None =>
          Future.successful(determineTaxReference(tempUserAnswers))

        case Some(vatNum) =>
          registrationConnector.getVatCustomerInfo(vatNum).flatMap {
            case Right(vatInfo) =>
              val updatedTempUserAnswers = tempUserAnswers.copy(vatInfo = Some(vatInfo))
              Future.successful(determineTaxReference(updatedTempUserAnswers))
            case Left(err) =>
              val message: String = s"Received an unexpected error when trying to retrieve Tax information" +
                s"for the Vat Number: $vatNum,\nWith Errors: $err"
              val exception: Exception = new Exception(message)
              logger.error(exception.getMessage, exception)
              throw exception
          }
      }
    }

    Future.sequence(futures)
  }

  def retrieveSingleSavedUserAnswers(
                                      journeyId: String,
                                      waypoints: Waypoints)
                                    (implicit request: OptionalDataRequest[_], hc: HeaderCarrier): Future[UserAnswers] = {
    saveForLaterConnector.getClientRegistration(journeyId).flatMap {
      case Right(savedUserAnswers) =>
        UserAnswers(
          request.userId,
          savedUserAnswers.journeyId,
          data = savedUserAnswers.data,
          vatInfo = None,
          lastUpdated = savedUserAnswers.lastUpdated).toFuture

      case Left(error) =>
        val message: String = s"Received an unexpected error when trying to retrieve Saved User Answers " +
          s"for the journey ID: $journeyId,\nWith Errors: $error"
        val exception: Exception = new Exception(message)
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }
  
  def deleteSavedUserAnswers(journeyId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit] = {
    saveForLaterConnector.delete(journeyId).map {
      _.fold(
        error => {
          val message = s"Received an unexpected error when trying to retrieve Saved User Answers " +
            s"for the journey ID: $journeyId,\nWith Errors: $error"
          val exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
        },
        _ => ()
      )
    }
  }
}
