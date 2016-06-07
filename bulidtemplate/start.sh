#!/bin/sh
baseDir=$(cd "$(dirname "$0")"; pwd)
cp=.
for file in $baseDir/libs/*.jar
do
   cp=$cp:$file
done
java -server -Xmx40m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/pmes/Java/dump -Xss256k -cp $cp com.puxtech.reuters.rfa.RelayServer.RelayServer >> $baseDir/stdout.out 2>&1 &
#Xms should be equal with Xmx
#
