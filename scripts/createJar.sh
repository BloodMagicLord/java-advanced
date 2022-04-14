#!/bin/bash
path=info/kgeorgiy/java/advanced/implementor

jar xf ../../java-advanced-2022/artifacts/info.kgeorgiy.java.advanced.implementor.jar $path/Impler.class $path/JarImpler.class $path/ImplerException.class

javac -d out --module-path ../../java-advanced-2021/artifacts ../java-solutions/info/kgeorgiy/ja/Maksonov/implementor/Implementor.java

cd ./out

jar cfm Implementor.jar ../MANIFEST.MF info/kgeorgiy/ja/Maksonov/implementor/Implementor.class ../$path/Impler.class ../$path/JarImpler.class ../$path/ImplerException.class

