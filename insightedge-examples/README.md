# InsightEdge Examples

This repository contains example [InsightEdge](https://github.com/InsightEdge/insightedge) applications.

## Requirements
* Java 1.8
* Scala 2.10
* Maven 3.1+
* SBT 0.13.9+ (optional)
* InsightEdge distribution


## Building project

InsightEdge jars are not published to Maven Central Repository yet. To install artifacts to your local Maven repository, make sure you have Maven installed and then run the following from InsightEdge directory:
```bash
# Linux:
./insightedge/tools/maven/insightedge-maven.sh

# Windows:
insightedge\tools\maven\insightedge-maven.cmd
```

This project has both SBT and Maven build files. You can build it with the next commands:

```bash
# Maven
mvn clean test package

# SBT
sbt clean test assembly
```


## Running examples

There are several options how you can run examples:

* from Web Notebook
* from your IDE
* from a command line

## Configure insightedge/bin/setenv-overrides
 
##### Linux
Add to /insightedge/bin/setenv-overrides.sh:
```bash
export SPARK_LOCAL_IP=127.0.0.1
```
##### Windows
Add to /insightedge/bin/setenv-overrides.bat:
```bash
set SPARK_LOCAL_IP=127.0.0.1
```

#### Starting local environment



Prior to executing example application, you have to start the Data Grid and deploy an empty space on it. You can do it using `demo` mode:
```bash
# Linux:
./insightedge/bin/insightedge demo

# Windows:
insightedge\bin\insightedge.bat demo
```

Such command will start next components:
* Spark master at `spark://127.0.0.1:7077` and Spark slave
* Spark monitoring UI at `127.0.0.1:8080`
* Data Grid manager and two containers with `1G` heap each
    - space is deployed with name `demo`

#### Web Notebook 
  		  
The interactive Web Notebook (based on Zeppelin) is started automatically in `demo` mode at http://127.0.0.1:8090. For more information, refer to 'Zeppelin Notebook' section in [InsightEdge documentation](http://insightedge.io/docs) 
 
#### Running from IDE

You can run examples from your favourite IDE. Every example has a `main` method, so it can be executed as standard application. There two important things:

* enable `run-from-ide` maven profile (this will switch required dependencies to `compile` scope so they are available in classpath)
* spark master url should be set to `local[*]` so that Spark is started in embedded mode

Here is an example of run configuration for `SaveRDD` for `Intellij Idea`:
![IDEA run configuration](doc/images/idea-configuration_1.png?raw=true)

With this configuration, example will run on local Spark cluster and save the generated RDD to Data Grid to specified space.

#### Running from command line

You can build the project and submit examples as Spark applications with the next command:
```bash
# Linux:
./insightedge/bin/insightedge-submit --class {main class name} --master {Spark master URL} \
    {insightedge-examples.jar location} \
    {Spark master URL} {space name} {lookup group} {lookup locator}

# Windows:
insightedge\bin\insightedge-submit --class {main class name} --master {Spark master URL} ^
    {insightedge-examples.jar location} ^
    {Spark master URL} {space name} {lookup group} {lookup locator}
```

For example, `SaveRDD` can be submitted with the next syntax:
```bash
# Linux:
./insightedge/bin/insightedge-submit --class org.insightedge.examples.basic.SaveRdd --master spark://127.0.0.1:7077 \
    ./insightedge/examples/jars/insightedge-examples.jar

# Windows:
insightedge\bin\insightedge-submit --class org.insightedge.examples.basic.SaveRdd --master spark://127.0.0.1:7077 ^
    insightedge\quickstart\scala\insightedge-examples.jar
```

If you are running local cluster with default settings (see [Running Examples](#running-examples)), you can omit arguments:
```bash
# Linux:
./insightedge/bin/insightedge-submit --class {main class name} --master {Spark master URL} \
    {insightedge-examples.jar location}

# Windows:
insightedge\bin\insightedge-submit --class {main class name} --master {Spark master URL} ^
    {insightedge-examples.jar location}
```

> Note that running `TwitterPopularTags` example requires you to pass [Twitter app tokens](https://apps.twitter.com/) as arguments

#### Python examples

You can run Python examples with
```bash
# Linux:
./insightedge/bin/insightedge-submit --master {Spark master URL} {path to .py file}

# Windows:
insightedge\bin\insightedge-submit --master {Spark master URL} {path to .py file}
```

For example,
```bash
# Linux:
./insightedge/bin/insightedge-submit --master spark://127.0.0.1:7077 ./quickstart/python/sf_salaries.py

# Windows:
insightedge\bin\insightedge-submit --master spark://127.0.0.1:7077 quickstart\python\sf_salaries.py
```

#### Stopping local environment
##### open version
Ctrl C

##### Premium version
To stop all InsightEdge components, next command can be executed:
./insightedge/bin/insightedge.bat  host kill-agent



## Troubleshooting

If you have any troubles running the example applications, please, contact us with:
- Slack channel using [invitation](http://insightedge-slack.herokuapp.com/)
- StackOverflow [insightedge tag](http://stackoverflow.com/questions/tagged/insightedge)
- contact form at [InsightEdge main page](http://insightedge.io/)
- or [email message](mailto:hello@insightedge.io)
