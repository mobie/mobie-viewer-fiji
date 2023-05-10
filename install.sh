#!/bin/bash
# This script is shamelessly adapted from https://github.com/saalfeldlab/n5-utils
# thanks @axtimwalde & co!

JAVA_HOME="/Users/tischer/Library/Java/JavaVirtualMachines/corretto-1.8.0_312/Contents/Home"
VERSION="4.0.4-SNAPSHOT"
MEM=8

mvn clean install -Denforcer.skip -Dmaven.test.skip=true
mvn -Dmdep.outputFile=cp.txt -Dmdep.includeScope=runtime dependency:build-classpath

echo '#!/bin/bash' > mobie-files
echo '' >> mobie-files
echo "JAR=\$HOME/.m2/repository/org/embl/mobie/mobie-viewer-fiji/${VERSION}/mobie-viewer-fiji-${VERSION}.jar" >> mobie-files
echo "${JAVA_HOME}/bin/java \\" >> mobie-files
echo "  -Xmx${MEM}g \\" >> mobie-files
echo '  -XX:+UseConcMarkSweepGC \' >> mobie-files
echo -n '  -cp $JAR:' >> mobie-files
echo -n $(cat cp.txt) >> mobie-files
echo ' \' >> mobie-files
echo '  org.embl.mobie.cmd.FilesCmd "$@"' >> mobie-files
chmod a+x mobie-files
echo "Installed mobie-files in current directory."
echo "Execute ./mobie-files to see all options"

echo '#!/bin/bash' > mobie-table
echo '' >> mobie-table
echo "JAR=\$HOME/.m2/repository/org/embl/mobie/mobie-viewer-fiji/${VERSION}/mobie-viewer-fiji-${VERSION}.jar" >> mobie-table
echo "${JAVA_HOME}/bin/java \\" >> mobie-table
echo "  -Xmx${MEM}g \\" >> mobie-table
echo '  -XX:+UseConcMarkSweepGC \' >> mobie-table
echo -n '  -cp $JAR:' >> mobie-table
echo -n $(cat cp.txt) >> mobie-table
echo ' \' >> mobie-table
echo '  org.embl.mobie.cmd.TableCmd "$@"' >> mobie-table
chmod a+x mobie-table
echo "Installed mobie-table in current directory."
echo "Execute ./mobie-table to see all options"

echo '#!/bin/bash' > mobie-hcs
echo '' >> mobie-hcs
echo "JAR=\$HOME/.m2/repository/org/embl/mobie/mobie-viewer-fiji/${VERSION}/mobie-viewer-fiji-${VERSION}.jar" >> mobie-hcs
echo "${JAVA_HOME}/bin/java \\" >> mobie-hcs
echo "  -Xmx${MEM}g \\" >> mobie-hcs
echo '  -XX:+UseConcMarkSweepGC \' >> mobie-hcs
echo -n '  -cp $JAR:' >> mobie-hcs
echo -n $(cat cp.txt) >> mobie-hcs
echo ' \' >> mobie-hcs
echo '  org.embl.mobie.cmd.HCSCmd "$@"' >> mobie-hcs
chmod a+x mobie-hcs
echo "Installed mobie-hcs in current directory."
echo "Execute ./mobie-hcs to see all options"

echo '#!/bin/bash' > mobie-project
echo '' >> mobie-project
echo "JAR=\$HOME/.m2/repository/org/embl/mobie/mobie-viewer-fiji/${VERSION}/mobie-viewer-fiji-${VERSION}.jar" >> mobie-project
echo "${JAVA_HOME}/bin/java \\" >> mobie-project
echo "  -Xmx${MEM}g \\" >> mobie-project
echo '  -XX:+UseConcMarkSweepGC \' >> mobie-project
echo -n '  -cp $JAR:' >> mobie-project
echo -n $(cat cp.txt) >> mobie-project
echo ' \' >> mobie-project
echo '  org.embl.mobie.cmd.ProjectCmd "$@"' >> mobie-project
chmod a+x mobie-project
echo "Installed mobie-project in current directory."
echo "Execute ./mobie-project to see all options"

rm cp.txt

# Examples
#
# ./mobie-files -r "/g/cba/exchange/agata-misiaszek/data/analysed" -i "nuclei=.*.ome.tif" -l "labels=.*.ome_cp_masks.tif" --remove-spatial-calibration
#
# ./mobie-table -r "/g/cba/exchange/agata-misiaszek/data/analysed" -t "/g/cba/exchange/agata-misiaszek/data/analysed/Image.txt" -i "DAPI=FileName_DNA;0" -i "RPAC1=FileName_DNA;1" -l "CytoSeg=FileName_CytoplasmLabels" -l "NucleiSeg=ObjectsFileName_Nuclei" --remove-spatial-calibration
#
# ./mobie-project -p "https://github.com/mobie/platybrowser-datasets" -v "cells"
#
# ./mobie-hcs TODO

