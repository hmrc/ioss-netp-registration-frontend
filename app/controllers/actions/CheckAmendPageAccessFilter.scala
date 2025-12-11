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

package controllers.actions

import logging.Logging
import models.{Index, UserAnswers}
import models.requests.{DataRequest, OptionalDataRequest}
import pages.*
import pages.previousRegistrations.*
import pages.tradingNames.*
import pages.vatEuDetails.*
import pages.website.*
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckAmendPageAccessFilter @Inject()(
                                            implicit val executionContext: ExecutionContext
                                          ) extends Logging {

  private[actions] def shouldBlockPage(maybeUserAnswers: Option[UserAnswers], page: Page, hasExclusion: Boolean): Boolean = {
    maybeUserAnswers.exists { userAnswers =>
      val userAnswers = maybeUserAnswers.getOrElse(UserAnswers(""))
      val blockedPages = getBlockedPages(userAnswers, hasExclusion)
      blockedPages.exists(_.getClass == page.getClass)
    }
  }

  private[actions] def getBlockedPages(userAnswers: UserAnswers, hasExclusion: Boolean): Seq[Page] = {
    val isUkBased = userAnswers.get(BusinessBasedInUKPage).getOrElse(true)
    val hasVatNumber = userAnswers.get(ClientHasVatNumberPage).getOrElse(false)
    val hasUtrNumber = userAnswers.get(ClientHasUtrNumberPage).getOrElse(false)

    val alwaysBlocked = Seq(
      BusinessBasedInUKPage,
      ClientHasVatNumberPage,
      CheckVatDetailsPage(),
      ClientHasUtrNumberPage,
      ClientUtrNumberPage,
      ClientsNinoNumberPage,
      ClientVatNumberPage
    )

    val exclusionBlocked = if (hasExclusion) {
      Seq(
        ClientBusinessAddressPage,
        ClientCountryBasedPage,
        ClientTaxReferencePage,
        ClientBusinessNamePage,
      ) ++
      websiteBlocked ++
      vatEuDetailsBlocked ++
      previousRegistrationsBlocked ++
      tradingNameBlocked
    } else {
      Seq.empty
    }

    val additionalBlocked = (isUkBased, hasVatNumber, hasUtrNumber) match {
      case (true, true, _) => Seq(ClientBusinessAddressPage, ClientCountryBasedPage, ClientTaxReferencePage, ClientBusinessNamePage)
      case (true, false, true) => Seq(ClientTaxReferencePage, ClientCountryBasedPage)
      case (true, false, false) => Seq(ClientTaxReferencePage, ClientCountryBasedPage)
      case (false, true, _) => Seq(ClientTaxReferencePage)
      case (false, false, _) => Seq.empty
    }

    alwaysBlocked ++ additionalBlocked ++ exclusionBlocked
  }

  private def tradingNameBlocked = Seq(
    TradingNamePage(Index(0)),
    HasTradingNamePage,
    AddTradingNamePage(),
    DeleteAllTradingNamesPage,
    DeleteTradingNamePage(Index(0)),
  )

  private def previousRegistrationsBlocked = Seq(
    PreviouslyRegisteredPage,
    PreviousEuCountryPage(Index(0)),
    PreviousSchemeTypePage(Index(0), Index(0)),
    ClientHasIntermediaryPage(Index(0), Index(0)),
    PreviousSchemePage(Index(0), Index(0)),
    CheckPreviousSchemeAnswersPage(Index(0)),
    PreviousOssNumberPage(Index(0), Index(0)),
    PreviousIossNumberPage(Index(0), Index(0)),
    AddPreviousRegistrationPage(),
    DeleteAllPreviousRegistrationsPage,
    DeletePreviousSchemePage(Index(0), Index(0)),
    DeletePreviousRegistrationPage(Index(0)),
  )

  private def vatEuDetailsBlocked = Seq(
    HasFixedEstablishmentPage,
    EuCountryPage(Index(0)),
    TradingNameAndBusinessAddressPage(Index(0)),
    RegistrationTypePage(Index(0)),
    EuVatNumberPage(Index(0)),
    EuTaxReferencePage(Index(0)),
    CheckEuDetailsAnswersPage(Index(0)),
    AddEuDetailsPage(),
    DeleteAllEuDetailsPage,
    DeleteEuDetailsPage(Index(0)),
  )

  private def websiteBlocked = Seq(
    WebsitePage(Index(0)),
    AddWebsitePage(),
    DeleteWebsitePage(Index(0))
  )

  def apply(page: Page): ActionFilter[DataRequest] = new ActionFilter[DataRequest] {
    override protected def executionContext: ExecutionContext = CheckAmendPageAccessFilter.this.executionContext

    override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
      val hasExclusion = request.registrationWrapper.exists(_.etmpDisplayRegistration.isExcluded)

      if (shouldBlockPage(Some(request.userAnswers), page, hasExclusion)) {
        logger.info(s"Blocked access to ${page.toString} for ${request.iossNumber}")
        Future.successful(Some(Redirect(controllers.amend.routes.ChangeRegistrationController.onPageLoad())))
      } else {
        Future.successful(None)
      }
    }
  }

  def forOptionalData(page: Page): ActionFilter[OptionalDataRequest] = new ActionFilter[OptionalDataRequest] {
    override protected def executionContext: ExecutionContext = CheckAmendPageAccessFilter.this.executionContext

    override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
      val hasExclusion = request.registrationWrapper.exists(_.etmpDisplayRegistration.isExcluded)

      if (shouldBlockPage(request.userAnswers, page, hasExclusion)) {
        logger.info(s"Blocked access to ${page.toString} for ${request.intermediaryNumber}")
        Future.successful(Some(Redirect(controllers.amend.routes.ChangeRegistrationController.onPageLoad())))
      } else {
        Future.successful(None)
      }
    }
  }
}