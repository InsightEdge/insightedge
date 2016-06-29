package com.gigaspaces.spark.java.context;

import com.gigaspaces.spark.context.GigaSpacesSparkContext;
import com.gigaspaces.spark.implicits;
import com.gigaspaces.spark.model.GridModel;
import com.gigaspaces.spark.rdd.GigaSpacesRDD;
import com.gigaspaces.spark.utils.GigaSpaceUtils;
import com.gigaspaces.spark.utils.JavaApiHelper;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Option;
import scala.reflect.ClassTag;

/**
 * @author Oleksiy_Dyagilev
 */
public class JavaGigaSpacesSparkContext {

    private GigaSpacesSparkContext gigaSpacesSparkContext;

    public JavaGigaSpacesSparkContext(JavaSparkContext javaSparkContext) {
        SparkContext sc = javaSparkContext.sc();
        this.gigaSpacesSparkContext = implicits.gigaSpacesSparkContext(sc);
    }

    // TODO: overloaded methods
    public <T extends GridModel> JavaRDD<T> gridRdd(Class<T> gridClass) {
        Option<Object> splitCount = Option.apply((Object) GigaSpaceUtils.DefaultSplitCount());
        Integer readRddBufferSize = GigaSpacesSparkContext.DefaultReadRddBufferSize();
        ClassTag<T> classTag = JavaApiHelper.getClassTag(gridClass);
        GigaSpacesRDD<T> gigaSpacesRDD = gigaSpacesSparkContext.gridRdd(splitCount, readRddBufferSize, classTag);
        return JavaRDD.fromRDD(gigaSpacesRDD, classTag);
    }


}
