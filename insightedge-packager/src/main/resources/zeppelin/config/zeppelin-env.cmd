@echo off

REM Licensed to the Apache Software Foundation (ASF) under one or more
REM contributor license agreements.  See the NOTICE file distributed with
REM this work for additional information regarding copyright ownership.
REM The ASF licenses this file to You under the Apache License, Version 2.0
REM (the "License"); you may not use this file except in compliance with
REM the License.  You may obtain a copy of the License at
REM
REM    http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM

REM set JAVA_HOME=
REM set MASTER=                 		REM Spark master url. eg. spark://master_addr:7077. Leave empty if you want to use local mode.
REM set ZEPPELIN_JAVA_OPTS      		REM Additional jvm options. for example, set ZEPPELIN_JAVA_OPTS="-Dspark.executor.memory=8g -Dspark.cores.max=16"
REM set ZEPPELIN_MEM            		REM Zeppelin jvm mem options Default -Xmx1024m -XX:MaxPermSize=512m
REM set ZEPPELIN_INTP_MEM       		REM zeppelin interpreter process jvm mem options. Default = ZEPPELIN_MEM
REM set ZEPPELIN_INTP_JAVA_OPTS 		REM zeppelin interpreter process jvm options. Default = ZEPPELIN_JAVA_OPTS

REM set ZEPPELIN_LOG_DIR        		REM Where log files are stored.  PWD by default.
REM set ZEPPELIN_PID_DIR        		REM The pid files are stored. /tmp by default.
REM set ZEPPELIN_WAR_TEMPDIR    		REM The location of jetty temporary directory.
REM set ZEPPELIN_NOTEBOOK_DIR   		REM Where notebook saved
REM set ZEPPELIN_NOTEBOOK_HOMESCREEN		REM Id of notebook to be displayed in homescreen. ex) 2A94M5J1Z
REM set ZEPPELIN_NOTEBOOK_HOMESCREEN_HIDE	REM hide homescreen notebook from list when this value set to "true". default "false"
REM set ZEPPELIN_NOTEBOOK_S3_BUCKET            REM Bucket where notebook saved
REM set ZEPPELIN_NOTEBOOK_S3_USER              REM User in bucket where notebook saved. For example bucket/user/notebook/2A94M5J1Z/note.json
REM set ZEPPELIN_IDENT_STRING   		REM A string representing this instance of zeppelin. $USER by default.
REM set ZEPPELIN_NICENESS       		REM The scheduling priority for daemons. Defaults to 0.
REM set ZEPPELIN_INTERPRETER_LOCALREPO         REM Local repository for interpreter's additional dependency loading
REM set ZEPPELIN_NOTEBOOK_STORAGE		REM Refers to pluggable notebook storage class, can have two classes simultaneously with a sync between them (e.g. local and remote).


REM Spark interpreter configuration

REM Use provided spark installation
REM defining SPARK_HOME makes Zeppelin run spark interpreter process using spark-submit
REM
REM set SPARK_HOME                             REM (required) When it is defined, load it instead of Zeppelin embedded Spark libraries
REM set SPARK_SUBMIT_OPTIONS                   REM (optional) extra options to pass to spark submit. eg) "--driver-memory 512M --executor-memory 1G".
REM set SPARK_APP_NAME                         REM (optional) The name of spark application.

rem setlocal enableextensions enabledelayedexpansion

if "x%INSIGHTEDGE_HOME%"=="x" (
   set INSIGHTEDGE_HOME=%~dp0..
)

call "%INSIGHTEDGE_HOME%\sbin\common-insightedge.cmd" GET_LIBS "," "false"

set SPARK_HOME=%INSIGHTEDGE_HOME%
set SPARK_SUBMIT_OPTIONS=--jars %INSIGHTEDGE_JARS%

REM Use embedded spark binaries
REM without SPARK_HOME defined, Zeppelin still able to run spark interpreter process using embedded spark binaries.
REM however, it is not encouraged when you can define SPARK_HOME
REM
REM Options read in YARN client mode
REM set HADOOP_CONF_DIR         		REM yarn-site.xml is located in configuration directory in HADOOP_CONF_DIR.
REM Pyspark (supported with Spark 1.2.1 and above)
REM To configure pyspark, you need to set spark distribution's path to 'spark.home' property in Interpreter setting screen in Zeppelin GUI
REM set PYSPARK_PYTHON          		REM path to the python command. must be the same path on the driver(Zeppelin) and all workers.
REM set PYTHONPATH

REM Spark interpreter options
REM
REM set ZEPPELIN_SPARK_USEHIVECONTEXT  REM Use HiveContext instead of SQLContext if set true. true by default.
REM set ZEPPELIN_SPARK_CONCURRENTSQL   REM Execute multiple SQL concurrently if set true. false by default.
REM set ZEPPELIN_SPARK_IMPORTIMPLICIT  REM Import implicits, UDF collection, and sql if set true. true by default.
REM set ZEPPELIN_SPARK_MAXRESULT       REM Max number of SparkSQL result to display. 1000 by default.

REM ZeppelinHub connection configuration
REM
REM set ZEPPELINHUB_API_ADDRESS	       REM Refers to the address of the ZeppelinHub service in use
REM set ZEPPELINHUB_API_TOKEN          REM Refers to the Zeppelin instance token of the user
REM set ZEPPELINHUB_USER_KEY           REM Optional, when using Zeppelin with authentication.
