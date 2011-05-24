Direct Download 0.7.0-BETA
=================
- jircd-server-0.7.0-BETA.jar_ (core api)

Maven Repository
===========
jircd can be consumed in a Maven build. Multiple artifacts are available, depending on the application's requirements::

	<repositories>
		<repository>
			<id>jp.kurusugawa.jircd</id>
			<url>https://github.com/squld/jircd/raw/master/repository/</url>
		</repository>
	</repositories>

	<dependencies>
		<!-- Logger Libraries -->
		<dependency>
			<groupId>ch.qos.logback</groupId>
			<artifactId>logback-classic</artifactId>
			<version>0.9.28</version>
		</dependency>
		<dependency>
			<groupId>org.slf4j</groupId>
			<artifactId>log4j-over-slf4j</artifactId>
			<version>1.6.1</version>
		</dependency>

		<!-- GeoIP Library -->
		<dependency>
			<groupId>org.dspace.dependencies</groupId>
			<artifactId>dspace-geoip</artifactId>
			<version>1.2.3</version>
		</dependency>

		<!-- jircd-server -->
		<dependency>
			<groupId>jircd</groupId>
			<artifactId>jircd-server</artifactId>
			<version>0.7.0-BETA</version>
		</dependency>
	</dependencies>


About
===
original project link http://j-ircd.sourceforge.net/

License (GPL)
========

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

.. _jircd-server-0.7.0-BETA.jar: https://github.com/squld/jircd/raw/master/repository/jircd/jircd-server/0.7.0-BETA/jircd-server-0.7.0-BETA.jar