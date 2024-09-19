package com.kafka.ui.serdes.glue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.apicurio.registry.utils.converter.AvroConverter;
import io.kafbat.ui.serde.api.*;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.json.JsonConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ApicurioGlueSerde implements Serde{

    private AvroConverter converter = new AvroConverter();
    private RegistryClient client;
    ObjectMapper mapper = new ObjectMapper();
    JsonConverter jsonConverter = new JsonConverter();


    @Override
    public void configure(PropertyResolver serdeProperties, PropertyResolver clusterProperties, PropertyResolver appProperties) {
        String AVRO_REGISTRY_ENDPOINT = serdeProperties.getProperty("endpoint", String.class).orElseThrow(() -> new IllegalArgumentException("endpoint is not set"));
        Map<String, String> props = new HashMap<>();
        props.put("apicurio.registry.url", AVRO_REGISTRY_ENDPOINT);
        converter.configure(props, true);
        client = RegistryClientFactory.create(AVRO_REGISTRY_ENDPOINT);
        jsonConverter.configure(new HashMap<>(), false);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.empty();
    }

    @Override
    public Optional<SchemaDescription> getSchema(String topic, Target target) {
        String artifactName = Target.KEY == target ? topic + "-key" : topic + "-value";
        try {
            return Optional.of(
                    new SchemaDescription(
                            mapper.writeValueAsString(client.getLatestArtifact("default", artifactName))
                                    , Map.of()
                    )
            );
        }catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canDeserialize(String s, Target target) {
        return true;
    }

    @Override
    public boolean canSerialize(String s, Target target) {
        return true;
    }

    @Override
    public Serializer serializer(String topic, Target target) {
        return new Serializer() {
            @Override
            public byte[] serialize(String s) {
                return new AvroKafkaSerializer<>(client).serialize(topic, s);
            }
        };
    }

    @Override
    public Deserializer deserializer(String topic, Target target) {
        return new Deserializer() {
            @Override
            public DeserializeResult deserialize(RecordHeaders recordHeaders, byte[] bytes) {
                SchemaAndValue data = converter.toConnectData(topic, bytes);
                ObjectNode jsonStr = convertStructToJson((Struct) data.value(), mapper);
                return new DeserializeResult(jsonStr.toString(), DeserializeResult.Type.JSON, Map.of());
            }
        };
    }

    private ObjectNode convertStructToJson(Struct struct, ObjectMapper mapper) {
        ObjectNode jsonNode = mapper.createObjectNode();
        Schema schema = struct.schema();

        for (Field field : schema.fields()) {
            Object value = struct.get(field);
            if (value instanceof Struct){
                jsonNode.set(field.name(), convertStructToJson((Struct) value, mapper));
            } else{
                jsonNode.putPOJO(field.name(), value);
            }
        }
        return jsonNode;
    }
}
