/*
 * Copyright 2013 - 2020 Outworkers Ltd.
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
package com.outworkers.phantom.builder.query.prepared

import com.outworkers.phantom.PhantomSuite
import com.outworkers.phantom.tables._
import com.outworkers.phantom.dsl._
import com.outworkers.util.samplers._

class PreparedDeleteQueryTest extends PhantomSuite {

  override def beforeAll(): Unit = {
    super.beforeAll()
    val _ = database.recipes.createSchema()
    database.articlesByAuthor.createSchema()
  }

  it should "execute a prepared delete query" in {
    val recipe = gen[Recipe]

    val query = database.recipes.delete.where(_.url eqs ?).prepare()

    val chain = for {
      _ <- database.recipes.store(recipe).future()
      get <- database.recipes.select.where(_.url eqs recipe.url).one()
      _ <- query.bind(recipe.url).future()
      get2 <- database.recipes.select.where(_.url eqs recipe.url).one()
    } yield (get, get2)

    whenReady(chain) { case (initial, afterDelete) =>
      initial shouldBe defined
      initial.value shouldEqual recipe
      afterDelete shouldBe empty
    }
  }

  it should "execute an asynchronous prepared delete query" in {
    val recipe = gen[Recipe]

    val chain = for {
      query <- database.recipes.delete.where(_.url eqs ?).prepareAsync()
      _ <- database.recipes.store(recipe).future()
      get <- database.recipes.select.where(_.url eqs recipe.url).one()
      _ <- query.bind(recipe.url).future()
      get2 <- database.recipes.select.where(_.url eqs recipe.url).one()
    } yield (get, get2)

    whenReady(chain) { case (initial, afterDelete) =>
      initial shouldBe defined
      initial.value shouldEqual recipe
      afterDelete shouldBe empty
    }
  }

  it should "execute an asynchronous prepared delete query on a map column" in {
    val recipe = gen[Recipe]
    val keyToRemove = recipe.props.keys.headOption.value

    val chain = for {
      query <- database.recipes.deleteP(_.props(?)).where(_.url eqs ?).prepareAsync()
      _ <- database.recipes.store(recipe).future()
      _ <- query.bind(keyToRemove, recipe.url).future()
      res <- database.recipes.select.where(_.url eqs recipe.url).one()
    } yield res

    whenReady(chain) { result =>
      result shouldBe defined
      result.value.props shouldEqual (recipe.props - keyToRemove)
    }
  }

  it should "correctly execute a prepared delete query with 2 bound values" in {
    val (author, cat, article) = gen[(UUID, UUID, Article)]

    val query = database.articlesByAuthor.delete
      .where(_.category eqs ?)
      .and(_.author_id eqs ?)
      .prepare()

    val chain = for {
      _ <- database.articlesByAuthor.store(author, cat, article).future()
      get <- database.articlesByAuthor.select.where(_.category eqs cat).and(_.author_id eqs author).one()
      _ <- query.bind(cat, author).future()
      get2 <- database.articlesByAuthor.select.where(_.category eqs cat).and(_.author_id eqs author).one()
    } yield (get, get2)

    whenReady(chain) { case (initial, afterDelete) =>
      initial shouldBe defined
      initial.value shouldEqual article
      afterDelete shouldBe empty
    }
  }

  it should "correctly execute an asynchronous prepared delete query with 2 bound values" in {
    val (author, cat, article) = gen[(UUID, UUID, Article)]

    val chain = for {
      query <- database.articlesByAuthor.delete.where(_.category eqs ?).and(_.author_id eqs ?).prepareAsync()
      _ <- database.articlesByAuthor.store(author, cat, article).future()
      get <- database.articlesByAuthor.select.where(_.category eqs cat).and(_.author_id eqs author).one()
      _ <- query.bind(cat, author).future()
      get2 <- database.articlesByAuthor.select.where(_.category eqs cat).and(_.author_id eqs author).one()
    } yield (get, get2)

    whenReady(chain) { case (initial, afterDelete) =>
      initial shouldBe defined
      initial.value shouldEqual article
      afterDelete shouldBe empty
    }
  }

  it should "not compile invalid arguments sent to a store method" in {
    val (author, cat, article) = gen[(UUID, UUID, Article)]

    "database.articlesByAuthor.store(author, cat, article, cat)" shouldNot compile
  }

  it should "not compile invalid argument orders sent to store method" in {
    val (author, cat, article) = gen[(UUID, UUID, Article)]

    "database.articlesByAuthor.store(article, cat, author)" shouldNot compile
  }
}
