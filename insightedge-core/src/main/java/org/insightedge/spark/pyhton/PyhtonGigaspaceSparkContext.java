package org.insightedge.spark.pyhton;

import org.insightedge.spark.context.InsightEdgeSparkContext;
import org.insightedge.spark.mllib.MLInstance;

public class PyhtonGigaspaceSparkContext {

    private InsightEdgeSparkContext ieContext;

    public PyhtonGigaspaceSparkContext(InsightEdgeSparkContext ieContext) {
        this.ieContext = ieContext;
    }

    public void saveMlInstance(String name, Object model) {
        ieContext.grid().write(new MLInstance(name, model));
    }

    public Object loadMlInstance(String name, String className) {
        try {
            Class clazz = Class.forName(className);
            MLInstance mlModel = ieContext.grid().readById(MLInstance.class, name);
            return clazz.cast(mlModel.getInstance());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find class", e);
        }
    }

}
