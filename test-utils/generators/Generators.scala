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

import org.scalacheck.Arbitrary.*
import org.scalacheck.Gen.*
import org.scalacheck.{Gen, Shrink}

trait Generators extends ModelGenerators with UserAnswersGenerator with PageGenerators with UserAnswersEntryGenerators {

  implicit val dontShrink: Shrink[String] = Shrink.shrinkAny

  def genIntersperseString(gen: Gen[String],
                           value: String,
                           frequencyV: Int = 1,
                           frequencyN: Int = 10): Gen[String] = {

    val genValue: Gen[Option[String]] = Gen.frequency(frequencyN -> None, frequencyV -> Gen.const(Some(value)))

    for {
      seq1 <- gen
      seq2 <- Gen.listOfN(seq1.length, genValue)
    } yield {
      seq1.toSeq.zip(seq2).foldLeft("") {
        case (acc, (n, Some(v))) =>
          acc + n + v
        case (acc, (n, _)) =>
          acc + n
      }
    }
  }

  def intsInRangeWithCommas(min: Int, max: Int): Gen[String] = {
    val numberGen = choose[Int](min, max).map(_.toString)
    genIntersperseString(numberGen, ",")
  }

  def intsLargerThanMaxValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x > Int.MaxValue)

  def intsSmallerThanMinValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x < Int.MinValue)

  def nonNumerics: Gen[String] =
    alphaStr suchThat (_.size > 0)

  def decimals: Gen[String] =
    arbitrary[BigDecimal]
      .suchThat(_.abs < Int.MaxValue)
      .suchThat(!_.isValidInt)
      .map("%f".format(_))

  def intsBelowValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ < value)

  def intsAboveValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ > value)

  def intsOutsideRange(min: Int, max: Int): Gen[Int] =
    arbitrary[Int] suchThat (x => x < min || x > max)

  def nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat(_.nonEmpty)
      .suchThat(_ != "true")
      .suchThat(_ != "false")

  def nonEmptyString: Gen[String] =
    arbitrary[String] suchThat (_.nonEmpty)

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars <- listOfN(length, Gen.alphaNumChar)
    } yield chars.mkString

  def stringsLongerThan(minLength: Int): Gen[String] = for {
    maxLength <- (minLength * 2).max(100)
    length <- Gen.chooseNum(minLength + 1, maxLength)
    chars <- listOfN(length, arbitrary[Char])
  } yield chars.mkString

  def stringsExceptSpecificValues(excluded: Seq[String]): Gen[String] =
    nonEmptyString suchThat (!excluded.contains(_))

  def oneOf[T](xs: Seq[Gen[T]]): Gen[T] =
    if (xs.isEmpty) {
      throw new IllegalArgumentException("oneOf called on empty collection")
    } else {
      val vector = xs.toVector
      choose(0, vector.size - 1).flatMap(vector(_))
    }

  def unsafeInputs: Gen[Char] = Gen.oneOf(
    Gen.const('<'),
    Gen.const('>'),
    Gen.const('='),
    Gen.const('|')
  )

  def unsafeInputsWithMaxLength(maxLength: Int): Gen[String] = {
    (for {
      length <- choose(2, maxLength)
      invalidChar <- unsafeInputs
      validChars <- listOfN(length - 1, unsafeInputs)
    } yield (validChars :+ invalidChar).mkString).suchThat(_.trim.nonEmpty)
  }

  def alphaNumStringWithLength(minLength: Int, maxLength: Int): Gen[String] = {
    (
      for {
        length <- choose(minLength, maxLength)
        chars <- listOfN(length, Gen.alphaNumChar)
      } yield chars.mkString).suchThat(_.trim.nonEmpty)
  }


  def commonFieldString(maxLength: Int): Gen[String] = (for {
    length <- choose(1, maxLength)
    chars <- listOfN(length, commonFieldSafeInputs)
  } yield chars.mkString).suchThat(_.trim.nonEmpty).retryUntil(_.matches(retryUntilString))

  val retryUntilString = """^(?!^[’'"])(?:[A-Za-z0-9À-ÿ \!\)\(.,_/’'"&-])(?<![’'"]$)$"""

  private def commonFieldSafeInputs: Gen[Char] = Gen.oneOf(
    Gen.alphaNumChar,
    Gen.oneOf('À' to 'ÿ'),
    Gen.const('.'),
    Gen.const(','),
    Gen.const('/'),
    Gen.const('’'),
    Gen.const('\''),
    Gen.const('"'),
    Gen.const('_'),
    Gen.const('&'),
    Gen.const(' '),
    Gen.const('\'')
  )

  def validEmails: Gen[String] = {
    for {
      length <- choose(5, 12)
      user <- listOfN(length, Gen.alphaNumChar)
      domain <- listOfN(length, Gen.alphaNumChar)
      suffix <- Gen.oneOf(Seq(".com", ".co.uk", ".gov.uk"))
    } yield s"${user.mkString}@${domain.mkString}${suffix.mkString}"
  }

  def safeInputsWithMaxLength(maxLength: Int): Gen[String] = (for {
    length <- choose(1, maxLength)
    chars <- listOfN(length, safeInputs)
  } yield chars.mkString).suchThat(_.trim.nonEmpty)

  def safeInputs: Gen[Char] = Gen.oneOf(
    Gen.alphaNumChar,
    Gen.const('"'),
    Gen.const('\''),
    Gen.const('.'),
    Gen.const(','),
    Gen.const('/'),
    Gen.const('-'),
    Gen.const('_'),
    Gen.const(' '),
    Gen.const('&'),
    Gen.const('’'),
    Gen.const('('),
    Gen.const(')'),
    Gen.const('!'),
    Gen.oneOf('À' to 'ÿ')
  )
}
