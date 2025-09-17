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

package models.saveAndComeBack

import models.SavedUserAnswers
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

trait ContinueRegistrationSelection

case class SingleRegistration(singleJourneyId: String) extends ContinueRegistrationSelection
case class MultipleRegistrations(multipleRegistrations: Seq[SavedUserAnswers]) extends ContinueRegistrationSelection
case object NoRegistrations extends ContinueRegistrationSelection

object ContinueRegistrationSelection {

  def options(
               seqTaxReferenceInformation: Seq[TaxReferenceInformation]
             )(implicit messages: Messages): Seq[RadioItem] =
    seqTaxReferenceInformation.zipWithIndex.map {
    case (taxReferenceInformation, index) =>
      RadioItem(
        content = Text(messages(
          s"${taxReferenceInformation.organisationName} (${taxReferenceInformation.taxReference}: ${taxReferenceInformation.referenceNumber})"
        )),
        value = Some(taxReferenceInformation.journeyId),
        id = Some(s"value_${index}")
      )
  }
}

