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

package forms.validation

object Validation {


  val websitePattern = """^(https?://)((([a-z\d]([a-z\d-]*[a-z\d])*)\.)+[a-z]{2,})(\:\d+)?(\/[-a-z\d%_.~+]*)*(\?[;&a-z\d%_.~+=-]*)?(\#[-a-z\d_]*)?"""
  val commonTextPattern = """^(?!^[’'"])(?:[A-Za-z0-9À-ÿ \!\)\(.,_/’'"&-]|[’'"](?=[A-Za-z0-9À-ÿ \!\)\(.,_/’'"&-]))*[A-Za-z0-9À-ÿ \!\)\(.,_/’'"&-](?<![’'"]$)$"""
  val postcodePattern = """^[A-Za-z0-9 ]{0,100}$"""
  val alphaNumericWithSpace = """^[a-zA-Z0-9 ]+$"""
  val utrRegex = """^(k?\d{10,13}|(\d{10,13})k)$""".r

}
