package com.smarto.rest.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.ValidConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OracleAdapter {

    private PoolDataSource pds;

    private String connectionString;
    private String userName;
    private String password;
    private static final Logger Log = LoggerFactory.getLogger(OracleAdapter.class);
    private final static String POOL_NAME = "oraConnPool";

    //Internal properties
    private final static String CURSOR_COLUMNS_PROP = "i#cursor_columns";
    private final static String AUTH_LOGIN = "i#auth_login";
    private final static String AUTH_CAR_ID = "i#auth_car_id";
    private final static String HTTP_ERROR_CODE = "i#http_error_code";
    private final static String ERROR_MESSAGE = "i#error_message";

    OracleAdapter(String connectionString, String userName, String password) {
        this.connectionString = connectionString;
        this.userName = userName;
        this.password = password;
    }
    //Init connection pool
    void initPool() {

        final int MAX_POOL_SIZE = 20;

        try {

            UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();

            pds = PoolDataSourceFactory.getPoolDataSource();
            pds.setConnectionPoolName(POOL_NAME);
            pds.setMinPoolSize(1);
            pds.setMaxPoolSize(MAX_POOL_SIZE);
            pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
            pds.setURL(connectionString);
            pds.setUser(userName);
            pds.setPassword(password);
            pds.setValidateConnectionOnBorrow(true);

            mgr.createConnectionPool((UniversalConnectionPoolAdapter) pds);
            mgr.startConnectionPool(POOL_NAME);

            Log.info("Connection to Oracle " + pds.getURL() + " successful");

        } catch (Exception e) {
            Log.error("Error creating pool", e);
        }

    }

    String getConnectionString() { return connectionString; }

    String getUserName() { return userName; }

    String getPassword() { return password; }

    private Connection getConnection() throws SQLException {
        return pds.getConnection();
    }


    private void returnConnection(Connection conn) {
        try {
            conn.close();
        } catch (java.sql.SQLException e) {
            Log.info("Connection closing failed: " + e.getMessage());
        }
    }

    boolean isValidConnection() {
        Connection oraConn = null;
        try {
            oraConn = getConnection();
            return ((ValidConnection) oraConn).isValid();
        } catch (SQLException e) {
            Log.error("Connection is not valid: " + e.getMessage());
            return false;
        } finally {
            if (oraConn != null) returnConnection(oraConn);
        }
    }

    void disconnect() {

        try {

            UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
            mgr.stopConnectionPool(POOL_NAME);
            mgr.destroyConnectionPool(POOL_NAME);

        } catch (Exception e) {
            Log.error("Unsuccessful disconnect from oracle: ", e);
        }

    }

    private void reconnect() {
        try {

            UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
            mgr.stopConnectionPool(POOL_NAME);
            mgr.startConnectionPool(POOL_NAME);

        } catch (Exception e) {
            Log.error("Unsuccessful reconnection to oracle: ", e);
        }

    }

    Map<String, String> getSystemParams() {

        Map<String, String> systemParams = new HashMap<>();
        Connection oraConn;

        try {
            oraConn = getConnection();
        } catch (SQLException e) {
            Log.error("Unsuccessful connection while execute reloadDbParams: " + e.getMessage());
            return null;
        }

        CallableStatement stmt = null;
        ResultSet rs = null;
        try {
            //Load system parameters

            String query = "begin ? := smarto.prv_rest_api.get_param_list; end;";
            stmt = oraConn.prepareCall(query);
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();
            rs = (ResultSet) stmt.getObject(1);

            if (rs != null)
                while (rs.next()) {
                    systemParams.put(rs.getString("PARAM_CODE"), rs.getString("PARAM_VALUE"));
                }

            return systemParams;

        } catch (Exception e) {
            Log.error("Unsuccessful execution of smarto.prv_rest_api.param_list: " + e.getMessage());
            return null;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (Exception e) {
                Log.error("Cannot close rs or stmt", e);
            }
            returnConnection(oraConn);
        }

    }

    Response getResponseJson(JsonObject inputJson, boolean firstAttempt, List<String> rowsArray, boolean addTotalCountHeader) throws Exception {

        //Init DB connection
        Connection oraConn;
        try {
            oraConn = getConnection();
        } catch (SQLException e) {
            Log.error("Unsuccessful connection while execute getResponseJson:" + e.getMessage());
            return Response.newErrInstance(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        //Thread.sleep(10000);
        CallableStatement stmt = null;
        ResultSet rs = null;
        String query = "begin ? := smarto.prv_rest_api.get_response(?, ?, ?); end;";

        try {

            ArrayDescriptor desc = ArrayDescriptor.createDescriptor("SMARTO.PRV_VARCHAR4000_TABLE_T", oraConn);
            ARRAY bindArray = new ARRAY(desc, oraConn, rowsArray.toArray());

            stmt = oraConn.prepareCall(query);
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.setString(2, inputJson.toString());
            stmt.registerOutParameter(3, OracleTypes.VARCHAR);
            stmt.setArray(4, bindArray);

            stmt.execute();
            rs = (ResultSet) stmt.getObject(1);
            String outParams = (String) stmt.getObject(3);

            JsonElement resultJson;
            JsonObject outParamsJson = null;

            if (outParams != null)
                outParamsJson = Conf.getJsonParser().parse(outParams).getAsJsonObject();

            Map<String, String> headers = new HashMap<>();
            if (rs != null) {

                String[] uriItems = inputJson.get("uri").getAsString().split("/");
                String[] columns = null;

                if (outParamsJson != null && outParamsJson.has(CURSOR_COLUMNS_PROP)) {
                    columns = outParamsJson.get(CURSOR_COLUMNS_PROP).getAsString().split(",");
                }

                ObjectResultSetAdapter resultSetTypeAdapter = new ObjectResultSetAdapter(
                        Utils.getAsInt(inputJson, "first_rec"),
                        Utils.getAsInt(inputJson, "rec_count"),
                        uriItems.length <= 3 || uriItems[3] == null,
                        columns,
                        addTotalCountHeader
                );

                resultJson = resultSetTypeAdapter.toJsonTree(rs);

                headers.put(Response.HAS_MORE_ROWS_HEADER, Boolean.toString(resultSetTypeAdapter.moreRows()));
                headers.put(Response.FETCHED_COUNT_HEADER, Integer.toString(resultSetTypeAdapter.getFetchedRowsCnt()));
                if (!resultSetTypeAdapter.hasNext())
                    headers.put(Response.TOTAL_COUNT_HEADER, Integer.toString(resultSetTypeAdapter.getTotalCount()));

            } else {
                resultJson = new JsonObject();
            }


            if (outParamsJson != null) {

                //Add token for successful auth method
                if (outParamsJson.has(HTTP_ERROR_CODE)) {

                    return Response.newErrInstance(
                            outParamsJson.get(ERROR_MESSAGE).getAsString(),
                            HttpStatus.valueOf(outParamsJson.get(HTTP_ERROR_CODE).getAsInt())
                    );

                } else if (outParamsJson.has(AUTH_LOGIN)) {

                    outParamsJson.addProperty("token", Auth.getToken(
                            outParamsJson.get(AUTH_LOGIN).getAsString(),
                            inputJson.get("remote_addr").getAsString(),
                            0
                        )
                    );

                }

                //Copy parameters from outParamsJson to resultJson
                if (resultJson.isJsonObject())
                    outParamsJson.entrySet().stream().filter(entry -> !entry.getKey().matches("i#[^ ]*")).forEach(entry -> resultJson.getAsJsonObject().add(entry.getKey(), entry.getValue()));

            }

            Response response = Response.newInstance(resultJson, HttpStatus.OK);
            for(Map.Entry<String, String> entry: headers.entrySet())
                response.addHeader(entry.getKey(), entry.getValue());
            return response;

        } catch (SQLException e) {

            boolean retryResponse = false;

            if (firstAttempt && (e.getErrorCode() == 3113 //end-of-file on communication channel)
                    || e.getErrorCode() == 3114 //not connected to ORACLE
                    || e.getErrorCode() == 29260 //network error
                    || e.getErrorCode() == 28 //your session has been killed
                    || e.getErrorCode() == 29471 //DBMS_SQL access denied
                    || e.getErrorCode() == 65535
                    || e.getErrorCode() == 4068 //existing state of packages has been discarded
                    || e.getErrorCode() == 4061 //existing state of  has been invalidated
                    || e.getErrorCode() == 25408 //can not safely replay call
                    || e.getErrorCode() == 600)) { //Oracle client cannot handle error code exceeding 65535

                //Try to reconnect to DB and retry response
                Log.info("Reconnecting to DB due to error: " + e.getErrorCode());
                reconnect();
                retryResponse = true;

            }

            if (retryResponse) {
                Log.info("Retry response due to error: " + e.getErrorCode());
                return getResponseJson(inputJson, false, rowsArray, addTotalCountHeader);
            } else {
                Log.error("Error", e);
                return Response.newErrInstance(translateErrorMsg(e.getMessage()), HttpStatus.BAD_REQUEST);
            }

        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    Log.warn("Cannot close resultset", e);
                }

            }

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    Log.warn("Cannot close statement", e);
                }
            }

            returnConnection(oraConn);

        }
    }

    private String translateErrorMsg(String errorMsg) {

        String translatedErrorMsg = errorMsg;
        Connection oraConn;

        try {
            oraConn = getConnection();
        } catch (java.sql.SQLException e) {
            Log.error("Error connecting to Oracle: ", e);
            return "Translate message failed";
        }

        try {

            CallableStatement stmt = null;
            String query = "begin ? := smarto.prv_rest_api.get_translated_error_msg(?); end;";

            try {
                stmt = oraConn.prepareCall(query);
                stmt.registerOutParameter(1, OracleTypes.VARCHAR);
                stmt.setString(2, errorMsg);
                stmt.execute();
                oraConn.commit();

                translatedErrorMsg = stmt.getString(1);
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        Log.warn("Cannot close statement", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.error("Error translating message: " + errorMsg, e);
        } finally {
            returnConnection(oraConn);
        }

        return translatedErrorMsg;
    }

    public List<HashMap<String, String>> getQueryResultAsSetOfRows(String query, int maxRows) {

        Connection oraConn;

        try {
            oraConn = getConnection();
        } catch (java.sql.SQLException e) {
            Log.error("Error connecting to Oracle while generating Swagger json: ", e);
            return null;
        }

        try {

            CallableStatement stmt = null;

            try {
                stmt = oraConn.prepareCall(query);

                List<HashMap<String, String>> result = new ArrayList<>();
                stmt.registerOutParameter(1, OracleTypes.CURSOR);
                stmt.execute();
                ResultSet rs = (ResultSet) stmt.getObject(1);
                ResultSetMetaData meta = rs.getMetaData();
                int totalCols = meta.getColumnCount();

                int rowsCnt = 0;
                while (rs.next() && rowsCnt < maxRows) {

                    HashMap<String, String> hashMap = new HashMap<>();

                    for (int i = 1; i <= totalCols; ++i)
                        hashMap.put(meta.getColumnName(i).toLowerCase(), rs.getString(i));

                    result.add(hashMap);
                    rowsCnt++;
                }

                return result;


            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        Log.warn("Cannot close statement", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.error("Error executing statement: " + query, e);
            return null;
        } finally {
            returnConnection(oraConn);
        }


    }

}