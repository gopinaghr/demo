package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.plm.Product;

public class EscmRequisitionItemCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmRequisitionItemCallout.class);
  /**
   * Callout to update the Description and UOM.
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strMProductID = vars.getStringParameter("inpmProductId");
    String strQty = vars.getNumericParameter("inpqty");
    String strPriceActual = vars.getNumericParameter("inppriceactual");
    String receivingType = vars.getStringParameter("inpemEscmReceivingtype");
    String inpcustodyItem = vars.getStringParameter("inpcustodyItem");
    BigDecimal lineNetAmount = BigDecimal.ZERO;
    try {
      if (inpLastFieldChanged.equals("inpmProductId")) {
        if (strMProductID != null) {
          Product product = OBDal.getInstance().get(Product.class, strMProductID);
          info.addResult("inpcUomId", product.getUOM().getId());
          info.addResult("inpdescription", product.getName());
          if (product.getImage() != null)
            info.addResult("inpadImageId", product.getImage().getId());
          else
            info.addResult("inpadImageId", "");
          if (product.getEscmStockType().getSearchKey().equals("CUS")) {
            info.addResult("inpcustodyItem", true);
          }
          if (receivingType.equals("SR"))
            info.addResult("inpunitprice", BigDecimal.ONE);
        } else {
          info.addResult("inpcUomId", "");
          info.addResult("inpdescription", "");
          info.addResult("inpadImageId", "");
        }
      }
      if (inpLastFieldChanged.equals("inpqty")) {
        lineNetAmount = new BigDecimal(strPriceActual).multiply(new BigDecimal(strQty));
        info.addResult("inplinenetamt", lineNetAmount);
      }
      if (inpLastFieldChanged.equals("inppriceactual")) {
        lineNetAmount = new BigDecimal(strPriceActual).multiply(new BigDecimal(strQty));
        info.addResult("inplinenetamt", lineNetAmount);
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Exception in Requisition item callout:" + e);
    }

  }
}
