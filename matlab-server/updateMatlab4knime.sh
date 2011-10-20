#!/bin/sh

knimeRoot=$1
matlabJar="matlab-server-1.1.jar"

echo " "
echo "Working directory:"`pwd`
echo " "

mvn clean

mvn package -Dmaven.test.skip=true

echo " "
echo "Removing old binaries in 'matlab4knime/lib/'"
echo " "
mvn dependency:copy-dependencies
rm $knimeRoot/matlab4knime/lib/*.jar
echo " "
echo "Copying new binaries into 'matlab4knime/lib/'"
cp target/dependency/* $knimeRoot/matlab4knime/lib/
cp target/$matlabJar $knimeRoot/matlab4knime/lib/
echo " "
echo "Copying new binaries into 'target/bin/'"
mkdir target/bin/
cp target/dependency/* target/bin/
cp target/$matlabJar target/bin/
cp src/main/bin/startserver.m target/bin/

# remove missing jars from lib-folder   http://codesnippets.joyent.com/posts/show/551
