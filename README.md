# kafkaui-glue-apicurio-serde

> kafka-ui에서 apicurio scheme registry로 serialize/deserialize를 처리하기 위한 프로젝트
> [custom-pluggable-serde-registration](https://ui.docs.kafbat.io/configuration/serialization-serde#custom-pluggable-serde-registration)

## 적용 가이드

### 패키지 업로드

### kafka-ui에 적용

- kafka-ui 기동 시 해당 라이브러리를 읽어오게 되어 있음.
- configmap 설정

```yaml
# kafka-ui-configmap
- name: ${name}
  bootstrapServers: ${brokers}
  schemaRegistry: ${apicurio}
  defaultKeySerde: ApicurioGlueSerde
  defaultValueSerde: ApicurioGlueSerde
  serde:
    - name: ApicurioGlueSerde
      className: com.kafka.ui.serdes.glue.ApicurioGlueSerde
      filePath: /kafkaui-glue-apicurio-serde-1.0-SNAPSHOT-jar-with-dependencies.jar
      properties:
        endpont: ${apicurio url}
```

### kafka-ui 컨테이너에서 post job으로 download 처리

```yaml
containers:
  - resources: {}
    lifecycle:
      postStart:
        exec:
          command:
            - sh
            - '-c'
            - wget ${download url}
```