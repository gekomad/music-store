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


/*
{
  "took": 16,
  "timed_out": false,
  "_shards": {
      "total": 5,
      "successful": 5,
      "skipped": 0,
      "failed": 0
  },
  "hits": {
      "total": 2,
      "max_score": 0,
      "hits": []
  },
  "aggregations": {
      "avg_rating": {
          "value": 3928
      }
  }



}


* */

final case class AvgRating(value: Double)

final case class Aggregations(avg_rating: AvgRating)

final case class Aggregations1(took: Int,
                               timed_out: Boolean,
                               _shards: Shards,
                               hits: MainHits[Int],
                               aggregations: Aggregations)
  extends ElasticSearchTrait[Int]



