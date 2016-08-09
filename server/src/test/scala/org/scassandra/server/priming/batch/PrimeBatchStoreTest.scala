/*
 * Copyright (C) 2016 Christopher Batey and Dogan Narinc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scassandra.server.priming.batch

import org.scalatest.{FunSpec, Matchers}
import org.scassandra.server.cqlmessages._
import org.scassandra.server.priming._
import org.scassandra.server.priming.json.{Success, WriteTimeout}
import org.scassandra.server.priming.query.Then

class PrimeBatchStoreTest extends FunSpec with Matchers {
  describe("Recording and matching primes") {
    it("Should match a single query") {
      val underTest = new PrimeBatchStore()

      underTest.record(BatchPrimeSingle(
        BatchWhen(List(BatchQueryPrime("select * blah", QueryKind))),
        Then(result = Some(Success))))

      val prime = underTest.findPrime(BatchExecution(Seq(BatchQuery("select * blah", QueryKind)), ONE, LOGGED))

      prime should equal(Some(BatchPrime(SuccessResult, None)))
    }

    it("Should fail if single query does not match") {
      val underTest = new PrimeBatchStore()

      underTest.record(BatchPrimeSingle(
        BatchWhen(List(BatchQueryPrime("select * blah", QueryKind))),
        Then(result = Some(Success))))

      val prime = underTest.findPrime(BatchExecution(Seq(BatchQuery("I am do different", QueryKind)), ONE, LOGGED))

      prime should equal(None)
    }

    it("Should fail if any query does not match") {
      val underTest = new PrimeBatchStore()

      underTest.record(BatchPrimeSingle(
        BatchWhen(List(BatchQueryPrime("select * blah", QueryKind),
          BatchQueryPrime("select * wah", PreparedStatementKind))),
        Then(result = Some(Success))))

      val prime = underTest.findPrime(BatchExecution(Seq(BatchQuery("select * blah", QueryKind)), ONE, LOGGED))

      prime should equal(None)
    }

    it("Should match if all queries match") {
      val underTest = new PrimeBatchStore()

      underTest.record(BatchPrimeSingle(
        BatchWhen(List(BatchQueryPrime("select * blah", QueryKind),
          BatchQueryPrime("select * wah", PreparedStatementKind))),
        Then(result = Some(Success))))

      val prime = underTest.findPrime(BatchExecution(Seq(
        BatchQuery("select * blah", QueryKind),
        BatchQuery("select * wah", PreparedStatementKind)), ONE, LOGGED))

      prime should equal(Some(BatchPrime(SuccessResult, None)))
    }

    it("Should match on consistency") {
      val underTest = new PrimeBatchStore()

      underTest.record(BatchPrimeSingle(
        BatchWhen(List(BatchQueryPrime("select * blah", QueryKind),
          BatchQueryPrime("select * wah", PreparedStatementKind)),
          Some(List(TWO))),
        Then(result = Some(Success))))

      val prime = underTest.findPrime(BatchExecution(Seq(
        BatchQuery("select * blah", QueryKind),
        BatchQuery("select * wah", PreparedStatementKind)), ONE, LOGGED))

      prime should equal(None)
    }

    it("Should match on batch type - no match") {
      val underTest = new PrimeBatchStore()

      underTest.record(BatchPrimeSingle(
        BatchWhen(List(BatchQueryPrime("select * blah", QueryKind)), batchType = Some(COUNTER)),
        Then(result = Some(Success))))

      val prime = underTest.findPrime(BatchExecution(Seq(BatchQuery("select * blah", QueryKind)), ONE, LOGGED))

      prime should equal(None)
    }

    it("Should record a batch WriteTimeout with default configuration") {
      val underTest = new PrimeBatchStore()

      underTest.record(BatchPrimeSingle(
        BatchWhen(List(BatchQueryPrime("select * blah", QueryKind))),
        Then(result = Some(WriteTimeout))))

      val prime = underTest.findPrime(BatchExecution(Seq(BatchQuery("select * blah", QueryKind)), ONE, LOGGED))

      prime should equal(Some(BatchPrime(WriteRequestTimeoutResult(), None)))
    }

    it("Should record a batch WriteTimeout with custom configuration") {
      val underTest = new PrimeBatchStore()

      val config: Map[String, String] = Map(ErrorConstants.WriteType -> WriteType.BATCH.toString)
      underTest.record(BatchPrimeSingle(
        BatchWhen(List(BatchQueryPrime("select * blah", QueryKind))),
        Then(result = Some(WriteTimeout), config = Option(config))))

      val prime = underTest.findPrime(BatchExecution(Seq(BatchQuery("select * blah", QueryKind)), ONE, LOGGED))

      prime should equal(Some(BatchPrime(WriteRequestTimeoutResult(writeType = WriteType.BATCH), None)))
    }
  }
}
