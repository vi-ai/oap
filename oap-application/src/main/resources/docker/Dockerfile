FROM openjdk:8-jre-alpine

ENV SERVICE_NAME=${oap.service.name}
ENV SERVICE_WORK_DIR=/opt/${oap.service.name}
ENV SERVICE_CONFIG_DIR=/opt/${oap.service.name}/conf
ENV SERVICE_KEYS_DIR=/opt/${oap.service.name}/keys
ENV SERVICE_DATA_DIR=/var/lib/${oap.service.name}
ENV SERVICE_LOG_DIR=/var/log/${oap.service.name}


ENV UID 0

RUN mkdir -p $SERVICE_WORK_DIR

WORKDIR $SERVICE_WORK_DIR

# copy directories structure
COPY src/ /

VOLUME $SERVICE_CONFIG_DIR
VOLUME $SERVICE_KEYS_DIR
VOLUME $SERVICE_DATA_DIR
VOLUME $SERVICE_LOG_DIR



ENTRYPOINT ["/opt/${oap.service.name}/bin/${oap.service.name}.sh", "--run"]