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
 */
package io.mycat.beans;

import io.mycat.SystemConfig;

/**
 * 代表一个数据存储节点的配置信息
 * @author wuzhihui
 *
 */
public class DataHostConfig {
    private String hostName;
    private String ip;
    private int port;
    private String url;
    private String user;
    private String password;
    private String defaultSchema;
    private long idleTimeout = SystemConfig.DEFAULT_IDLE_TIMEOUT; // 连接池中连接空闲超时时间
    private int maxCon;
    private int minCon;
    private String dbType;
    private int weight;				

    public DataHostConfig() {
        super();
    }

    public DataHostConfig(String hostName, String ip, int port, String user, String password,String defaultSchema) {
        super();
        this.hostName = hostName;
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.password = password;
        this.defaultSchema=defaultSchema;
    }

    public String getDefaultSchema() {
		return defaultSchema;
	}

	public void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getMaxCon() {
        return maxCon;
    }

    public void setMaxCon(int maxCon) {
        this.maxCon = maxCon;
    }

    public int getMinCon() {
        return minCon;
    }

    public void setMinCon(int minCon) {
        this.minCon = minCon;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "DataHostConfig [hostName=" + hostName + ", ip=" + ip + ", port=" + port + ", url=" + url + ", user="
				+ user + ", password=" + password + ", defaultSchema=" + defaultSchema + ", idleTimeout=" + idleTimeout
				+ ", maxCon=" + maxCon + ", minCon=" + minCon + ", dbType=" + dbType + ", weight=" + weight + "]";
	}

  

}