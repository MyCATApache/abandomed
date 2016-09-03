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
 */package io.mycat.backend;
/**
 * 表示一组DataHost的数据复制关系，如主从，主主
 * @author wuzhihui
 *
 */
public abstract class DHReplicatSet {
public static final int M_S_REP=0;
public static final int M_M_REP=1;
private int repType;
    private final String name;
    
	public DHReplicatSet( String name) {
		super();
	this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getRepType() {
	return repType;
}

	/**
	 * 是否支持主宕机后的自动切换能力
	 * @return
	 */
public abstract boolean supportAutoSwitch();

/**
 * 切换到新的写的DHSource
 * @param dhName
 * @return
 */
public abstract boolean switchWriteDH(String dhName);
/**
 * 得到当前用于写的DHSource
 * @return DHSource
 */
public abstract DHSource getCurWriteDH();
/**
 *  得到当前用于读的DHSource（负载均衡模式，如果支持）
 * @return DHSource
 */
public abstract DHSource getLBReadDH();
public void setRepType(int repType) {
	this.repType = repType;
}

	
	
	 

}
