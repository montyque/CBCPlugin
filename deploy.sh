#!/usr/bin/env bash
set -e

LOCAL_JAR="build/libs/cbcplugin_new-2.0.0-SNAPSHOT-all.jar"
REMOTE_USER="emontecl.8fc92513"
REMOTE_HOST="NA-49.enviromc.host"
REMOTE_PATH="/plugins/cbcplugin_new-2.0.0-SNAPSHOT-all.jar"

# Upload the JAR
scp -P 2022 "$LOCAL_JAR" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}"