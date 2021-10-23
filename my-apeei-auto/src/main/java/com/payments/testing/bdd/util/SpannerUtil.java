package com.payments.testing.bdd.util;

import com.google.cloud.spanner.*;
import com.payments.testing.bdd.exceptions.CustomException;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpannerUtil {

    private static final String SELECT_ALL_RECORD = "SELECT * FROM ";
    private static Config config = BDDConfig.getConfig();
    private static final Logger LOG = LoggerFactory.getLogger(SpannerUtil.class);

    DatabaseClient dbClient;
    Spanner spanner;

    private DatabaseClient createDatabaseClient() {
        String SPANNER_PROJECT_ID = config.getString("spanner.project.id");
        String SPANNER_INSTANCE_ID = config.getString("spanner.instance.id");
        String SPANNER_DATABASE_ID = config.getString("spanner.database.id");

        SpannerOptions options = SpannerOptions.newBuilder().setProjectId(SPANNER_PROJECT_ID).build();
        spanner = options.getService();
        dbClient = spanner.getDatabaseClient(
                DatabaseId.of(options.getProjectId(), SPANNER_INSTANCE_ID, SPANNER_DATABASE_ID)
        );
        return dbClient;
    }

    private static String getQueryForDataSet(final String datasetName) {
        String query = "";
        query = SELECT_ALL_RECORD + datasetName;
        return query;
    }

    private ResultSet getResultSet(String dbQuery) {
        dbClient = createDatabaseClient();
        ResultSet resultSet = dbClient.singleUse().executeQuery(Statement.of(dbQuery));
        return resultSet;
    }

    private String queryBuilder(String databasetName, Map<String, String> filterMap) {
        StringBuilder query = new StringBuilder(getQueryForDataSet(databasetName));
        int size = 0;
        if (filterMap != null && filterMap.size() > 0) {
            query.append(" where ");
            for (Map.Entry<String, String> entry : filterMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("Amount"))
                    query.append(entry.getKey() + " = " + entry.getValue());
                else
                    query.append(entry.getKey() + " = '" + entry.getValue() + "'");
                size++;
                if (size != filterMap.size()) {
                    query.append(" and ");
                }
            }
        }
        return query.toString();
    }

    public List<Map<String, Object>> getRecords(String databasetName, Map<String, String> filterMap) {
        Map<String, Object> recordMap = new HashMap<>();
        List<Map<String, Object>> recordList = new ArrayList<>();

        String query = queryBuilder(databasetName, filterMap);

        LOG.info("Executing Query= " + query + " on Dataset= " + databasetName);
        try (ResultSet resultSet = getResultSet(query)) {
            while (resultSet.next()) {
                resultSet.getType().getStructFields().stream().forEach(s -> {
                            recordMap.put(s.getName(), getObject(s, resultSet));
                        }
                );
                recordList.add(recordMap);
            }
            // System.out.println(Arrays.toString(recordList.toArray()));
        } catch (SpannerException e) {
            String error = "Dataset= " + databasetName + " Query= " + query + " SpannerExceptions: " + e.getMessage();
            throw new CustomException(error);
        }
        return recordList;
    }

    public List<String> getListOfColumnsInResultSet(String databasetName, Map<String, String> filterMap) {
        List<String> getListOfColumnsInResultSet = new ArrayList<String>();
        String query = queryBuilder(databasetName, filterMap);
        try (ResultSet resultSet = getResultSet(query)) {
            resultSet.next();
            resultSet.getType().getStructFields().stream().forEach(s -> {
                getListOfColumnsInResultSet.add(s.getName());
            });
        } catch (SpannerException e) {
            String error = "Dataset= " + databasetName + " Query= " + query + " SpannerExceptions: " + e.getMessage();
            throw new CustomException(error);
        }
        return getListOfColumnsInResultSet;
    }

    private Object getObject(Type.StructField structField, ResultSet resultSet) {
        if (structField.getType() == Type.bool())
            return resultSet.isNull(structField.getName()) ? null : resultSet.getBoolean(structField.getName());
        if (structField.getType() == Type.bytes())
            return resultSet.isNull(structField.getName()) ? null : resultSet.getBytes(structField.getName());
        if (structField.getType() == Type.date())
            return resultSet.isNull(structField.getName()) ? null : resultSet.getDate(structField.getName());
        if (structField.getType() == Type.float64())
            return resultSet.isNull(structField.getName()) ? null : resultSet.getDouble(structField.getName());
        if (structField.getType() == Type.int64())
            return resultSet.isNull(structField.getName()) ? null : resultSet.getLong(structField.getName());
        if (structField.getType() == Type.string())
            return resultSet.isNull(structField.getName()) ? null : resultSet.getString(structField.getName());
        if (structField.getType() == Type.timestamp())
            return resultSet.isNull(structField.getName()) ? null : resultSet.getTimestamp(structField.getName());
        return null;
    }

    public void writeUsingDml(String sql) {
        dbClient = createDatabaseClient();
        dbClient
                .readWriteTransaction()
                .run(
                        new TransactionRunner.TransactionCallable<Void>() {
                            @Override
                            public Void run(TransactionContext transaction) throws Exception {
                                long rowCount = transaction.executeUpdate(Statement.of(sql));
                                return null;
                            }
                        });
    }

//    public void deleteData(String CustomerID) {
//        String entityQuery = "Delete from KeyDetail where EntityId = \"" + CustomerID + "\"";
//        String customerQuery = "Delete from CustomerDetail where CustomerId = \"" + CustomerID + "\"";
//        writeUsingDml(entityQuery);
//        writeUsingDml(customerQuery);
//    }

    public void closeSpannerClient() {
        spanner.close();
    }

}