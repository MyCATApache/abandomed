# Mycat-Core
Mycat 2.0的核心组件之一，正在重新设计和演化
目前参与的人为 
   Leader.us以及Java高端培训一期和二期培训同学，希望更多高手参与

入口类：[io.mycat.net2.mysql.MockMySQLServer]

入口类中的静态方法快配置了一个DB node，目前大部分报文会直接转发到这个node上进行查询，返回的报文直接传给client
DBHostConfig config = new DBHostConfig("host1", "127.0.0.1", 3306, "mysql", "root", "123456");
        config.setMaxCon(10);
        PhysicalDatasource dbSource = new MySQLDataSource(config, false);
        PhysicalDBPool dbPool = new PhysicalDBPool("host1", new PhysicalDatasource[] { dbSource }, new HashMap<>());
        PhysicalDBNode dbNode = new PhysicalDBNode("host1", "mysql", dbPool);
        
本机要启动一个MySQL Server，用户名root,密码 123456 ，如上面代码所示，即可连接和学习测试
监听端口：8066

目前MockServer只支持登录认证、大部分CRUD语句、exit

##### 登录：mysql -uroot -proot -P 8066 （是判断用户名是否为root）

##### 查询

##### exit  释放连接