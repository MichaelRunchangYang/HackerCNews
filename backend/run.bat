@echo off
REM 设置控制台编码为UTF-8
chcp 65001

REM 设置Java编码为UTF-8，并启动应用程序
mvn spring-boot:run

REM 如果需要在命令执行完成后保持窗口打开
pause 