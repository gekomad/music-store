/*
    Copyright (C) Giuseppe Cannella

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.github.gekomad.musicstore.model.json.elasticsearch


final case class Hits[A](
                          _index: String,
                          _type: String,
                          _id: String,
                          _score: Float,
                          _routing: Option[String],
                          _source: A
                        )

final case class MainHits[A](
                              total: Int,
                              max_score: Float,
                              hits: List[Hits[A]]
                            )

final case class Shards(
                         total: Int,
                         successful: Int,
                         skipped: Int,
                         failed: Int
                       )

final case class ElasticSearchTemplate[A](
                                           took: Int,
                                           timed_out: Boolean,
                                           _shards: Shards,
                                           hits: MainHits[A]
                                         )