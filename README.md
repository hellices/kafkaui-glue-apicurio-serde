# kafkaui-glue-apicurio-serde

> kafka-ui에서 apicurio scheme registry로 serialize/deserialize를 처리하기 위한 프로젝트
> [custom-pluggable-serde-registration](https://ui.docs.kafbat.io/configuration/serialization-serde#custom-pluggable-serde-registration)

## 적용 가이드

### 패키지 경로
[release 페이지에서 확인 가능](https://github.com/hellices/kafkaui-glue-apicurio-serde/releases)

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
      filePath: /shared/kafkaui-glue-apicurio-serde-1.0-SNAPSHOT-jar-with-dependencies.jar
      properties:
        endpoint: ${apicurio url}
```

### kafka-ui 컨테이너에서 init-container로 download

```yaml
initContainers:
- name: init-container
  image: busybox
  command:
    - /bin/sh
    - -c
    - cd /shared && wget https://github.com/hellices/kafkaui-glue-apicurio-serde/releases/download/v1.0.0-SNAPSHOT/kafbatui-glue-apicurio-serde-1.0-SNAPSHOT-jar-with-dependencies.jar
  volumeMounts:
    - name: shared-data
      mountPath: /shared  # 파일 다운로드 경로
containers:
  - name: kafka-ui
    volumeMounts:
    - name: shared-data
      mountPath: /shared  # 같은 경로에 공유된 볼륨 마운트
```