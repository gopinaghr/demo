package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.EscmInitialReceipt;

public class POInspectionHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POInspectionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      System.out.println("jsonparams:" + jsonparams.toString());
      JSONObject inspectLines = jsonparams.getJSONObject("Inspection");
      final String inoutId = jsonRequest.getString("inpmInoutId");
      JSONArray selectedlines = inspectLines.getJSONArray("_selection");
      long lineno = 10;

      log.debug("inoutId:" + inoutId);
      // shipment
      ShipmentInOut objInout = OBDal.getInstance().get(ShipmentInOut.class, inoutId);
      // get recent lineno
      OBQuery<EscmInitialReceipt> linesQry = OBDal.getInstance().createQuery(
          EscmInitialReceipt.class,
          "as e where e.goodsShipment.id='" + inoutId + "' order by e.lineNo desc");
      linesQry.setMaxResult(1);
      if (linesQry.list().size() > 0) {
        EscmInitialReceipt objExistLine = linesQry.list().get(0);
        lineno = objExistLine.getLineNo() + 10;
      }
      log.debug("lineno:" + lineno);
      if (selectedlines.length() > 0) {
        for (int i = 0; i < selectedlines.length(); i++) {
          JSONObject selectedRow = selectedlines.getJSONObject(i);
          log.debug("selectedRow:" + selectedRow);
          String escmInitialreceipt = selectedRow.getString("escmInitialreceipt");
          String strStatus = selectedRow.getString("alertStatus"), description = selectedRow
              .getString("description");
          // int qty = selectedRow.getInt("quantity");
          log.debug("escmInitialreceipt:" + escmInitialreceipt);
          log.debug("alertStatus:" + strStatus);
          // get Initial receipt line
          EscmInitialReceipt objIRLine = OBDal.getInstance().get(EscmInitialReceipt.class,
              escmInitialreceipt);
          if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) < 0) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
            json.put("message", errorMessage);
            return json;
          }
          // check already present in the lines
          // exist update quantity
          if (strStatus.equals("A") && escmInitialreceipt.length() == 32) {
            OBQuery<EscmInitialReceipt> existLineQry = OBDal.getInstance().createQuery(
                EscmInitialReceipt.class,
                "as e where e.sourceRef.id='" + escmInitialreceipt + "' and e.goodsShipment.id='"
                    + inoutId + "' and e.alertStatus='A'");
            existLineQry.setMaxResult(1);
            if (existLineQry.list().size() > 0) {
              EscmInitialReceipt objInitialReceipt = existLineQry.list().get(0);
              if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) == 0) {
                OBDal.getInstance().remove(objInitialReceipt);
              } else {
                objInitialReceipt.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
                objInitialReceipt.setDescription(description);
                objInitialReceipt.setAlertStatus("A");
                objInitialReceipt.setNotes(selectedRow.getString("notes"));
                objInitialReceipt.setFailurereason(null);
                if (selectedRow.getString("qualityCode") != null
                    && !selectedRow.getString("qualityCode").equals(""))
                  objInitialReceipt.setQualityCode(selectedRow.getString("qualityCode"));
                OBDal.getInstance().save(objInitialReceipt);
              }
            } else if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) > 0) {
              log.debug("qualityCode:" + selectedRow.getString("qualityCode"));
              log.debug("qualityCode:" + !selectedRow.getString("qualityCode").equals(""));
              EscmInitialReceipt newObject = OBProvider.getInstance().get(EscmInitialReceipt.class);
              newObject.setGoodsShipment(objInout);
              newObject.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
              newObject.setOrganization(objInout.getOrganization());
              newObject.setClient(objInout.getClient());
              newObject.setAlertStatus("A");
              newObject.setUnitprice(BigDecimal.ZERO);
              newObject.setProduct(objIRLine.getProduct());
              newObject.setImage(newObject.getProduct().getImage());
              newObject.setSourceRef(objIRLine);
              newObject.setDescription(description);
              newObject.setLineNo(lineno);
              newObject.setNotes(selectedRow.getString("notes"));
              if (selectedRow.getString("qualityCode") != null
                  && !selectedRow.getString("qualityCode").equals(""))
                newObject.setQualityCode(selectedRow.getString("qualityCode"));
              newObject.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
              OBDal.getInstance().save(newObject);
              lineno = lineno + 10;
            }
          }
          // check condition for reject Record
          if (strStatus.equals("R") && escmInitialreceipt.length() == 32) {
            OBQuery<EscmInitialReceipt> existLineQry = OBDal.getInstance().createQuery(
                EscmInitialReceipt.class,
                "as e where e.sourceRef.id='" + escmInitialreceipt + "' and e.goodsShipment.id='"
                    + inoutId + "' and e.alertStatus='R'");
            existLineQry.setMaxResult(1);
            if (existLineQry.list().size() > 0) {
              EscmInitialReceipt objInitialReceipt = existLineQry.list().get(0);

              if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) == 0) {
                OBDal.getInstance().remove(objInitialReceipt);
              } else {
                objInitialReceipt.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
                objInitialReceipt.setDescription(description);
                objInitialReceipt.setAlertStatus("R");
                objInitialReceipt.setNotes(selectedRow.getString("notes"));
                objInitialReceipt.setFailurereason(null);
                if (selectedRow.getString("qualityCode") != null
                    && !selectedRow.getString("qualityCode").equals(""))
                  objInitialReceipt.setQualityCode(selectedRow.getString("qualityCode"));
                OBDal.getInstance().save(objInitialReceipt);
              }
            } else if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) > 0) {
              EscmInitialReceipt newObject = OBProvider.getInstance().get(EscmInitialReceipt.class);
              newObject.setGoodsShipment(objInout);
              newObject.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
              newObject.setOrganization(objInout.getOrganization());
              newObject.setClient(objInout.getClient());
              newObject.setAlertStatus("R");
              newObject.setUnitprice(BigDecimal.ZERO);
              newObject.setProduct(objIRLine.getProduct());
              newObject.setImage(newObject.getProduct().getImage());
              newObject.setSourceRef(objIRLine);
              newObject.setLineNo(lineno);
              newObject.setDescription(description);
              newObject.setNotes(selectedRow.getString("notes"));
              newObject.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
              if (selectedRow.getString("qualityCode") != null
                  && !selectedRow.getString("qualityCode").equals(""))
                newObject.setQualityCode(selectedRow.getString("qualityCode"));
              OBDal.getInstance().save(newObject);
              lineno = lineno + 10;

            }
          }
          OBDal.getInstance().flush();
        }

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
      log.error("Exception in POInspectionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
