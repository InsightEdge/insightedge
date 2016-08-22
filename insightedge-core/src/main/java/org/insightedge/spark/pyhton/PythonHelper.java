package org.insightedge.spark.pyhton;

import org.apache.spark.api.java.JavaSparkContext;
import org.insightedge.spark.context.InsightEdgeSparkContext;

public class PythonHelper {

    public PyhtonGigaspaceSparkContext createPythonGigaSpacesSparkContext(JavaSparkContext jsc) {
        return new PyhtonGigaspaceSparkContext(new InsightEdgeSparkContext(jsc.sc()));
    }

}
