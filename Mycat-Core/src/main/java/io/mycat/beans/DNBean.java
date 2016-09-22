/*
 * Copyright (c) 2016, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese
 * opensource volunteers. you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Any questions about this component can be directed to it's project Web address
 * https://code.google.com/p/opencloudb/.
 *
 */package io.mycat.beans;
/**
 * refer a database on a mysql replica
 * @author wuzhihui
 *
 */

public class DNBean {
private String database;
private String mysqlReplica;
public DNBean(String database, String mysqlReplica) {
	super();
	this.database = database;
	this.mysqlReplica = mysqlReplica;
}
public String getDatabase() {
	return database;
}
public void setDatabase(String database) {
	this.database = database;
}
public String getMysqlReplica() {
	return mysqlReplica;
}
public void setMysqlReplica(String mysqlReplica) {
	this.mysqlReplica = mysqlReplica;
}
@Override
public String toString() {
	return "DNBean [database=" + database + ", mysqlReplica=" + mysqlReplica + "]";
} 
	

}
