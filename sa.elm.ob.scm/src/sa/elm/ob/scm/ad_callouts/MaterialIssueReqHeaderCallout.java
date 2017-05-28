package sa.elm.ob.scm.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.hibernate.criterion.Expression;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import sa.elm.ob.scm.MaterialIssueRequest;

/*
 * automatically fill the signature group fields based on last record and organization.
 */
public class MaterialIssueReqHeaderCallout extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String inpLastFieldChanged = info.vars.getStringParameter("inpLastFieldChanged");
    String strOrgid = info.vars.getStringParameter("inpadOrgId");
    String strwarehouse = info.vars.getStringParameter("inpmWarehouseId");
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    if (inpLastFieldChanged.equals("inpadOrgId")) {
      try {
        OBCriteria<MaterialIssueRequest> mreq = OBDal.getInstance().createCriteria(
            MaterialIssueRequest.class);
        mreq.add(Expression.eq("organization.id", strOrgid));
        mreq.addOrderBy("updated", false);
        mreq.setMaxResults(1);
        if (mreq.list() != null && mreq.list().size() > 0) {
          MaterialIssueRequest mrequest = mreq.list().get(0);
          info.addResult("inprequestManager", mrequest.getRequestManager());
          info.addResult("inpinventoryMgr", mrequest.getInventoryMgr());
          info.addResult("inpwarehouseKeeper", mrequest.getWarehouseKeeper());
          info.addResult("inpreceiver", mrequest.getReceiver());
          info.addResult("inpfinalApprover", mrequest.getFinalApprover());
        } else {
          info.addResult("inprequestManager", null);
          info.addResult("inpinventoryMgr", null);
          info.addResult("inpwarehouseKeeper", null);
          info.addResult("inpreceiver", null);
          info.addResult("inpfinalApprover", null);
        }

      } catch (Exception e) {
        log4j.debug("callout in material issue request header:" + e);
        e.printStackTrace();
      }
    }
    /*
     * if (inpLastFieldChanged.equals("inpmWarehouseId")) {
     * 
     * try { Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, strwarehouse); if
     * (warehouse.getEscmWarehouseType() != null) { if
     * (warehouse.getEscmWarehouseType().equals("RTW")) { info.addResult("inpescmIssuereason",
     * "ID"); } else { info.addResult("inpescmIssuereason", null); } }
     * 
     * } catch (Exception e) { log4j.debug("callout in material issue request header:" + e);
     * e.printStackTrace(); }
     * 
     * }
     */

  }
}
