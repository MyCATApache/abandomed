package io.mycat.net2;

import java.net.URL;
import java.util.List;

import org.junit.Test;

import io.mycat.ConfigLoader;
import io.mycat.beans.MySQLRepBean;
import io.mycat.beans.SchemaBean;
import io.mycat.beans.ShardingRuleBean;
import junit.framework.Assert;

public class TestParseDatasource {

	@Test
	public void  TestDatasource() {
		URL datasourceURL=ConfigLoader.class.getResource("/datasource.xml");
		List<MySQLRepBean> allReps=ConfigLoader.loadMySQLRepBean(datasourceURL.toString());
		Assert.assertEquals(2, allReps.size());
		Assert.assertEquals(2, allReps.get(0).getMysqls().size());
		Assert.assertEquals(2, allReps.get(1).getMysqls().size());
	}
	@Test
	public void  TestShardingRule() {
		URL datasourceURL=ConfigLoader.class.getResource("/sharding-rule.xml");
		List<ShardingRuleBean> allBeans=ConfigLoader.loadShardingRules(datasourceURL.toString());
		Assert.assertEquals(1, allBeans.size());
		Assert.assertEquals(2, allBeans.get(0).getParams().size());
		Assert.assertEquals("sharding-by-enum.txt", allBeans.get(0).getParams().get("mapFile"));
		
		
	}
	
	@Test
	public void  TestSheamBeans() {
		URL datasourceURL=ConfigLoader.class.getResource("/schema.xml");
		List<SchemaBean> allBeans=ConfigLoader.loadSheamBeans(datasourceURL.toString());
		Assert.assertEquals(2, allBeans.size());
		Assert.assertEquals(0, allBeans.get(0).getTableDefBeans().size());
		Assert.assertEquals(1, allBeans.get(1).getTableDefBeans().size());
		 
		
		
	}
	
	
}
