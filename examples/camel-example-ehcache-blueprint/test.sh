#!/bin/sh

TIMES=`seq 1 3`
for i in $TIMES
do
    curl http://localhost:8181/camel-example-ehcache-blueprint/data/$i
done

for i in $TIMES
do
    curl http://localhost:8181/camel-example-ehcache-blueprint/data/$i
done
