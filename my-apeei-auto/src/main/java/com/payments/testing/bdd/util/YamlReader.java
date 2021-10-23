package com.payments.testing.bdd.util;

import com.typesafe.config.Config;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.core.type.TypeReference;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.ObjectReader;
import io.cucumber.datatable.dependency.com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.List;
import java.util.Map;


/**
 * @author sumit_sharma
 * Basic reading and writing for Yaml Files
 */
public class YamlReader {

    /**
     * The logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(YamlReader.class);

    /**
     * The loaded yaml data
     */
    private final Map<String, Object> yamlObj;

    /**
     * Yaml reader for this thread, could be ThreadLocal static
     */
    private final Yaml yaml;

    private static Config config = BDDConfig.getConfig();

    private static Pair<ObjectNode, ObjectMapper> nodeObjectMapperPair;
    private static Pair<ObjectReader, ObjectMapper> readerObjectMapperPair;

    /**
     * Reads the given file as yaml into memory
     *
     * @param fileName the given file input stream
     */
    public YamlReader(final InputStream fileName) {

        this.yaml = new Yaml();

        final Object loadObj = this.yaml.load(fileName);

        if (loadObj instanceof Map<?, ?>) {

            this.yamlObj = (Map<String, Object>) loadObj;

        } else {

            this.yamlObj = null;

        }

    }

    /**
     * Reads the given file using a file reader
     *
     * @param fileName the given file path
     */
    public YamlReader(final String fileName) {

        this.yaml = new Yaml();

        Map<String, Object> yamlObjLocal = null;

        try (FileReader reader = new FileReader(fileName)) {

            final Object loadObj = this.yaml.load(reader);

            if (Map.class.isInstance(loadObj)) {

                yamlObjLocal = Map.class.cast(loadObj);

            }

        } catch (final FileNotFoundException e) {

            throw new RuntimeException("YAML file not found " + fileName, e);

        } catch (final IOException e1) {

            logger.debug(e1.toString(), "Error while loading yaml");

        }

        this.yamlObj = yamlObjLocal;

    }

    /**
     * Gets a Map from a list with a key and an index
     *
     * @param key       the key into the map
     * @param listIndex the index into the list in the map
     * @return the map or null if the stored object is not a map
     */
    public Map<String, Object> getMapFromList(final String key, final int listIndex) {

        final List<?> list = this.getList(key);

        final Object dataObj = list.get(listIndex);

        if (dataObj instanceof Map<?, ?>) {

            return (Map<String, Object>) dataObj;

        }

        return null;

    }

    /**
     * Gets a list with the specified key.
     * Returns null if the value at the key is not a list
     *
     * @param key the key to call getValue on
     * @return the list of objects or null
     */
    public List<?> getList(final String key) {

        final Object obj = this.getValue(key);

        if (obj instanceof List<?>) {

            return (List<?>) obj;

        }

        return null;

    }

    /**
     * Gets the object from the map with the key
     *
     * @param key the key into the map
     * @return the Object stored in the map
     */
    public Object getValue(final String key) {

        return this.yamlObj.get(key);

    }

    /**
     * Gets a list with the specified key.
     * Returns null if the value at the key is not a list
     *
     * @param key the key to call getValue on
     * @return the list of objects or null
     */
    public Map<String, Object> getYamlObj(final String key) {

        final Object obj = this.getValue(key);

        if (obj instanceof Map<?, ?>) {

            return (Map<String, Object>) obj;

        }

        return null;
    }


    /**
     * Creates/Overwrites a file with the data from the map in yaml format
     *
     * @param newYamlFileName the file in which to store the yaml
     * @param newData         the data to be stored in the file
     */
    public static void writeToNewYamlFile(final String newYamlFileName, final Map<String, Object> newData) {

        final File file = new File(newYamlFileName);

        if (file.exists()) {

            file.delete();

        }

        final DumperOptions options = new DumperOptions();

        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        final Yaml yaml = new Yaml(options);

        try (final FileWriter writer = new FileWriter(file, true)) {

            file.createNewFile();

            writer.append(System.getProperty("line.separator"));

            yaml.dump(newData, writer);

        } catch (final IOException e) {

            throw new RuntimeException("Error occured while creating new file " + newYamlFileName, e);

        }

    }

    /**
     * Appends the data to the given yaml file
     *
     * @param yamlFile the target of the append action
     * @param newData  the data to be appeneded to the file
     */
    public static void writeToYamlFile(final String yamlFile, final Map<String, Object> newData) {

        final DumperOptions options = new DumperOptions();

        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        final Yaml yaml = new Yaml(options);

        try (final FileWriter writer = new FileWriter(yamlFile, true)) {

            writer.append(System.getProperty("line.separator"));

            yaml.dump(newData, writer);

        } catch (final IOException e) {

            throw new RuntimeException("YAML file not found " + yamlFile, e);

        }

    }

    public static Pair<ObjectNode, ObjectMapper> readJsonSchemaFile(String schema) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.readValue(new File(config.getConfig("files").getString("schema")
                .replace("schema_name", schema)), ObjectNode.class);
        return new Pair<>(node, mapper);
    }

    public static Pair<ObjectReader, ObjectMapper> JsonPartToUpdate(String schema, String getValueToUpdate) throws IOException {
        nodeObjectMapperPair = readJsonSchemaFile(schema);
        ObjectReader updater = nodeObjectMapperPair.getValue()
                .readerForUpdating(nodeObjectMapperPair.getKey().get(getValueToUpdate));
        ObjectMapper reqWriter = new ObjectMapper();
        return new Pair<>(updater, reqWriter);
    }

    public static String replaceJsonWithYmlDataAsString
            (String schema, String getValueToUpdate, Map<String, Object> selectData) throws IOException {
        readerObjectMapperPair = JsonPartToUpdate(schema, getValueToUpdate);
        String retValue = readerObjectMapperPair.getKey().readValue(readerObjectMapperPair.getValue().writeValueAsString(selectData)).toString();
        return retValue;
    }

    public Map<String, String> replaceJsonWithYmlDataAsMap
            (String schema, String getValueToUpdate, Map<String, Object> selectData) throws IOException {
        readerObjectMapperPair = JsonPartToUpdate(schema, getValueToUpdate);
        return readerObjectMapperPair.getValue()
                .readValue(readerObjectMapperPair.getKey().readValue(readerObjectMapperPair.getValue()
                        .writeValueAsString(selectData)).toString(), new TypeReference<Map<String, Object>>() {
                });
    }

    public static Pair<ObjectNode, ObjectMapper> readJsonToNode(String jsonObject) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.readValue(jsonObject, ObjectNode.class);
        return new Pair<>(node, mapper);
    }

    public static String getJsonNodeValue(JsonNode data, String nodeKey) {
        String value = null;
        JsonNode node = data;
        if (data.isArray()) {
            if (data.size() == 0) {
                return null;
            } else if (data.size() == 1) {
                node = data.get(0);
            } else {
                node = data.get(0);
            }
        }

        if (nodeKey.contains("/")) {
            value = node.at(nodeKey) == null ? null : node.at(nodeKey).asText();
        } else {
            value = node.get(nodeKey) == null ? null : node.get(nodeKey).asText();
        }
        return value;
    }
}