#!/bin/bash

export SBT_OPTS="-XX:MaxPermSize=256m -Xmx4G"

sbt assembly
