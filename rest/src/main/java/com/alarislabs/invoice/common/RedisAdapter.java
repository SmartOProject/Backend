package com.alarislabs.invoice.common;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.util.Map;
import java.util.Set;


public class RedisAdapter {

    private static final Logger Log = LoggerFactory.getLogger(RedisAdapter.class);
    private JedisPool pool;
    private String hostname;
    private int port;
    private String pwd;

    RedisAdapter(String hostname_, int port_, String pwd_) {

        hostname = hostname_;
        port = port_;
        pwd = pwd_;
    }

    private JedisPool getPoolInstance() {

        if (pool == null) {

            pool = new JedisPool(new GenericObjectPoolConfig(), hostname, port, 1800, pwd);

            Jedis jedis = null;
            try {
                jedis = pool.getResource();
            } catch (JedisConnectionException e) {
                if (pwd != null && e.getCause().getMessage().equals("ERR Client sent AUTH, but no password is set")) {
                    //Reset connection without password
                    pwd = null;
                    destroyPoolInstance();
                    return getPoolInstance();
                } else {
                    throw e;
                }
            } finally {
                if (jedis != null) jedis.close();
            }

        }

        return pool;

    }

    void destroyPoolInstance() {

        if (pool != null) {
            pool.destroy();
            pool = null;
        }
    }

    boolean testConnection(String label) {

        try (Jedis jedis = getPoolInstance().getResource()) {
            jedis.exists("dummy");
            Log.trace("Connection to " + label + "@" + hostname + ":" + Integer.toString(port) + " successfully tested");
            return true;
        } catch (Exception e) {
            Log.error("Test connection " + label + "@" + hostname + ":" + Integer.toString(port) + " failed due to " + e.getMessage());
            return false;
        }
    }


    public String get(String key) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            return jedis.get(key);
        }
    }

    public void set(String key, String value) throws Exception {
        set(key, value, 0);
    }

    public void set(String key, String value, int expireSeconds) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            jedis.set(key, value);
            if (expireSeconds > 0) jedis.expire(key, expireSeconds);
        }
    }

    public void del(String key) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            jedis.del(key);
        }
    }

    public boolean exists(String key) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            return jedis.exists(key);
        }
    }

    public boolean hexists(String key, String field) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            return jedis.hexists(key, field);
        }
    }

    public String hget(String key, String field) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            return jedis.hget(key, field);
        }
    }

    public void hset(String key, String field, String value) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            jedis.hset(key, field, value);
        }
    }

    public void hmset(String key, Map<String,String> map) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            jedis.hmset(key, map);
        }
    }

    public Set<String> keys(String pattern) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            return jedis.keys(pattern);
        }
    }

    public Map<String, String> hgetAll(String key) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            return jedis.hgetAll(key);
        }
    }

    public Long hincrBy(String key, String field, long value) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            return jedis.hincrBy(key, field, value);
        }
    }

    public double hincrByFloat(String key, String field, double value) throws Exception {

        try (Jedis jedis = getPoolInstance().getResource()) {
            return jedis.hincrByFloat(key, field, value);
        }
    }


}
