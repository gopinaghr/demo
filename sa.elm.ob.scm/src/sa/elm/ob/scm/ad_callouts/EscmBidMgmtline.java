package sa.elm.ob.scm.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.plm.Product;

/**
 * 
 * 
 */
public class EscmBidMgmtline extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmBidMgmtline.class);
  /**
   * Callout to update the line Details in
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    Connection conn = OBDal.getInstance().getConnection();
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strMProductID = vars.getStringParameter("inpmProductId"), strOrgId = vars
        .getStringParameter("inpadOrgId");
    Product objProduct = OBDal.getInstance().get(Product.class, strMProductID);

    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      log.debug("LastChanged:" + inpLastFieldChanged);
      if (inpLastFieldChanged.equals("inpmProductId")) {
        if (strMProductID != null) {
          Product product = OBDal.getInstance().get(Product.class, strMProductID);
          info.addResult("inpcUomId", product.getUOM().getId());
          info.addResult("inpdescription", product.getName());

        } else {
          info.addResult("inpcUomId", "");
          info.addResult("inpdescription", "");
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Exception in EscmBidMgmtline item callout:" + e);
    }

  }
}
