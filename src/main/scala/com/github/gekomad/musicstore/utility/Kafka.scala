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

package com.github.gekomad.musicstore.utility

import kafka.admin.AdminUtils
import kafka.utils.ZkUtils
import org.slf4j.{Logger, LoggerFactory}

object Kafka {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  def createTopic(topic: String, zookeeperHosts: String, partitionSize: Int, replicationCount: Int = 1, connectionTimeoutMs: Int = 10000, sessionTimeoutMs: Int = 10000, prop: java.util.Properties = new java.util.Properties): Unit = {
    val zkUtils = ZkUtils.apply(zookeeperHosts, sessionTimeoutMs, connectionTimeoutMs, isZkSecurityEnabled = false)
    AdminUtils.createTopic(zkUtils, topic, partitionSize, replicationCount, prop)
    zkUtils.close()
  }

  def deleteTopic(topic: String, zookeeperHosts: String, connectionTimeoutMs: Int = 10000, sessionTimeoutMs: Int = 10000): Unit = {
    val zkUtils = ZkUtils.apply(zookeeperHosts, sessionTimeoutMs, connectionTimeoutMs, isZkSecurityEnabled = false)
    AdminUtils.deleteTopic(zkUtils, topic)
    zkUtils.close()
  }
}
