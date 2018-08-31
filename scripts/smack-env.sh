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

export DEBUG=false
export ENVIRONMENT=production
export LOG_LEVEL=warning

export CASSANDRA_NODE_INSTANCES=2
export KAFKA_NODE_INSTANCES=2
export SMACK_FRONTEND_INSTANCES=3
export SMACK_BACKEND_INSTANCES=3
export SMACK_SERVICE_INSTANCES=3

export LOG_TOPIC="logs"
export LOG_PARTITIONS=3
export LOG_REPLICATION_FACTOR=2

export SPARK_LOCAL_IP=127.0.0.1
export SPARK_PUBLIC_DNS=localhost
