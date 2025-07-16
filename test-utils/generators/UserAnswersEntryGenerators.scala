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

import models.*
import models.vatEuDetails.TradingNameAndBusinessAddress
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import pages.previousRegistrations.*
import pages.vatEuDetails.*
import pages.{BusinessContactDetailsPage, ClientBusinessAddressPage}
import play.api.libs.json.{JsValue, Json}

trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

  implicit lazy val arbitraryClientBusinessAddressUserAnswersEntry: Arbitrary[(ClientBusinessAddressPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[ClientBusinessAddressPage.type]
        value <- arbitrary[InternationalAddress].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryBusinessContactDetailsUserAnswersEntry: Arbitrary[(BusinessContactDetailsPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[BusinessContactDetailsPage.type]
        value <- arbitrary[BusinessContactDetails].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryPreviouslyRegisteredUserAnswersEntry: Arbitrary[(PreviouslyRegisteredPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[PreviouslyRegisteredPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryPreviousEuCountryUserAnswersEntry: Arbitrary[(PreviousEuCountryPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[PreviousEuCountryPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryPreviousSchemePageUserAnswersEntry: Arbitrary[(PreviousSchemePage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[PreviousSchemePage]
        value <- arbitrary[PreviousScheme].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryPreviousSchemeTypePageUserAnswersEntry: Arbitrary[(PreviousSchemeTypePage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[PreviousSchemeTypePage]
        value <- arbitrary[PreviousSchemeType].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryPreviousOssNumberUserAnswersEntry: Arbitrary[(PreviousOssNumberPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[PreviousOssNumberPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryPreviousIossNumberUserAnswersEntry: Arbitrary[(PreviousIossNumberPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[PreviousIossNumberPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }


  implicit lazy val arbitraryAddPreviousRegistrationUserAnswersEntry: Arbitrary[(AddPreviousRegistrationPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[AddPreviousRegistrationPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryDeleteAllPreviousRegistrationsUserAnswersEntry: Arbitrary[(DeleteAllPreviousRegistrationsPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[DeleteAllPreviousRegistrationsPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryDeletePreviousSchemeUserAnswersEntry: Arbitrary[(DeletePreviousSchemePage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[DeletePreviousSchemePage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryHasFixedEstablishmentAnswersEntry: Arbitrary[(HasFixedEstablishmentPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[HasFixedEstablishmentPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryEuCountryUserAnswersEntry: Arbitrary[(EuCountryPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[EuCountryPage]
        value <- arbitrary[Country].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryTradingNameAndBusinessAddressEntry: Arbitrary[(TradingNameAndBusinessAddressPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[TradingNameAndBusinessAddressPage]
        value <- arbitrary[TradingNameAndBusinessAddress].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryRegistrationTypeUserAnswersEntry: Arbitrary[(RegistrationTypePage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[RegistrationTypePage]
        value <- arbitrary[RegistrationType].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryEuVatNumberUserAnswersEntry: Arbitrary[(EuVatNumberPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[EuVatNumberPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryEuTaxReferenceUserAnswersEntry: Arbitrary[(EuTaxReferencePage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[EuTaxReferencePage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryAddEuDetailsUserAnswersEntry: Arbitrary[(AddEuDetailsPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[AddEuDetailsPage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryClientHasIntermediaryUserAnswersEntry: Arbitrary[(ClientHasIntermediaryPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[ClientHasIntermediaryPage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }
}
