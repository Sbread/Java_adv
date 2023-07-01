#!/bin/bash

MODULE=info.kgeorgiy.ja.osipov.implementor
DIR=info/kgeorgiy/ja/osipov/implementor

# экранирование

mkdir "$MODULE"
cp ../java-solutions/module-info.java ${MODULE}
cp MANIFEST.MF ${MODULE}
mkdir --parents ${MODULE}/${DIR}
cp ../java-solutions/"$DIR"/*.java ${MODULE}/${DIR}

javac \
	-d ./javac_files \
	-p ../../java-advanced-2023/artifacts/:../../java-advanced-2023/lib \
	--module-source-path . \
	--module ${MODULE}

jar -c -f Implementor.jar -m ${MODULE}/MANIFEST.MF -C javac_files/${MODULE} .
rm -rf ${MODULE}
rm -rf javac_files
