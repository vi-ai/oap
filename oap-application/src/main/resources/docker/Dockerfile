FROM openjdk:8-jre-alpine

ENV SERVICE_NAME=${oap.service.name}
ENV SERVICE_WORK_DIR=/opt/${oap.service.name}
ENV SERVICE_CONFIG_DIR=/opt/${oap.service.name}/conf
ENV SERVICE_KEYS_DIR=/opt/${oap.service.name}/keys
ENV SERVICE_DATA_DIR=/var/lib/${oap.service.name}
ENV SERVICE_LOG_DIR=/var/log/${oap.service.name}

WORKDIR $SERVICE_WORK_DIR

ENV UID 0

# copy keys
COPY src/opt/${oap.service.name}/keys/* /opt/${oap.service.name}/keys/

# copy libs
COPY src/opt/${oap.service.name}/lib/*.jar /opt/${oap.service.name}/lib/

# copy shell scripts
COPY src/opt/${oap.service.name}/bin/*.sh /opt/${oap.service.name}/bin/


VOLUME $SERVICE_CONFIG_DIR
VOLUME $SERVICE_DATA_DIR
VOLUME $SERVICE_LOG_DIR


ENTRYPOINT ["/opt/${oap.service.name}/bin/${oap.service.name}.sh", "--run"]