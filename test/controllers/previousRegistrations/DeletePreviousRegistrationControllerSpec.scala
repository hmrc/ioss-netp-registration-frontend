package controllers.previousRegistrations

import base.SpecBase
import controllers.routes
import forms.previousRegistrations.DeletePreviousRegistrationFormProvider
import models.domain.{PreviousSchemeDetails, PreviousSchemeNumbers}
import models.previousRegistrations.PreviousRegistrationDetails
import models.{Country, Index, NormalMode, PreviousScheme, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.Waypoints
import pages.previousRegistrations.DeletePreviousRegistrationPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository

import scala.concurrent.Future

class DeletePreviousRegistrationControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new DeletePreviousRegistrationFormProvider()
  private val form = formProvider()
  
  private val index = Index(0)
  private val country = Country.euCountries.head
  private val previousSchemeNumbers = PreviousSchemeNumbers("VAT Number", None)
  private val previousScheme = PreviousSchemeDetails(PreviousScheme.OSSU, previousSchemeNumbers, None)
  private val previousRegistration = PreviousRegistrationDetails(country, List(previousScheme))

  private def deletePreviousRegistrationRoute(waypoints: Waypoints) =
    controllers.previousRegistrations.routes.DeletePreviousRegistrationController.onPageLoad(waypoints, index).url
    

  "DeletePreviousRegistration Controller" - {
    
  }
}
