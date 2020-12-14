package controllers

import java.nio.file.{Files, Paths}

import api.server.Commons._
import javax.inject._
import play.api.mvc._

import scala.sys.process._


@Singleton
class RenderController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

    /**
      * This controller handles all the get requests and page rendering in the application
      * @author Obafemi Teminife
      * @group controllers
      */

  def flash(notice: String, mode: String): List[(String, String)] = {
    List("notice" -> notice, "notice-type" -> s"alert-$mode", "showing" -> "show")
  }
  def err500: Result = InternalServerError(views.html.err.InternalServerError())
  def err404: Result = NotFound(views.html.err.NotFound())
  def apiErr: Result = InternalServerError(views.html.err.HostUnreachable())
  def errUnknown(reason: String): Result = InternalServerError(views.html.err.UnknownError(reason))

  def validateSession[A](request: Request[A], next: Result): Result = {
    request.session.get("user").map(x => if (user.username == x){
      next
    } else {
      Forbidden(views.html.err.Error(403, "Session expired!"))
    }).get
  }
  

  //TODO: Add sid verification and session validation
  def dashboard(route: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] => {
      route match {
        case "home" =>
          validateSession(request, Ok(views.html.home()))
        case _ => err404
      }
    }
  }
  def studentExamReplyTheory(x: Int): Action[AnyContent] = Action {
    implicit request => {
      validateSession(request, Ok(views.html.student.exam_reply_theory(x)))
    }
  }
  def studentExamReplyObjective(x: Int): Action[AnyContent] = Action {
    implicit request => {
      validateSession(request, Ok(views.html.student.exam_reply_obj(x)))
    }
  }

//  def teacherExamReplyTheory(x: Int): Action[AnyContent] = Action {
//    implicit request => {
//      validateSession(request, Ok(views.html.teacher.manage.exam_reply_theory(x)))
//    }
//  }
//  def teacherExamReplyObjective(x: Int): Action[AnyContent] = Action {
//    implicit request => {
//      validateSession(request, Ok(views.html.teacher.manage.exam_reply_obj(x)))
//    }
//  }

  def student(path: String): Action[AnyContent] = Action{
    implicit request: Request[AnyContent] => {
      path match {
        case "notes" => validateSession(request, Ok(views.html.student.notes()))
        case "classwork" => validateSession(request, Ok(views.html.student.classwork()))
        case "assignments" => validateSession(request, Ok(views.html.student.assignments()))
        case "assessments" => validateSession(request, Ok(views.html.student.assessments()))
        case _ => err404
      }
    }
  }

  def upload(path: String): Action[AnyContent] = Action{
    implicit request: Request[AnyContent] => {
      path match {
        case "note" => validateSession(request, Ok(views.html.teacher.upload.notes()))
        case "classwork" => validateSession(request, Ok(views.html.teacher.upload.classwork()))
        case "assignment" => validateSession(request, Ok(views.html.teacher.upload.assignments()))
        case "assessment" => validateSession(request, Ok(views.html.teacher.upload.assessments()))
        case _ => err404
      }
    }
  }
  def manage(path: String): Action[AnyContent] = Action{
    implicit request: Request[AnyContent] => {
      path match {
        case "notes" => validateSession(request, Ok(views.html.teacher.manage.notes()))
        case "classworks" => validateSession(request, Ok(views.html.teacher.manage.classwork()))
        case "assignments" => validateSession(request, Ok(views.html.teacher.manage.assignments()))
        case "assessments" => validateSession(request, Ok(views.html.teacher.manage.assessments()))
        case _ => err404
      }
    }
  }

  def account(route: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] => {
      route match {
        case "login" => Ok(views.html.login())
        case "create" => Ok(views.html.signup())
        case "profile" => validateSession(request, Ok(views.html.profile()))
        case "logout" =>
          user = null
          Redirect("/account/login").withNewSession
        case _ => err404
      }
    }
  }

  def redirect: Action[AnyContent] = Action{
    implicit request: Request[AnyContent] => {
      Redirect("/account/login").withNewSession
    }
  }

  def error(err: String): Action[AnyContent] = Action{
    implicit request: Request[AnyContent] => {
      err match {
        case "api/connect_failure" => request.flash.get("reason") match {
          case Some("Server unreachable") => apiErr
          case Some(reason) => errUnknown(reason)
          case None => InternalServerError(views.html.err.InternalServerError())
        }
        case _ => err500
      }
    }
  }
  def notFound(unknown: String): Action[AnyContent] = Action{
    implicit request: Request[AnyContent] => {
      err404
    }
  }

  def success(msg: String): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] => {
      msg match {
        case "signup" => Redirect("/account/login").flashing(flash("Signup Successfull!!!", "success"): _*)
        case _ => err404
      }
    }
  }

  def shutdown: Action[AnyContent] = Action {
    request => {
      val rpid = s"${sys.env("SCHOOLSCAPE_HOME")}student\\bin\\RUNNING_PID"
      if(Files.exists(Paths.get(rpid))){
        val pid = Files.readAllLines(Paths.get(rpid)).get(0)
        s"cmd /c taskkill /F /PID $pid".!!
      }
      System.exit(0)
      Ok("Shutting down...")
    }
  }
}
