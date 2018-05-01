#!/usr/bin/env bash

INPUT=$1
OUTPUT=$2
NUMBER=$3

for entry in `find $1 -name "*.json" -type f -exec ls {} \;`; do
    OUT=`basename $entry`
    head -n $NUMBER $entry > $OUTPUT/$OUT
done
