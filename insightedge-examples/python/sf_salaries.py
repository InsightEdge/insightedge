import sys
import os
from pyspark.sql import SparkSession

# InsightEdge config
if len(sys.argv) == 1:
    spaceName = os.environ['INSIGHTEDGE_SPACE_NAME']
else:
    spaceName = sys.argv[1]

print("InsightEdge config: %s" % spaceName)

spark = SparkSession \
    .builder \
    .appName("SF Salaries Example") \
    .config("spark.insightedge.space.name", spaceName) \
    .getOrCreate()


# load SF salaries dataset from file
jsonFilePath = os.path.join(os.environ["XAP_HOME"], "insightedge/data/sf_salaries_sample.json")
jsonDf = spark.read.json(jsonFilePath)

# save DataFrame to the grid
jsonDf.write.format("org.apache.spark.sql.insightedge").mode("overwrite").save("salaries")

# load DataFrame from the grid
gridDf = spark.read.format("org.apache.spark.sql.insightedge").option("collection", "salaries").load()
gridDf.printSchema()

# register this DataFrame as a table
gridDf.registerTempTable("salaries")

# run SQL query
averagePay = spark.sql(
    """SELECT JobTitle, AVG(TotalPay) as AveragePay
       FROM salaries
       WHERE Year = 2012
       GROUP BY JobTitle
       ORDER BY AVG(TotalPay) DESC
       LIMIT 15""")

for each in averagePay.collect():
    print("%s: %s" % (each[0], each[1]))

spark.stop()