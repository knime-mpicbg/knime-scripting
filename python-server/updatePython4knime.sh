#!/bin/sh

pythonRoot=$1
pythonJar="python-server-1.1.jar"

echo " "
echo "Working directory:"`pwd`
echo " "

mvn clean

mvn package -Dmaven.test.skip=true

echo " "
echo "Removing old binaries in 'python4knime/lib/'"
echo " "
mvn dependency:copy-dependencies
rm $pyhtonRoot/lib/*.jar
echo " "
echo "Copying new binaries into 'python4knime/lib/'"
cp target/dependency/* $pythonRoot/lib/
cp target/$pythonJar $pythonRoot/lib/
echo " "
echo "Copying new binaries into 'target/bin/'"
mkdir target/bin/
cp target/dependency/* target/bin/
cp target/$pythonJar target/bin/
cp bin/pyserver.sh target/bin/

# remove missing jars from lib-folder   http://codesnippets.joyent.com/posts/show/551
