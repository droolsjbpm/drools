/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.test.util.db.internal;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import com.arjuna.ats.jta.common.jtaPropertyManager;
import org.apache.tomcat.dbcp.dbcp2.managed.BasicManagedDataSource;
import org.kie.test.util.db.PoolingDataSourceWrapper;

/**
 * Wrapper for actual Pooling Data Source provided by tomcat DBCP library. This class offers data source with
 * XA transactions and connection pooling capabilities.
 */
public final class PoolingDataSourceWrapperImpl implements PoolingDataSourceWrapper {

    private static final Logger logger = Logger.getLogger(PoolingDataSourceWrapperImpl.class.getSimpleName());

    private Properties driverProperties;
    private String uniqueName;
    private String className;
    private BasicManagedDataSource managedDataSource;
    private DatabaseProvider databaseProvider;

    /**
     * This constructor creates a PoolingDataSource using internally {@link BasicManagedDataSource} with its default
     * pooling parameters.
     * @param uniqueName Data Source unique name. Serves for registration to JNDI.
     * @param dsClassName Name of a class implementing {@link XADataSource} available in a JDBC driver on a classpath.
     * @param driverProperties Properties of a database driver.
     */
    public PoolingDataSourceWrapperImpl(final String uniqueName,
                                        final String dsClassName,
                                        final Properties driverProperties) {
        this(uniqueName, dsClassName, driverProperties, new Properties());
    }

    /**
     * This constructor creates a PoolingDataSource using internally {@link BasicManagedDataSource}.
     * @param uniqueName Data Source unique name. Serves for registration to JNDI.
     * @param dsClassName Name of a class implementing {@link XADataSource} available in a JDBC driver on a classpath.
     * @param driverProperties Properties of a database driver.
     * @param poolingProperties Properties of a pooling data source. See {@link BasicManagedDataSource} for details.
     */
    public PoolingDataSourceWrapperImpl(final String uniqueName,
                                        final String dsClassName,
                                        final Properties driverProperties,
                                        final Properties poolingProperties) {
        this.uniqueName = uniqueName;
        this.className = dsClassName;
        this.driverProperties = copy(driverProperties);
        this.databaseProvider = DatabaseProvider.fromDriverClassName(className);

        final XADataSource xaDataSource = createXaDataSource();

        final TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        final TransactionSynchronizationRegistry tsr =
                jtaPropertyManager.getJTAEnvironmentBean().getTransactionSynchronizationRegistry();

        Properties sanitizedPoolingProperties = copy(poolingProperties);
        sanitizedPoolingProperties.setProperty("username", driverProperties.getProperty("user"));
        sanitizedPoolingProperties.setProperty("password", driverProperties.getProperty("password"));

        managedDataSource = (BasicManagedDataSource)
                PoolingDataSourceFactory.createPoolingDataSource(tm, xaDataSource, tsr, sanitizedPoolingProperties);

        try {
            InitialContext initContext = new InitialContext();

            initContext.rebind(uniqueName, managedDataSource);
            initContext.rebind("java:comp/UserTransaction", com.arjuna.ats.jta.UserTransaction.userTransaction());
            initContext.rebind("java:comp/TransactionManager", tm);
            initContext.rebind("java:comp/TransactionSynchronizationRegistry", tsr);
        } catch (NamingException e) {
            logger.warning("No InitialContext available, resource won't be accessible via lookup");
        }
    }

    private Properties copy(final Properties props) {
        Properties copiedProperties = new Properties();
        copiedProperties.putAll(props);
        return copiedProperties;
    }

    private XADataSource createXaDataSource() {
        XADataSource xaDataSource;
        try {
            xaDataSource = (XADataSource) Class.forName(className).newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }

        if (databaseProvider == DatabaseProvider.H2) {
            invokeMethodOnXADataSource(xaDataSource, "setUser", getUsernameFromDriverProperties(), String.class);
            invokeMethodOnXADataSource(xaDataSource, "setPassword", getPasswordFromDriverProperties(), String.class);
        }

        if (databaseProvider != DatabaseProvider.DB2 && databaseProvider != DatabaseProvider.SYBASE) {
            setupUrlOnXADataSource(xaDataSource);
        } else {
            invokeMethodOnXADataSource(xaDataSource, "setServerName", driverProperties.getProperty("serverName"), String.class);
            invokeMethodOnXADataSource(xaDataSource, "setDatabaseName", driverProperties.getProperty("databaseName"), String.class);
            if (databaseProvider == DatabaseProvider.DB2) {
                invokeMethodOnXADataSource(xaDataSource, "setDriverType", 4, int.class);
                invokeMethodOnXADataSource(xaDataSource, "setPortNumber", Integer.parseInt(driverProperties.getProperty("portNumber")), int.class);
                invokeMethodOnXADataSource(xaDataSource, "setResultSetHoldability", Integer.parseInt(driverProperties.getProperty("ResultSetHoldability")), int.class);
                invokeMethodOnXADataSource(xaDataSource, "setDowngradeHoldCursorsUnderXa", Boolean.parseBoolean(driverProperties.getProperty("DowngradeHoldCursorsUnderXa")), boolean.class);
            } else if (databaseProvider == DatabaseProvider.SYBASE) {
                invokeMethodOnXADataSource(xaDataSource, "setPortNumber", Integer.parseInt(driverProperties.getProperty("portNumber")), int.class);
                invokeMethodOnXADataSource(xaDataSource, "setPassword", driverProperties.getProperty("password"), String.class);
                invokeMethodOnXADataSource(xaDataSource, "setUser", driverProperties.getProperty("user"), String.class);
            }
        }

        return xaDataSource;
    }

    private void setupUrlOnXADataSource(final XADataSource xaDataSource) {
        String url = driverProperties.getProperty("url", driverProperties.getProperty("URL"));
        try {
            invokeMethodOnXADataSource(xaDataSource, "setUrl", url, String.class);
        } catch (UnsupportedOperationException outerException) {
            logger.info("Unable to find \"setUrl\" method in db driver JAR. Trying \"setURL\" ");
            try {
                invokeMethodOnXADataSource(xaDataSource, "setURL", url, String.class);
            } catch (UnsupportedOperationException innerException) {
                logger.info("Driver does not support setURL and setUrl method.");
                throw innerException;
            }
        }
    }

    private void invokeMethodOnXADataSource(XADataSource dataSource, String methodName, Object parameter, Class type) {
        try {
            dataSource.getClass().getMethod(methodName, new Class[]{type}).invoke(dataSource, parameter);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException ex) {
            throw new UnsupportedOperationException("Unable to invoke method \"" + methodName + "\" on XADataSource.");
        }
    }

    private String getUsernameFromDriverProperties() {
        return driverProperties.getProperty("user");
    }

    private String getPasswordFromDriverProperties() {
        return driverProperties.getProperty("password");
    }

    public void close() {
        try {
            managedDataSource.close();
            new InitialContext().unbind(uniqueName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public String getClassName() {
        return className;
    }

    public Connection getConnection() throws SQLException {
        return managedDataSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return managedDataSource.getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return managedDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return managedDataSource.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return managedDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        managedDataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        managedDataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return managedDataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return managedDataSource.getParentLogger();
    }
}