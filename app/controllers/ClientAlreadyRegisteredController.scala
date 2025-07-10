package controllers

import controllers.actions.*
import models.UserAnswers

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.{Waypoint, Waypoints}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ClientAlreadyRegisteredView

class ClientAlreadyRegisteredController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       view: ClientAlreadyRegisteredView
                                     ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad: Action[AnyContent] = (cc.actionBuilder andThen cc.identify) {
    implicit request =>
      
      Ok(view())
  }
  
}
