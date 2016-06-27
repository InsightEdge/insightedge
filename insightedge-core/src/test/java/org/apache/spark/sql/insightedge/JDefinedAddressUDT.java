package org.apache.spark.sql.insightedge;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.Row$;
import org.apache.spark.sql.types.*;
import scala.collection.JavaConversions;

import static java.util.Arrays.asList;

/**
 * Converter for JDefinedAddress, sorry for the name, it's kinda convention is spark to name it SomethingUDT
 */
public class JDefinedAddressUDT extends UserDefinedType<JDefinedAddress> {
    @Override
    public DataType sqlType() {
        return new StructType(new StructField[]{
                new StructField("city", StringType$.MODULE$, true, Metadata.empty()),
                new StructField("state", StringType$.MODULE$, true, Metadata.empty()),
        });
    }

    @Override
    public Object serialize(Object obj) {
        JDefinedAddress address = (JDefinedAddress) obj;
        Object[] fields = {address.getCity(), address.getState()};
        return Row$.MODULE$.apply(JavaConversions.asScalaBuffer(asList(fields)).toSeq());
    }

    @Override
    public JDefinedAddress deserialize(Object datum) {
        if (datum instanceof Row) {
            Row row = (Row) datum;
            String city = (String) row.get(0);
            String state = (String) row.get(1);
            return new JDefinedAddress(city, state);
        } else {
            throw new IllegalArgumentException("expected datum to be a Row, got: " + datum.getClass().getName());
        }
    }

    @Override
    public Class<JDefinedAddress> userClass() {
        return JDefinedAddress.class;
    }
}
