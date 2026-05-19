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

package services

import models.domain.{PreviousRegistration, PreviousSchemeDetails}
import models.etmp.display.*
import models.etmp.EtmpOtherAddress
import models.vatEuDetails.EuDetails
import models.{BusinessContactDetails, ClientBusinessName, Country, InternationalAddress, TradingName, UserAnswers, Website}
import pages.{BusinessContactDetailsPage, ClientBusinessAddressPage, ClientBusinessNamePage, ClientCountryBasedPage, ClientTaxReferencePage}
import queries.AllWebsites
import queries.euDetails.AllEuDetailsQuery
import queries.previousRegistrations.AllPreviousRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery

import javax.inject.Inject

class AmendAnswersComparisonService @Inject()() {

  def answersHaveChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    countryBasedInChanged(originalAnswers, userAnswers) ||
      taxReferenceChanged(originalAnswers, userAnswers) ||
      clientBusinessNameChanged(originalAnswers, userAnswers) ||
      clientBusinessAddressChanged(originalAnswers, userAnswers) ||
      tradingNamesChanged(originalAnswers, userAnswers) ||
      previousRegistrationsChanged(originalAnswers, userAnswers) ||
      fixedEstablishmentsChanged(originalAnswers, userAnswers) ||
      amendedFixedEstablishmentsChanged(originalAnswers, userAnswers) ||
      previousRegistrationSchemesChanged(originalAnswers, userAnswers) ||
      websitesChanged(originalAnswers, userAnswers) ||
      contactDetailsChanged(originalAnswers, userAnswers)
  }
  
  private def countryBasedInChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(ClientCountryBasedPage).exists { country =>
      originalAnswers.otherAddress.exists(_.issuedBy != country.code)
    }
  }

  private def taxReferenceChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(ClientTaxReferencePage).exists { taxReference =>
      taxReference != originalAnswers.customerIdentification.idValue
    }
  }
  
  private def clientBusinessNameChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(ClientBusinessNamePage).map(_.name) !=
      originalAnswers.otherAddress.flatMap(_.tradingName)
  }
  
  private def clientBusinessAddressChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(ClientBusinessAddressPage) match {
      case Some(address) =>
        originalAnswers.otherAddress match {
          case Some(originalAddress) =>
            address.line1 != originalAddress.addressLine1 ||
              address.line2 != originalAddress.addressLine2 ||
              address.townOrCity != originalAddress.townOrCity ||
              address.stateOrRegion != originalAddress.regionOrState ||
              address.postCode != originalAddress.postcode

          case None =>
            true
        }

      case None =>
        originalAnswers.otherAddress.isDefined
    }
  }
  
  private def tradingNamesChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllTradingNamesQuery).map(_.map(_.name)).getOrElse(Seq.empty) !=
      originalAnswers.tradingNames.map(_.tradingName)
  }
  
  private def previousRegistrationsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllPreviousRegistrationsQuery).map(_.map(_.previousEuCountry.code)).getOrElse(Seq.empty) !=
      originalAnswers.schemeDetails.previousEURegistrationDetails.map(_.issuedBy).distinct
  }
  
  private def fixedEstablishmentsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllEuDetailsQuery).map(_.map(_.euCountry.code)).getOrElse(Seq.empty) !=
      originalAnswers.schemeDetails.euRegistrationDetails.map(_.issuedBy)
  }
  
  private def amendedFixedEstablishmentsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllEuDetailsQuery).getOrElse(Seq.empty).exists { amendedDetails =>
      originalAnswers.schemeDetails.euRegistrationDetails
        .find(_.issuedBy == amendedDetails.euCountry.code)
        .exists(originalDetails => hasFixedEstablishmentDetailsChanged(amendedDetails, originalDetails))
    }
  }
  
  private def previousRegistrationSchemesChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllPreviousRegistrationsQuery).getOrElse(Seq.empty).exists { amendedCountry =>
      val matchingOriginalRegistrations =
        originalAnswers.schemeDetails.previousEURegistrationDetails
          .filter(_.issuedBy == amendedCountry.previousEuCountry.code)

      val originalSchemeNumbers =
        matchingOriginalRegistrations.map { registration =>
          PreviousSchemeDetails.fromEtmpPreviousEuRegistrationDetails(registration).previousSchemeNumbers
        }

      val amendedSchemeNumbers =
        amendedCountry.previousSchemesDetails.map(_.previousSchemeNumbers)

      originalSchemeNumbers.nonEmpty && amendedSchemeNumbers != originalSchemeNumbers
    }
  }
  
  private def websitesChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllWebsites).map(_.map(_.site)).getOrElse(Seq.empty) !=
      originalAnswers.schemeDetails.websites.map(_.websiteAddress)
  }
  
  private def contactDetailsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(BusinessContactDetailsPage).exists { contactDetails =>
      contactDetails.fullName != originalAnswers.schemeDetails.contactName ||
        contactDetails.telephoneNumber != originalAnswers.schemeDetails.businessTelephoneNumber ||
        contactDetails.emailAddress != originalAnswers.schemeDetails.businessEmailId
    }
  }

  private def hasFixedEstablishmentDetailsChanged(amendedDetails: EuDetails, originalDetails: EtmpDisplayEuRegistrationDetails): Boolean = {

    val vatNumberWithoutCountryCode: Option[String] =
      amendedDetails.euVatNumber.map(_.stripPrefix(amendedDetails.euCountry.code))

    val originalRegistrationVatNumber: Option[String] =
      originalDetails.vatNumber

    amendedDetails.tradingNameAndBusinessAddress.map(_.tradingName.name).exists(_ != originalDetails.fixedEstablishmentTradingName) ||
      amendedDetails.tradingNameAndBusinessAddress.map(_.address).exists { address =>
        originalDetails.fixedEstablishmentAddressLine1 != address.line1 ||
          originalDetails.fixedEstablishmentAddressLine2 != address.line2 ||
          originalDetails.townOrCity != address.townOrCity ||
          originalDetails.regionOrState != address.stateOrRegion ||
          originalDetails.postcode != address.postCode
      } ||
      vatNumberWithoutCountryCode != originalRegistrationVatNumber ||
      amendedDetails.euTaxReference != originalDetails.taxIdentificationNumber
  }
}