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

package generators

import models.Index
import org.scalacheck.Arbitrary
import pages.previousRegistrations.*
import pages.vatEuDetails.*
import pages.{BusinessContactDetailsPage, ClientBusinessAddressPage}

trait PageGenerators {

  implicit lazy val arbitraryClientBusinessAddressPage: Arbitrary[ClientBusinessAddressPage.type] =
    Arbitrary(ClientBusinessAddressPage)

  implicit lazy val arbitraryBusinessContactDetailsPage: Arbitrary[BusinessContactDetailsPage.type] = {
    Arbitrary(BusinessContactDetailsPage)
  }

  implicit lazy val arbitraryPreviouslyRegisteredPage: Arbitrary[PreviouslyRegisteredPage.type] = {
    Arbitrary(PreviouslyRegisteredPage)
  }

  implicit lazy val arbitraryPreviousEuCountryPage: Arbitrary[PreviousEuCountryPage] = {
    Arbitrary(PreviousEuCountryPage(Index(0)))
  }

  implicit lazy val arbitraryPreviousSchemePage: Arbitrary[PreviousSchemePage] = {
    Arbitrary(PreviousSchemePage(Index(0), Index(0)))
  }

  implicit lazy val arbitraryPreviousSchemeTypePage: Arbitrary[PreviousSchemeTypePage] = {
    Arbitrary(PreviousSchemeTypePage(Index(0), Index(0)))
  }

  implicit lazy val arbitraryPreviousOssNumberPage: Arbitrary[PreviousOssNumberPage] = {
    Arbitrary(PreviousOssNumberPage(Index(0), Index(0)))
  }

  implicit lazy val arbitraryPreviousIossNumberPage: Arbitrary[PreviousIossNumberPage] = {
    Arbitrary(PreviousIossNumberPage(Index(0), Index(0)))
  }

  implicit lazy val arbitraryAddPreviousRegistrationPage: Arbitrary[AddPreviousRegistrationPage.type] = {
    Arbitrary(AddPreviousRegistrationPage)
  }

  implicit lazy val arbitraryDeletePreviousSchemePage: Arbitrary[DeletePreviousSchemePage.type] = {
    Arbitrary(DeletePreviousSchemePage)
  }

  implicit lazy val arbitraryDeleteAllPreviousRegistrationsPage: Arbitrary[DeleteAllPreviousRegistrationsPage.type] = {
    Arbitrary(DeleteAllPreviousRegistrationsPage)
  }

  implicit lazy val arbitraryHasFixedEstablishmentPage: Arbitrary[HasFixedEstablishmentPage.type] = {
    Arbitrary(HasFixedEstablishmentPage)
  }

  implicit lazy val arbitraryEuCountryPage: Arbitrary[EuCountryPage] = {
    Arbitrary(EuCountryPage(Index(0)))
  }

  implicit lazy val arbitraryTradingNameAndAddress: Arbitrary[TradingNameAndBusinessAddressPage] = {
    Arbitrary(TradingNameAndBusinessAddressPage(Index(0)))
  }

  implicit lazy val arbitraryRegistrationTypePage: Arbitrary[RegistrationTypePage] = {
    Arbitrary(RegistrationTypePage(Index(0)))
  }

  implicit lazy val arbitraryEuVatNumberPage: Arbitrary[EuVatNumberPage] = {
    Arbitrary(EuVatNumberPage(Index(0)))
  }

  implicit lazy val arbitraryEuTaxReferencePage: Arbitrary[EuTaxReferencePage] = {
    Arbitrary(EuTaxReferencePage(Index(0)))
  }

  implicit lazy val arbitraryAddEuDetailsPage: Arbitrary[AddEuDetailsPage] = {
    Arbitrary(AddEuDetailsPage(Some(Index(1))))
  }

  implicit lazy val arbitraryDeleteEuDetailsPage: Arbitrary[DeleteEuDetailsPage] = {
    Arbitrary(DeleteEuDetailsPage(Index(0)))
  }

  implicit lazy val arbitraryClientHasIntermediaryPage: Arbitrary[ClientHasIntermediaryPage] = {
    Arbitrary(ClientHasIntermediaryPage(Index(0), Index(0)))
  }
}
