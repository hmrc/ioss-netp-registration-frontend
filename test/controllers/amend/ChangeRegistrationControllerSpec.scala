package controllers.amend

import base.SpecBase
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar

class ChangeRegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {
  
  ".ChangeRegistrationController" - { 
    "should" in {
      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
        .build()
    }
  }
}

