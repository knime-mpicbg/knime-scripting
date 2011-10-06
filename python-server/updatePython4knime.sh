#!/bin/sh

if [ $# -lt 1 ]; then
   echo Usage: $0 knime_root
   exit 1
fi

knimeRoot=$1
echo "Working directory:"`pwd`
echo " "


mvn package -Dmaven.test.skip=true

rm -rf target/dependency
mvn dependency:copy-dependencies

rm $knimeRoot/python4knime/lib/*.jar
cp target/dependency/* $knimeRoot/python4knime/lib/
cp target/mpicbg-python-1.1.jar $knimeRoot/python4knime/lib/

# remove missing jars from lib-folder   http://codesnippets.joyent.com/posts/show/551
