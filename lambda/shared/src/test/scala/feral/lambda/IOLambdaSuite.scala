/*
 * Copyright 2021 Typelevel
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

package feral.lambda

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all._
import munit.CatsEffectSuite

class IOLambdaSuite extends CatsEffectSuite {

  test("handler is correctly installed") {
    for {
      allocationCounter <- IO.ref(0)
      invokeCounter <- IO.ref(0)
      lambda <- IO {
        new IOLambda[String, String] {
          def handler = Resource
            .eval(allocationCounter.getAndUpdate(_ + 1))
            .as(_.event.map(Some(_)) <* invokeCounter.getAndUpdate(_ + 1))
        }
      }

      handler <- IO.fromFuture(IO(lambda.setupMemo)).map(_._2)

      _ <- ('0' to 'z')
        .map(_.toString)
        .toList
        .traverse(x => handler(x, mockContext).assertEquals(Some(x)))
      _ <- allocationCounter.get.assertEquals(1)
      _ <- invokeCounter.get.assertEquals(75)
    } yield ()
  }

  def mockContext = Context[IO]("", "", "", 0, "", "", "", None, None, IO.stub)

}
