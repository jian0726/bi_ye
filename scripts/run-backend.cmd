@echo off
setlocal
set JAVA_HOME=D:\Java21
set PATH=D:\Java21\bin;D:\apache-maven-3.9.9\bin;%PATH%
cd /d D:\quanbudaima\bi_ye_she_ji\blog-backend
java -Dfile.encoding=UTF-8 -Dserver.port=8080 -jar target\blog-backend.jar 1> D:\quanbudaima\bi_ye_she_ji\.backend.log 2>&1
