#!/usr/bin/env bash


PROJECT_NAME="proxy-server"
JAR_NAME="proxy-server-0.0.1-SNAPSHOT.jar"


function compile()
{
	echo "rebuild project"
	mvn clean package -Pprod install -pl proxy-server -am
}

function hasCodeUpdate()
{
	git fetch origin master
	diff=`git diff FETCH_HEAD`
	if [ ! -n "$diff" ] ;then
            return 0
	fi
	    echo "服务器代码更新"
		return 1
}

function pullNewCode()
{
    git fetch origin master
    git merge
    echo "拉取新代码完成"

}

function checkServer()
{
	target=`ps -ef | grep "${PROJECT_NAME}" | grep "java" | grep -v "grep"`
	if [ -n "${target}" ] ;then
        	echo "服务正常启动"
        	echo "${target}"
		return 0;
	fi

	#这句代码有问题,判断不生效
	if [ ! -f "target/${JAR_NAME}" ] ;then
		echo "项目执行文件不存在，重新编译"
		compile
	fi

	echo "start server"

    nohup java -jar ./${PROJECT_NAME}/target/${JAR_NAME} >/dev/null 2>&1 &


    target=`ps -ef | grep "${PROJECT_NAME}" | grep "java" | grep -v "grep"`
    if [ -n "$target" ] ;then
           echo "server start success"
           echo "$target"
                return 0
    else
            echo "new server start failed please check IT!!!"
            return 1
    fi
}

function killServer(){
    target=`ps -ef | grep "${PROJECT_NAME}" | grep "java"  | grep -v "grep"`
    if [ -n "$target" ] ;then
    	echo "kill old server process"
    	ps -ef | grep "${PROJECT_NAME}" | grep -v "grep" | awk '{print $2}' | xargs kill -9
    	echo `ps -ef | grep "${PROJECT_NAME}" | grep -v "grep"`

    fi
}



killServer