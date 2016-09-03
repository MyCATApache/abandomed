/*
 * Copyright (c) 2013, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
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
package io.mycat.backend.mysql;

import java.util.concurrent.ThreadLocalRandom;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.DHReplicatSet;
import io.mycat.backend.DHSource;
import io.mycat.beans.DataHostConfig;

/**
 * MySQL主从的ReplicatSet 
 * @author wuzhihui
 *
 */
public class MySQLMSReplicatSet extends DHReplicatSet {
private int writeDHSourceIndex=0;	
private DHSource[] dhSources;	
public MySQLMSReplicatSet(String name,DataHostConfig[] masterAndSlaves) {
		super(name);
		dhSources=new DHSource[masterAndSlaves.length];
		for(int i=0;i<masterAndSlaves.length;i++)
		{
			dhSources[i]=new MySQLDataSource(SQLEngineCtx.INSTANCE().getBackendMySQLConFactory(), masterAndSlaves[i]);
			dhSources[i].initSource();
		}
		writeDHSourceIndex=0;
		
	}

private int switchType=0;

	 
	@Override
	public boolean supportAutoSwitch() {
		 
		return switchType!=0;
	}

	@Override
	public DHSource getCurWriteDH() {
	return 	dhSources[writeDHSourceIndex];
	}

	@Override
	public DHSource getLBReadDH() {
		return 	dhSources[ThreadLocalRandom.current().nextInt()/dhSources.length];
	}

	@Override
	public boolean switchWriteDH(String dhName) {
		// TODO Auto-generated method stub
		return false;
	}

}
