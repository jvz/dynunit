package atg.adapter.gsa;

import java.sql.Connection;

/**
 * Interface for code that should be executed with autoCommit=true.
 * @version $Id: //test/UnitTests/base/main/src/Java/atg/adapter/gsa/AutoCommitable.java#1 $
 * @author adamb
 *
 */
public interface AutoCommitable {
  /**
   * Work to be done withing the scope of an autoCommit transaction
   * should be placed into the implementation of this method.
   * Hand of the implementation (typically via Anonymous Inner Class) to an
   * instance of <code>DoInAutoCommitLand.doInAutoCommit</code>
   * @param pConnection
   */
 public void doInAutoCommit(Connection pConnection);
}
