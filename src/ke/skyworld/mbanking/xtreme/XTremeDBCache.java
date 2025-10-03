package ke.skyworld.mbanking.xtreme;

import ke.skyworld.lib.mbanking.core.MBankingLocalParameters;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("Duplicates")
public class XTremeDBCache {

    public static void store(String key, String value) throws Exception {
        HashMap<String, String> v = new HashMap<>();
        v.put("value", value);
        store(key, v, -1);
    }

    public static void store(String key, HashMap<String, String> value) throws Exception {
        store(key, value, -1);
    }

    /**
     *
     * @param key
     * @param value
     * @param timeToLive - In SECONDS
     * @throws Exception
     */
    public static void store(String key, String value, long timeToLive) throws Exception {
        HashMap<String, String> v = new HashMap<>();
        v.put("value", value);
        store(key, v, timeToLive);
    }

    /**
     *
     * @param key
     * @param value
     * @param timeToLive - In SECONDS
     */
    public static void store(String key, HashMap<String, String> value, long timeToLive) throws Exception {

        //Prevent null or empty key identifiers
        if(key == null || key.equals(""))
            throw new NullPointerException("Identifier - key cannot be null or empty");

        //Convert time to live to seconds
        if(timeToLive != -1) timeToLive = timeToLive*1000;

        //Store object in map (timeToLive stored too in case scheduler fails
        put(key, value, (timeToLive != -1) ? (System.currentTimeMillis() + timeToLive) : timeToLive);
        System.out.println("DBCache.store(): Stored Entry -> key: "+key+" successfully");

        //Schedule Map entry for deletion after timeToLive seconds
        if(timeToLive != -1){
            Timer t = new Timer();
            t.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            try { remove(key); } catch (Exception e) { throw new RuntimeException(e); }
                            t.cancel();
                        }
                    }, timeToLive
            );
        }
    }

    public static HashMap<String, String> retrieve(String key) throws Exception {

        //Check if key exists. Else return null
        if (exists(key)) {
            HashMap<String, String> value = get(key);
            long expires = Long.parseLong(value.get("time_to_live"));
            if(expires != -1){
                //Check if entry has expired. If so, delete it. Else retrieve entry and return
                if (expires - System.currentTimeMillis() > 0) {
                    return value;
                } else {
                    remove(key);
                }
            }else{
                return value;
            }
        }

        return null;
    }

    public static HashMap<String, String> retrieveAndRemove(String key) throws Exception {
        //Check if key exists. Else return null
        if (exists(key)) {
            //Retrieve entry and delete it
            HashMap<String, String> obj = retrieve(key);
            remove(key);
            return obj;
        }
        return null;
    }

    public static boolean exists(String  key) throws Exception {
        String MBankingDBName = MBankingLocalParameters.getDatabaseName();
        String sql = "SELECT 'EXISTS' AS _exists FROM "+MBankingDBName+".db_cache WHERE entry_key = :entry_key;";

        HashMap<String,Object> queryVariables = new HashMap<>();
        queryVariables.put("entry_key", key);

        XTremeQuery query = new XTremeQuery(sql, queryVariables);
        query.executeSelect();
        return !query.isResultSetEmpty();
    }


    private static HashMap<String, String> get(String  key) throws Exception {
        String MBankingDBName = MBankingLocalParameters.getDatabaseName();
        String sql = "SELECT id, entry_key, value, description, time_to_live, date_created, date_modified FROM "+MBankingDBName+".db_cache WHERE entry_key = :entry_key;";

        HashMap<String,Object> queryVariables = new HashMap<>();
        queryVariables.put("entry_key", key);

        XTremeQuery query = new XTremeQuery(sql, queryVariables);

        return query.executeSelect().get(0);
    }


    private static void put(String key, HashMap<String, String> value, long timeToLive) throws Exception {
        String MBankingDBName = MBankingLocalParameters.getDatabaseName();
        String sql = "INSERT INTO "+MBankingDBName+".db_cache (entry_key, value, description, time_to_live) VALUES (:entry_key, :value, :description, :time_to_live);";

        HashMap<String,Object> queryVariables = new HashMap<>();
        queryVariables.put("entry_key", key);
        queryVariables.put("value", value.get("value"));
        queryVariables.put("description", value.get("description"));
        queryVariables.put("time_to_live", timeToLive);

        XTremeQuery query = new XTremeQuery(sql, queryVariables);
        query.executeUpdate();
    }


    private static void put(String key, String value, long timeToLive) throws Exception {
        String MBankingDBName = MBankingLocalParameters.getDatabaseName();
        String sql = "INSERT INTO "+MBankingDBName+".db_cache (entry_key, value, time_to_live) VALUES (:entry_key, :value, :time_to_live);";

        HashMap<String,Object> queryVariables = new HashMap<>();
        queryVariables.put("entry_key", key);
        queryVariables.put("value", value);
        queryVariables.put("time_to_live", timeToLive);

        XTremeQuery query = new XTremeQuery(sql, queryVariables);
        query.executeUpdate();
    }


    public static void remove(String  key) throws Exception {
        String MBankingDBName = MBankingLocalParameters.getDatabaseName();
        String sql = "DELETE FROM "+MBankingDBName+".db_cache WHERE entry_key = :entry_key;";

        HashMap<String,Object> queryVariables = new HashMap<>();
        queryVariables.put("entry_key", key);

        XTremeQuery query = new XTremeQuery(sql, queryVariables);
        query.executeUpdate();
    }

    public static boolean existsAndRemove(String  key) throws Exception {
        if(exists(key)){
            remove(key);
            return true;
        } else return false;
    }
}