package io.github.smagical;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.jdbc.PreparedStatementLogger;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.ArrayTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.*;

/**
 * Hello world!
 *
 */
public class Test
{
    /**
     *
     * {@link Array#free()} in {@link ArrayTypeHandler#setNonNullParameter(PreparedStatement, int, Object, JdbcType)}
     *
     * When using this typehandler, results similar to the following will be triggered.
     *
     * @param args
     * @throws SQLException
     */
    public static void main( String[] args ) throws SQLException {


        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:default"
        );
        Array sqlArray = connection.createArrayOf("INTEGER",new Object[]{0,1,2});
        connection.createStatement().execute(
                "CREATE TABLE test(id INTEGER Array)"
        );

        PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO test VALUES (?)"
        );
        Log log =  new StdOutImpl(Test.class.getName());
         preparedStatement = PreparedStatementLogger.newInstance(
                preparedStatement,log,1
        );
        PreparedStatementLogger preparedStatementLogger =
                (PreparedStatementLogger) Proxy.getInvocationHandler(preparedStatement);
        preparedStatement.setArray(1,sqlArray);
        String sql = preparedStatementLogger.getPreparedStatement().toString();
        log.debug("################  free before");
        log.debug("Pay attention to the parameters  ==>" + sql);
        sqlArray.free();
        sql = preparedStatementLogger.getPreparedStatement().toString();
        log.debug("################### free after");
        log.debug("Pay attention to the parameters  ==>" + sql);
        log.debug("Pay attention to the printing of BaseJdbcLogger ↓↓↓↓↓");
        preparedStatement.execute();


        log.debug("\n");
        useArrayTypeHandler();
        connection.close();
    }

    public static void useArrayTypeHandler(){
        UnpooledDataSource dataSource = new UnpooledDataSource();
        dataSource.setDriver(org.h2.Driver.class.getName());
        dataSource.setUrl("jdbc:h2:mem:default");
        TransactionFactory transactionFactory = new JdbcTransactionFactory();

        Environment environment = new Environment("development", transactionFactory, dataSource);

        Configuration configuration = new Configuration(environment);
        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        configuration.setLogImpl(StdOutImpl.class);
        configuration.addMapper(TestMapper.class);
        SqlSessionFactory factory = builder.build(configuration);
        int w = factory.openSession().getMapper(TestMapper.class)
                .insert(new Integer[]{1,2});
        System.out.println("parameters is null.\n" +
                "Array.free() in ArrayTypeHandler." +
                "setNonNullParameter(PreparedStatement, int, Object, JdbcType)" +
                "\n When using this typehandler, " +
                "results similar to the following will be triggered.");

    }

    @Mapper
    static interface TestMapper{
        @Insert("INSERT INTO test VALUES(#{id,typeHandler=org.apache.ibatis.type.ArrayTypeHandler})")
        int insert(@Param("id") Integer[] id);
    }
}
