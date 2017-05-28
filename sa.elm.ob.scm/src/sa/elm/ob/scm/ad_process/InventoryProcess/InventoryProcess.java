package sa.elm.ob.scm.ad_process.InventoryProcess;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

public class InventoryProcess implements Process {

  private static final Logger log = Logger.getLogger(InventoryProcess.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    @SuppressWarnings("unused")
    Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }
    String inventoryId = (String) bundle.getParams().get("M_Inventory_ID");
    InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, inventoryId);

    OBQuery<InventoryCountLine> lines = OBDal.getInstance().createQuery(InventoryCountLine.class,
        "physInventory.id='" + inventoryId + "'");
    if (lines.list() == null || lines.list().size() == 0) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Nolines_Inventory@");
      bundle.setResult(result);
      return;
    }
    try {
      OBContext.setAdminMode(true);
      inventory.setUpdated(new java.util.Date());
      inventory.setUpdatedBy(OBContext.getOBContext().getUser());
      inventory.setEscmStatus("CO");
      OBDal.getInstance().save(inventory);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Inventory_sucess@");
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception in Inventory counting process:", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
