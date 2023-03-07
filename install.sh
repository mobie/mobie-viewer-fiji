#!/bin/bash
# This script is shamelessly adapted from https://github.com/saalfeldlab/n5-utils, thanks @axtimwalde & co!
# /usr/libexec/java_home -V

JAVA_HOME="/Users/tischer/Library/Java/JavaVirtualMachines/corretto-1.8.0_312/Contents/Home"
VERSION=$1
MEM=8 # FIXME

mvn clean install -Denforcer.skip -Dmaven.test.skip=true
mvn -Dmdep.outputFile=cp.txt -Dmdep.includeScope=runtime dependency:build-classpath

echo '#!/bin/bash' > mobie
echo '' >> mobie
echo "JAR=\$HOME/.m2/repository/org/embl/mobie/mobie-viewer-fiji/${VERSION}/mobie-viewer-fiji-${VERSION}.jar" >> mobie
echo "${JAVA_HOME}/bin/java \\" >> mobie
echo "  -Xmx${MEM}g \\" >> mobie
echo '  -XX:+UseConcMarkSweepGC \' >> mobie
echo -n '  -cp $JAR:' >> mobie
echo -n $(cat cp.txt) >> mobie
echo ' \' >> mobie
echo '  org.embl.mobie.cmd.MoBIECmd "$@"' >> mobie
chmod a+x mobie
echo ""
echo "Installed MoBIE in current directory."
echo "Type ./mobie to see all options"
echo "Example call:"
echo "./mobie -i \"./src/test/resources/input/mlj-2d-tiff/image.tif\" -s \"./src/test/resources/input/mlj-2d-tiff/segmentation.tif\" -t \"./src/test/resources/input/mlj-2d-tiff/table-mlj.csv\""
echo ""

rm cp.txt