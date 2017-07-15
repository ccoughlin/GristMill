#!/bin/bash

# gristmill - runs the GristMill Region Of Interest (ROI) detection app

# Check if JAVA_HOME is set for maven
if [ -n "$JAVA_HOME" ];
then
        # Check for maven
        command -v mvn >/dev/null 2>&1 || { echo >&2 "mvn not found; please add to PATH"; exit 1; }
        mvn exec:java -Dexec.mainClass="com.emphysic.myriad.gristmill.Main" -Dexec.args=gristmill.conf
else
        echo "Maven requires the enviroment variable JAVA_HOME to be set"
        echo "e.g. export JAVA_HOME=/home/username/jdk"
        exit 1
fi