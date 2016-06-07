#!/bin/sh
baseDir=$(cd "$(dirname "$0")"; pwd)
cp=.
for file in $baseDir/libs/*.jar
do
   cp=$cp:$file
done
java -jar $baseDir/configLoader/config_loader.jar LoadPrefsConfig -file $baseDir/configLoader/sessionConfig.xml
