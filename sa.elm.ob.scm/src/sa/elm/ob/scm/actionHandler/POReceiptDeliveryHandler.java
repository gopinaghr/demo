package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import sa.elm.ob.scm.EscmInitialReceipt;

public class POReceiptDeliveryHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POReceiptDeliveryHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject encumlines = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");
      ShipmentInOutLine inoutline = null;
      ShipmentInOut inout = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      Connection conn = OBDal.getInstance().getConnection();
      long line = 10;
      final String inoutId = jsonRequest.getString("inpmInoutId");
      inout = OBDal.getInstance().get(ShipmentInOut.class, inoutId);

      // delete exisiting lines
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          log.debug("selectedRow:" + selectedRow);
          String initialid = selectedRow.getString("id");
          inout = OBDal.getInstance().get(ShipmentInOut.class,
              selectedRow.getString("goodsShipment"));
          String custodyitem = selectedRow.getString("custodyItem");

          EscmInitialReceipt initold = OBDal.getInstance().get(EscmInitialReceipt.class, initialid);
          if (new BigDecimal(selectedRow.getString("quantity")).compareTo((new BigDecimal(
              selectedRow.getString("acceptedQuantity"))).subtract(new BigDecimal(selectedRow
              .getString("deliveredqty")))) > 0) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("ESCM_POFinalRec_GrtActQty"));
            json.put("message", errorMessage);
            return json;
          }
          if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) <= 0) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
            json.put("message", errorMessage);
            return json;
          }
          OBQuery<EscmInitialReceipt> chklineexistQry = OBDal.getInstance().createQuery(
              EscmInitialReceipt.class,
              "as e where e.goodsShipment.id='" + inoutId + "' and e.sourceRef.id='" + initialid
                  + "'");
          chklineexistQry.setMaxResult(1);
          log.debug("initial size:" + chklineexistQry.list().size());
          if (chklineexistQry.list().size() > 0) {
            EscmInitialReceipt upinitial = chklineexistQry.list().get(0);
            if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) == 0) {
              OBDal.getInstance().remove(upinitial);
            } else {

              upinitial.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
              upinitial.setDescription(selectedRow.getString("description"));
              upinitial.setFailurereason(null);
              upinitial.setNotes(selectedRow.getString("notes"));
              if (custodyitem.equals("false")) {
                upinitial.setCustodyItem(false);
              } else
                upinitial.setCustodyItem(true);
              OBDal.getInstance().save(upinitial);
            }
          } else {
            if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) > 0) {
              EscmInitialReceipt initialnew = (EscmInitialReceipt) DalUtil.copy(initold, false);
              ps = conn
                  .prepareStatement(" select coalesce(max(line),0)+10 as lineno from escm_initialreceipt where m_inout_id=?");
              ps.setString(1, inoutId);
              rs = ps.executeQuery();
              if (rs.next()) {
                line = rs.getLong("lineno");
                log.debug("line:" + line);
              }
              initialnew.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
              initialnew.setSourceRef(initold);
              initialnew.setGoodsShipment(inout);
              initialnew.setLineNo(line);
              initialnew.setNotes(selectedRow.getString("notes"));
              initialnew.setDescription(selectedRow.getString("description"));
              if (custodyitem.equals("false")) {
                initialnew.setCustodyItem(false);
              } else
                initialnew.setCustodyItem(true);
              OBDal.getInstance().save(initialnew);
              OBDal.getInstance().flush();
              log.debug("initialnew:" + initialnew.getId());
            }
          }
        }
        OBDal.getInstance().flush();

        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }

    } catch (Exception e) {
      log.error("Exception in POReceiptDeliveryHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
