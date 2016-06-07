#!/bin/sh
baseDir=$(cd "$(dirname "$0")"; pwd)
cp=.
for file in $baseDir/libs/*.jar
do
   cp=$cp:$file
done
$baseDir/jdk1.6.0_25/bin/java -jar $baseDir/configLoader/config_loader.jar LoadPrefsConfig -file $baseDir/configLoader/sessionConfig.xml