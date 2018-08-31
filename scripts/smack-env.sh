#!/usr/bin/env bash

export MESOS_HOST=127.0.0.1
export SENTRY_DNS=""
export DO_TOKEN=""
export DO_SSH_KEY_ID=0
export DO_IMAGE_ID=0
export APP_PATH=/app
export SPARK_HOME=/opt/spark
export KAFKA_HOME=/opt/kafka
export MAX_DROPLETS=10
export DO_DOMAIN="do.eciavatta.it"
export VERSION="0.3.0-SNAPSHOT"
export SPARK_LOCAL_IP=$MESOS_HOST
export MESOS_NATIVE_JAVA_LIBRARY=/usr/local/lib/libmesos.so