package controllers.amend

import base.SpecBase
import config.FrontendAppConfig
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.amend.ErrorSubmittingAmendView

class ErrorSubmittingAmendControllerSpec extends SpecBase with MockitoSugar {

  "ErrorSubmittingAmendment Controller" - {

    "must return OK and the correct view" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.amend.routes.ErrorSubmittingAmendController.onPageLoad(waypoints).url)

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val view = application.injector.instanceOf[ErrorSubmittingAmendView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(config.intermediaryYourAccountUrl)(request, messages(application)).toString
      }
    }
  }
}
