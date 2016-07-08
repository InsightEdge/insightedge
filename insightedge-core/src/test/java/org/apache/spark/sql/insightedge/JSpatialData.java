package org.apache.spark.sql.insightedge;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.openspaces.spatial.shapes.Point;

/**
 * Space class for tests
 */
@SpaceClass
public class JSpatialData {
    private String id;
    private Long routing;
    private Point point;

    public JSpatialData() {
    }

    public JSpatialData(Long routing, Point point) {
        this.routing = routing;
        this.point = point;
    }

    @SpaceId(autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @SpaceRouting
    public Long getRouting() {
        return routing;
    }

    public void setRouting(Long routing) {
        this.routing = routing;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }
}
