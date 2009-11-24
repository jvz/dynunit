package atg.adapter.gsa;

import java.util.HashMap;
import java.util.List;

public class SchemaTracker {

  public  HashMap<String,List<GSARepository>> mTableToRepository = new HashMap<String,List<GSARepository>>();
  
  /**
   * @return the tableToRepository
   */
  public HashMap<String,List<GSARepository>> getTableToRepository() {
    return mTableToRepository;
  }

  /**
   * @param pTableToRepository the tableToRepository to set
   */
  public void setTableToRepository(
      HashMap<String, List<GSARepository>> pTableToRepository) {
    mTableToRepository = pTableToRepository;
  }

  private static SchemaTracker sSchemaTracker = null;
  private SchemaTracker() {}
  
  public static SchemaTracker getSchemaTracker() {
    if (sSchemaTracker == null)
      sSchemaTracker = new SchemaTracker();
    return sSchemaTracker;
  }

  /**
   * Resets the state in this class.
   */
  public void reset() {
    mTableToRepository.clear();
  }
}
