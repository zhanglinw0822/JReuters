#!/bin/sh
baseDir=$(cd "$(dirname "$0")"; pwd)
cp=.
for file in $baseDir/libs/*.jar
do
   cp=$cp:$file
done
java -server -Xmx5120m -Xss128k -cp $cp com.puxtech.reuters.rfa.RelayServer.RelayServer >> $baseDir/stdout.out 2>&1 &