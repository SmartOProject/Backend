package com.smarto.rest.common;

import java.io.*;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import com.google.gson.*;
import com.google.gson.stream.*;

class ObjectResultSetAdapter extends TypeAdapter<ResultSet> {

    private static class NotImplemented extends RuntimeException {}
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy.MM.dd HH:mm:ss").create();

    public ResultSet read(JsonReader reader) throws IOException {
        throw new NotImplemented();
    }

    private int skipRows;
    private int maxRows;
    private boolean useArray;
    private boolean allColumns;
    private List<String> columns;
    private boolean addTotalCountHeader;

    private int totalCount;
    private int fetchedRowsCnt;
    private boolean hasNext;
    private boolean moreRows;

    int getTotalCount() {
        return totalCount;
    }
    int getFetchedRowsCnt() {
        return fetchedRowsCnt;
    }
    boolean hasNext() {
        return hasNext;
    }
    boolean moreRows() {
        return moreRows;
    }


    ObjectResultSetAdapter(int skipRows, int maxRows, boolean useArray, String[] columnList, boolean addTotalCountHeader) {

        //Clear all static counters
        totalCount = 0;
        fetchedRowsCnt = 0;
        hasNext = true;

        this.skipRows = skipRows;
        //Limit maxRows with Conf.getResponseRecordCountLimit
        this.maxRows = maxRows == 0 || maxRows > Conf.getResponseRecordCountLimit() ? Conf.getResponseRecordCountLimit() : maxRows;
        this.useArray = useArray;
        this.allColumns = (columnList == null);
        this.addTotalCountHeader = addTotalCountHeader;

        if (!allColumns)
            columns = Arrays.asList(columnList);
    }

    public void write(JsonWriter writer, ResultSet rs) throws IOException {

        try {
            ResultSetMetaData meta = rs.getMetaData();
            int totalCols = meta.getColumnCount();

            if (useArray) writer.beginArray();

            //Skip rows
            for (int i = 0; i < skipRows - 1; i++)
                totalCount += rs.next() ? 1 : 0;

            while (fetchedRowsCnt < (useArray ? maxRows : 1) && (hasNext = rs.next())) {
                writer.beginObject();
                for (int i = 1; i <= totalCols; ++i) {
                    String column = meta.getColumnName(i).toLowerCase();
                    if (allColumns || columns.contains(column)) {
                        writer.name(column);


                        if (meta.getColumnClassName(i).equals("oracle.jdbc.OracleClob")) {

                            String str = Utils.readStream(rs.getClob(i).getCharacterStream());

                            if (column.endsWith("_json")) {
                                gson.toJson(Conf.getJsonParser().parse(str), writer);
                            } else {
                                gson.toJson(str, Class.forName("java.lang.String"), writer);
                            }

                        } else {

                            Class<?> type = Class.forName(meta.getColumnClassName(i));
                            gson.toJson(rs.getObject(i), type, writer);

                        }
                    }
                }
                writer.endObject();
                totalCount++;
                fetchedRowsCnt++;
            }
            if (useArray) writer.endArray();

            moreRows = hasNext;

            //Fetch to end
            if (addTotalCountHeader) while (hasNext = rs.next()) totalCount++;

        } catch (SQLException|ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getClass().getName(), e);
        }
    }
}