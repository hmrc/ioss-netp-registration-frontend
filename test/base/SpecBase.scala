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

package base

import controllers.actions.*
import generators.Generators
import models.domain.VatCustomerInfo
import models.{BusinessContactDetails, UserAnswers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.{EmptyWaypoints, Waypoints}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest

import java.time.{Clock, Instant, LocalDate, ZoneId}

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with Generators {

  val userAnswersId: String = "12345-credId"

  val arbitraryInstant: Instant = arbitraryDate.arbitrary.sample.value.atStartOfDay(ZoneId.systemDefault()).toInstant
  val stubClockAtArbitraryDate: Clock = Clock.fixed(arbitraryInstant, ZoneId.systemDefault())

  val waypoints: Waypoints = EmptyWaypoints

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  def emptyUserAnswers: UserAnswers = UserAnswers(id = userAnswersId, lastUpdated = arbitraryInstant)

  def emptyUserAnswersWithVatInfo: UserAnswers = emptyUserAnswers.copy(vatInfo = Some(vatCustomerInfo))

  val vatNumber = "GB123456789"
  val intermediaryNumber = "IN9001234567"
  val vatCustomerInfo: VatCustomerInfo = {
    VatCustomerInfo(
      registrationDate = LocalDate.now(stubClockAtArbitraryDate),
      desAddress = arbitraryDesAddress.arbitrary.sample.value,
      organisationName = Some("Company name"),
      individualName = None,
      singleMarketIndicator = true,
      deregistrationDecisionDate = None
    )
  }

  val intermediaryVatCustomerInfo: VatCustomerInfo = {
    VatCustomerInfo(
      registrationDate = LocalDate.now(stubClockAtArbitraryDate),
      desAddress = arbitraryDesAddress.arbitrary.sample.value,
      organisationName = Some("Intermediary Company name"),
      individualName = None,
      singleMarketIndicator = true,
      deregistrationDecisionDate = None
    )
  }

  val businessContactDetails: BusinessContactDetails =
    BusinessContactDetails(fullName = "name", telephoneNumber = "0111 2223334", emailAddress = "email@example.com")

  protected def applicationBuilder(
                                    userAnswers: Option[UserAnswers] = None,
                                    clock: Option[Clock] = None
                                  ): GuiceApplicationBuilder = {

    val clockToBind = clock.getOrElse(stubClockAtArbitraryDate)
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[Clock].toInstance(clockToBind),
      )
  }
}
