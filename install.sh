#!/bin/bash
# This script is shamelessly adapted from https://github.com/saalfeldlab/n5-utils, thanks @axtimwalde & co!

VERSION="3.0.7-SNAPSHOT"
MEM=8 # FIXME

mvn clean install -Denforcer.skip
mvn -Dmdep.outputFile=cp.txt -Dmdep.includeScope=runtime dependency:build-classpath

echo '#!/bin/bash' > mobie
echo '' >> mobie
echo "JAR=\$HOME/.m2/repository/org/embl/mobie/mobie-viewer-fiji/${VERSION}/mobie-viewer-fiji-${VERSION}.jar" >> mobie
echo 'java \' >> mobie
echo "  -Xmx${MEM}g \\" >> mobie
echo '  -XX:+UseConcMarkSweepGC \' >> mobie
echo -n '  -cp $JAR:' >> mobie
echo -n $(cat cp.txt) >> mobie
echo ' \' >> mobie
echo '  org.embl.mobie.cmd.MoBIECommandLineInterface "$@"' >> mobie
chmod a+x mobie
echo ""
echo "Installed MoBIE in current directory."
echo "Type ./mobie to see all options"
echo "Example call:"
echo "./mobie -i \"./src/test/resources/golgi-intensities.tif\" -s \"./src/test/resources/golgi-cell-labels.tif\""
echo ""

rm cp.txt