package com.payments.testing.bdd.api.httpservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class InitProperties {

    private static final Logger logger = LoggerFactory.getLogger(InitProperties.class);

    private HashMap<String, String> envProperties;
    public void initEnvProperties(String envName){
        envProperties = new HashMap<>();
        loadConfigFile(envName);
    }

    private void loadConfigFile(String envFileName) {
        String environmentConfigPath = "src/test/resources/config/ENV.env.properties";
        Properties prop = new Properties();
        InputStream iStream;
        try {
            String envFileaPath = environmentConfigPath.replace("ENV", envFileName);
            iStream = new FileInputStream(envFileaPath);
            prop.load(iStream);
            logger.info("Property File Loaded Succesfully");
            Set<String> propertyNames = prop.stringPropertyNames();
            for (String Property : propertyNames) {
                logger.info(Property + ":" + prop.getProperty(Property));
                envProperties.put(Property, prop.getProperty(Property));
            }
        } catch (Exception e) {
            logger.error("Error in loading the environment properties file");
        }
    }

    //Method to getEnvPropertyOrSetDefault value from the key
    public String getEnvProperty(String key) {
        return envProperties.get(key);
    }

    //Method to getEnvPropertyOrSetDefault value or return default value.
    public static String getSystemPropertyOrSetDefault(String key, final String defaultValue) {
        Optional<String> value = Optional.ofNullable(System.getProperty(key));
        if(!value.isPresent()){
            logger.info("Value is is not given and setting it to the default: " + defaultValue);
        }
        return value.orElse(defaultValue);
    }

    //Method to getEnvPropertyOrSetDefault value or return default value.
    public String getEnvPropertyOrSetDefault(String key, String defaultValue) {
        Optional<String> value = Optional.ofNullable(envProperties.get(key));
        return value.orElse(defaultValue);
    }

    //Method to getEnvPropertyOrSetDefault boolean value or return default.
    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.valueOf(getEnvPropertyOrSetDefault(key, Boolean.toString(defaultValue)));
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public void put(String key, String value) {
        envProperties.put(key, value);
    }
}