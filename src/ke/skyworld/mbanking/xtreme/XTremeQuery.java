package ke.skyworld.mbanking.xtreme;

import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.utils.NamedParameterStatement;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class XTremeQuery {

	private String query;
	private long rowCount;
	private String lastInsertId;
	private List<String> resultColumns = new ArrayList<>();
	private NamedParameterStatement namedParameterStatement;
	private HashMap<String, Object> queryVariables = new HashMap<>();
	private ResultSet resultSet;
	private boolean isResultSetEmpty = false;

	public XTremeQuery(){}

	public XTremeQuery(String query) {
		this.query = query;
	}

	public XTremeQuery(String query, HashMap<String, Object> queryVariables) {
		this.query = query;
		this.queryVariables = queryVariables;
	}

	public LinkedList<HashMap<String, String>> executeSelect() throws Exception {
		LinkedList<HashMap<String, String>> records = new LinkedList<>();
		HashMap<String, String> record;

		/*long numberOfNamedParameters = query.chars().filter(ch -> ch == ':').count();

		if(numberOfNamedParameters != queryVariables.size())
			throw new Exception("Named variable count ("+numberOfNamedParameters+") " +
					"does not match query values size ("+queryVariables.size()+")");*/

		namedParameterStatement = new NamedParameterStatement(MBankingDB.getConnection(), query);

		//Prepare Statement
		if(queryVariables.size() > 0){
			for (int i = 1; i <= queryVariables.size(); i++) {
				for (String key : queryVariables.keySet()){
					namedParameterStatement.setObject(key, queryVariables.get(key));
				}
			}
		}

		resultSet = namedParameterStatement.executeQuery();

		try {
			ResultSetMetaData rsMetadata = resultSet.getMetaData();
			int resultColumnCount = rsMetadata.getColumnCount();

			//Get result columns
			for (int i = 1; i < resultColumnCount+1; i++) {
				resultColumns.add(rsMetadata.getColumnLabel(i));
			}

			//Get the data
			if (!resultSet.next()) {
				//then there are no rows.
				isResultSetEmpty = true;
				gc();
				return records;
			} else {
				do {
					record = new HashMap<>();
					for (String columnLabel : resultColumns) {
						record.put(columnLabel, resultSet.getString(columnLabel));
					}
					records.add(record);
					rowCount += 1;
				} while (resultSet.next());
			}

		} catch (Exception e){
			gc();
			System.err.println("xtremeerp.db.Query.executeSelect(): Error message: " + e.getMessage());
			throw new Exception("xtremeerp.db.Query.executeSelect(): Error message: " + e.getMessage());
		}

		gc();
		return records;
	}

	public int executeUpdate() throws Exception {
		/*long numberOfNamedParameters = query.chars().filter(ch -> ch == ':').count();

		if(numberOfNamedParameters != queryVariables.size())
			throw new Exception("Named variable count ("+numberOfNamedParameters+") " +
					"does not match query values size ("+queryVariables.size()+")");*/

		namedParameterStatement = new NamedParameterStatement(MBankingDB.getConnection(), query, Statement.RETURN_GENERATED_KEYS);

		//Prepare Statement
		if(queryVariables.size() > 0){
			for (int i = 1; i <= queryVariables.size(); i++) {
				for (String key : queryVariables.keySet()){
					namedParameterStatement.setObject(key, queryVariables.get(key));
				}
			}
		}

		int result = namedParameterStatement.executeUpdate();

		resultSet = namedParameterStatement.getGeneratedKeys();
		if (resultSet.next()){
			lastInsertId = resultSet.getString(1);
		}

		gc();

		return result;
	}

	public String getQuery() {
		return query;
	}

	public long getRowCount() {
		return rowCount;
	}

	public List<String> getResultColumns() {
		return resultColumns;
	}

	public boolean isResultSetEmpty() {
		return isResultSetEmpty;
	}

	public void addQueryVariable(String name, Object value){
		queryVariables.put(name, value);
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setQueryVariables(HashMap<String, Object> queryVariables) {
		this.queryVariables = queryVariables;
	}

	private void gc(){
		try {

			if (namedParameterStatement != null) {
				try {
					namedParameterStatement.close();
				} finally {
					namedParameterStatement = null;
				}
			}

			if (resultSet != null) {
				try {
					resultSet.close();
				} finally {
					resultSet = null;
				}
			}

		} catch (Exception e) {
			System.err.println("xtremeerp.db.Query.gc(): Error message: " + e.getMessage());
		} finally {
			namedParameterStatement = null;
			resultSet = null;
		}
	}

	public String getLastInsertId() {
		return lastInsertId;
	}
}