#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
An interactive shell.

This file is designed to be launched as a PYTHONSTARTUP script.
"""

import atexit
import os
import platform
import sys

import py4j

import pyspark
from pyspark.context import SparkContext
from pyspark.sql import SparkSession, SQLContext
from pyspark.storagelevel import StorageLevel

    # InsightEdge config
if len(sys.argv) < 4:
    spaceName = "insightedge-space"
    lookupGroup = "xap-12.2.0"
    lookupLocator = "127.0.0.1:4174"
else:
    spaceName = sys.argv[1]
    lookupGroup = sys.argv[2]
    lookupLocator = sys.argv[3]

print("InsightEdge config: %s %s %s" % (spaceName, lookupGroup, lookupLocator))

if os.environ.get("SPARK_EXECUTOR_URI"):
    SparkContext.setSystemProperty("spark.executor.uri", os.environ["SPARK_EXECUTOR_URI"])

SparkContext._ensure_initialized()

try:
    # Try to access HiveConf, it will raise exception if Hive is not added
    SparkContext._jvm.org.apache.hadoop.hive.conf.HiveConf()
    spark = SparkSession.builder\
        .enableHiveSupport()\
        .config("spark.insightedge.space.name", spaceName) \
        .config("spark.insightedge.space.lookup.group", lookupGroup) \
        .config("spark.insightedge.space.lookup.locator", lookupLocator) \
        .getOrCreate()
except py4j.protocol.Py4JError:
    spark = SparkSession.builder.config("spark.insightedge.space.name", spaceName).config("spark.insightedge.space.lookup.group", lookupGroup).config("spark.insightedge.space.lookup.locator", lookupLocator).getOrCreate()
except TypeError:
    spark = SparkSession.builder.config("spark.insightedge.space.name", spaceName).config("spark.insightedge.space.lookup.group", lookupGroup).config("spark.insightedge.space.lookup.locator", lookupLocator).getOrCreate()

sc = spark.sparkContext
sql = spark.sql
atexit.register(lambda: sc.stop())

# for compatibility
sqlContext = spark._wrapped
sqlCtx = sqlContext

print("""Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /__ / .__/\_,_/_/ /_/\_\   version %s
      /_/
""" % sc.version)
print("Using Python version %s (%s, %s)" % (
    platform.python_version(),
    platform.python_build()[0],
    platform.python_build()[1]))
print("SparkSession available as 'spark'.")

# The ./bin/pyspark script stores the old PYTHONSTARTUP value in OLD_PYTHONSTARTUP,
# which allows us to execute the user's PYTHONSTARTUP file:
_pythonstartup = os.environ.get('OLD_PYTHONSTARTUP')
if _pythonstartup and os.path.isfile(_pythonstartup):
    with open(_pythonstartup) as f:
        code = compile(f.read(), _pythonstartup, 'exec')
        exec(code)
