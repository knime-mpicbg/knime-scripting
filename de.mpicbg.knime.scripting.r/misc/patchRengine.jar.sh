#!/bin/sh


# extract the jar contents
jar xf REngine-0.6.jar



# repackage the jar
jar cf REngine-0.6.jar META-INF org

#copy patched classes into expaneded jar
..finder..

mv  REngine-0.6.jar ..