package org.insightedge.internal.utils;

import com.gigaspaces.api.InternalApi;
import org.apache.spark.TaskContext;
import org.apache.spark.sql.SparkSession;
import scala.Option;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Niv Ingberg
 * @since 14.2
 */
@InternalApi
public class SparkSessionUtils {
    public static Option<SparkSession> getDefaultSparkSession() {
        Option<SparkSession> session = SparkSession.getDefaultSession();
        if (session.isDefined())
            return session;
        if (TaskContext.get() != null) {
            Future<Option<SparkSession>> future = Executors.newSingleThreadExecutor().submit(SparkSession::getDefaultSession);
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while getting default spark session", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Failed to default spark session", e.getCause());
            }
        }
        return Option.empty();
    }
}
