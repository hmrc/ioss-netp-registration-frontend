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

package forms.mappings

import config.CurrencyFormatter
import java.time.LocalDate
import forms.validation.Validation.utrRegex
import play.api.data.validation.{Constraint, Invalid, Valid}

trait Constraints {

  protected def firstError[A](constraints: Constraint[A]*): Constraint[A] =
    Constraint {
      input =>
        constraints
          .map(_.apply(input))
          .find(_ != Valid)
          .getOrElse(Valid)
    }

  protected def minimumValue[A](minimum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input >= minimum) {
          Valid
        } else {
          Invalid(errorKey, minimum)
        }
    }

  protected def maximumValue[A](maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, maximum)
        }
    }

  protected def inRange[A](minimum: A, maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input >= minimum && input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, minimum, maximum)
        }
    }

  protected def regexp(regex: String, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.matches(regex) =>
        Valid
      case _ =>
        Invalid(errorKey, regex)
    }

  protected def maxLength(maximum: Int, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length <= maximum =>
        Valid
      case _ =>
        Invalid(errorKey, maximum)
    }

  protected def minLength(minimum: Int, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length >= minimum =>
        Valid
      case _ =>
        Invalid(errorKey, minimum)
    }

  protected def maxDate(maximum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isAfter(maximum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def minDate(minimum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isBefore(minimum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def nonEmptySet(errorKey: String): Constraint[Set[_]] =
    Constraint {
      case set if set.nonEmpty =>
        Valid
      case _ =>
        Invalid(errorKey)
    }

  protected def minimumCurrency(minimum: BigDecimal, errorKey: String)(implicit ev: Ordering[BigDecimal]): Constraint[BigDecimal] =
    Constraint {
      input =>
        if (input >= minimum) {
          Valid
        } else {
          Invalid(errorKey, CurrencyFormatter.currencyFormat(minimum))
        }
    }

  protected def maximumCurrency(maximum: BigDecimal, errorKey: String)(implicit ev: Ordering[BigDecimal]): Constraint[BigDecimal] =
    Constraint {
      input =>
        if (input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, CurrencyFormatter.currencyFormat(maximum))
        }
    }

  protected def ninoConstraint: Constraint[String] = Constraint("constraints.nino") { input =>
    val normalized = input.replaceAll("\\s", "").toUpperCase

    val validNinoRegex = "^[A-Z]{2}\\d{6}[A-D]?$"

    val specialCharRegex = "^[A-Z0-9]*$"

    if (!normalized.matches(specialCharRegex)) {
      Invalid("clientsNinoNumber.error.nino.special.character")
    } else if (normalized.length != 9) {
      Invalid("clientsNinoNumber.error.nino.length")
    } else if (!normalized.matches(validNinoRegex)) {
      Invalid("clientsNinoNumber.error.nino.invalid")
    } else {
      Valid
    }
  }

  protected def utrConstraint(errorKey: String): Constraint[String] =
    Constraint { input =>
      val normalized = input.replaceAll("[^A-Za-z0-9]", "").toLowerCase

      if (utrRegex.pattern.matcher(normalized).matches()) {
        Valid
      } else {
        Invalid(errorKey)
      }
    }

  protected def utrLengthConstraint(errorKey: String): Constraint[String] =
    Constraint { input =>
      val digitsOnly = input.replaceAll("[^0-9]", "")

      if (digitsOnly.length == 10 || digitsOnly.length == 13) {
        Valid
      } else {
        Invalid(errorKey)
      }
  }

  protected def vatNumberConstraint(errorKey: String): Constraint[String] =
    Constraint { input =>
      val normalized = input.toUpperCase.replaceAll("\\s", "").stripPrefix("GB")
      val digitsOnly = normalized.replaceAll("[^0-9]", "")

      if (digitsOnly.matches("^\\d{9}$")) {
        Valid
      } else {
        Invalid(errorKey)
      }
    }
}
