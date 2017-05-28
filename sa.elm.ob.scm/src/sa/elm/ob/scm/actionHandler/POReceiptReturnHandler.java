package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.EscmInitialReceipt;

/*
 * Inserting records in retuen to vendor tab and updating quantity in intial receipt.
 */
public class POReceiptReturnHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POReceiptReturnHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject lines = jsonparams.getJSONObject("Return");
      JSONArray selectedlines = lines.getJSONArray("_selection");
      EscmInitialReceipt IR = null;
      // ShipmentInOut inout = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      String supplierBatch = "";
      Date currentDate = new Date();
      Connection conn = OBDal.getInstance().getConnection();
      long line = 0;
      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        log.debug("selectedRow:" + selectedRow);
        String initialreceipt = selectedRow.getString("escmInitialreceipt");
        log.debug("initialreceipt:" + initialreceipt);
        final String currentHdId = jsonRequest.getString("inpmInoutId");
        // inout = OBDal.getInstance()
        // .get(ShipmentInOut.class, selectedRow.getString("goodsShipment"));
        ps = conn
            .prepareStatement(" select coalesce(max(line),0)+10 as lineno from escm_initialreceipt where m_inout_id=?");
        ps.setString(1, currentHdId);
        rs = ps.executeQuery();
        if (rs.next()) {
          line = rs.getLong("lineno");
        }
        String status = selectedRow.getString("status");
        BigDecimal quantity = new BigDecimal(selectedRow.getString("newquantity"));
        BigDecimal availablequantity = new BigDecimal(selectedRow.getString("reservedQuantity"));

        // String returnDate = selectedRow.getString("returnDate");
        if (selectedRow.getString("supplierBatch").equals("null")) {
          supplierBatch = "";
        } else {
          supplierBatch = selectedRow.getString("supplierBatch");
        }
        EscmInitialReceipt initial = OBDal.getInstance().get(EscmInitialReceipt.class,
            initialreceipt);
        ShipmentInOut sinout = OBDal.getInstance().get(ShipmentInOut.class, currentHdId);

        // validating Quantity should not be <= 0
        if (quantity.compareTo(BigDecimal.ZERO) < 0 || quantity.compareTo(BigDecimal.ZERO) == 0) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", OBMessageUtils.messageBD("Escm_IR_Quantity"));
          json.put("message", errorMessage);
          return json;
        }
        if (status.equals("I")) {
          OBQuery<EscmInitialReceipt> chklineexistQry = OBDal.getInstance().createQuery(
              EscmInitialReceipt.class,
              "as e where e.goodsShipment.id='" + currentHdId
                  + "' and e.alertStatus = 'I' and e.sourceRef.id='" + initial.getId() + "'");
          if (chklineexistQry.list().size() > 0) {
            IR = chklineexistQry.list().get(0);
            if (new BigDecimal(selectedRow.getString("newquantity")).compareTo(BigDecimal.ZERO) == 0) {
              OBDal.getInstance().remove(IR);
            } else {
              IR.setQuantity(quantity);
              IR.setNotes(selectedRow.getString("notes"));
              OBDal.getInstance().save(IR);
            }
          } else {
            if (new BigDecimal(selectedRow.getString("newquantity")).compareTo(BigDecimal.ZERO) > 0) {
              // update quantity in inspection
              /*
               * OBQuery<ESCMInspection> inspection = OBDal.getInstance().createQuery(
               * ESCMInspection.class, "escmInitialreceipt.id='" + initial.getId() +
               * "' and status = 'A' "); ESCMInspection insp = inspection.list().get(0);
               * insp.setQuantity(insp.getQuantity().subtract(quantity));
               * OBDal.getInstance().save(insp); OBDal.getInstance().flush();
               */
              // insertion in return to vendor line
              IR = OBProvider.getInstance().get(EscmInitialReceipt.class);
              IR.setOrganization(sinout.getOrganization());
              IR.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              IR.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              IR.setLineNo(line);
              IR.setQuantity(quantity);
              IR.setProduct(OBDal.getInstance()
                  .get(Product.class, selectedRow.getString("product")));
              IR.setImage(IR.getProduct().getImage());
              IR.setDescription(selectedRow.getString("description"));
              IR.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
              IR.setSupplierbatch(supplierBatch);
              // IR.setLineNumber(selectedRow.getLong("lineNo"));
              IR.setNotes(selectedRow.getString("notes"));
              IR.setGoodsShipment(sinout);
              IR.setSourceRef(initial);
              // returnVendor.setEscmInspection(null);
              // returnVendor.setGoodsShipmentLine(null);
              IR.setAlertStatus(status);
              OBDal.getInstance().save(IR);
            }
          }
          OBDal.getInstance().flush();
        } else if (status.equals("A")) {
          OBQuery<EscmInitialReceipt> chklineexistQry = OBDal.getInstance().createQuery(
              EscmInitialReceipt.class,
              "as e where e.goodsShipment.id='" + currentHdId
                  + "' and e.alertStatus = 'A' and e.sourceRef.id='" + initial.getId() + "'");
          if (chklineexistQry.list().size() > 0) {
            IR = chklineexistQry.list().get(0);
            if (new BigDecimal(selectedRow.getString("newquantity")).compareTo(BigDecimal.ZERO) == 0) {
              OBDal.getInstance().remove(IR);
            } else {
              IR.setQuantity(quantity);
              IR.setNotes(selectedRow.getString("notes"));
              OBDal.getInstance().save(IR);
            }
          } else {
            if (new BigDecimal(selectedRow.getString("newquantity")).compareTo(BigDecimal.ZERO) > 0) {
              // insertion in return to vendor line
              IR = OBProvider.getInstance().get(EscmInitialReceipt.class);
              IR.setOrganization(sinout.getOrganization());
              IR.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              IR.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              IR.setLineNo(line);
              IR.setQuantity(quantity);
              IR.setProduct(OBDal.getInstance()
                  .get(Product.class, selectedRow.getString("product")));
              IR.setImage(IR.getProduct().getImage());
              IR.setDescription(selectedRow.getString("description"));
              IR.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
              IR.setSupplierbatch(supplierBatch);
              // IR.setLineNumber(selectedRow.getLong("lineNo"));
              IR.setNotes(selectedRow.getString("notes"));
              IR.setGoodsShipment(sinout);
              IR.setSourceRef(initial);
              /*
               * OBQuery<ESCMInspection> inspection = OBDal.getInstance().createQuery(
               * ESCMInspection.class, "escmInitialreceipt.id='" + initial.getId() +
               * "' and status = 'A' "); if (inspection != null && inspection.list().size() > 0) {
               * returnVendor.setEscmInspection(inspection.list().get(0)); } else {
               * returnVendor.setEscmInspection(null); }
               */
              // returnVendor.setEscmInspection(null);
              // returnVendor.setGoodsShipmentLine(null);
              IR.setAlertStatus(status);
              OBDal.getInstance().save(IR);
            }
          }
          OBDal.getInstance().flush();
        } else if (status.equals("R")) {
          OBQuery<EscmInitialReceipt> chklineexistQry = OBDal.getInstance().createQuery(
              EscmInitialReceipt.class,
              "as e where e.goodsShipment.id='" + currentHdId
                  + "' and e.alertStatus = 'R' and e.sourceRef.id='" + initial.getId() + "'");
          if (chklineexistQry.list().size() > 0) {
            IR = chklineexistQry.list().get(0);
            if (new BigDecimal(selectedRow.getString("newquantity")).compareTo(BigDecimal.ZERO) == 0) {
              OBDal.getInstance().remove(IR);
            } else {
              IR.setQuantity(quantity);
              IR.setNotes(selectedRow.getString("notes"));
              OBDal.getInstance().save(IR);
            }
          } else {
            if (new BigDecimal(selectedRow.getString("newquantity")).compareTo(BigDecimal.ZERO) > 0) {
              // insertion in return to vendor line
              IR = OBProvider.getInstance().get(EscmInitialReceipt.class);
              IR.setOrganization(sinout.getOrganization());
              IR.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              IR.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              IR.setLineNo(line);
              IR.setQuantity(quantity);
              IR.setProduct(OBDal.getInstance()
                  .get(Product.class, selectedRow.getString("product")));
              IR.setImage(IR.getProduct().getImage());
              IR.setDescription(selectedRow.getString("description"));
              IR.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
              IR.setSupplierbatch(supplierBatch);
              // IR.setLineNumber(selectedRow.getLong("lineNo"));
              IR.setNotes(selectedRow.getString("notes"));
              IR.setGoodsShipment(sinout);
              IR.setSourceRef(initial);
              /*
               * OBQuery<ESCMInspection> inspection = OBDal.getInstance().createQuery(
               * ESCMInspection.class, "escmInitialreceipt.id='" + initial.getId() +
               * "' and status = 'R' "); if (inspection != null && inspection.list().size() > 0) {
               * returnVendor.setEscmInspection(inspection.list().get(0)); } else {
               * returnVendor.setEscmInspection(null); }
               */
              // returnVendor.setEscmInspection(null);
              // returnVendor.setGoodsShipmentLine(null);
              IR.setAlertStatus(status);
              OBDal.getInstance().save(IR);
            }
          }
          OBDal.getInstance().flush();
        } else if (status.equals("D")) {
          OBQuery<EscmInitialReceipt> chklineexistQry = OBDal.getInstance().createQuery(
              EscmInitialReceipt.class,
              "as e where e.goodsShipment.id='" + currentHdId
                  + "' and e.alertStatus = 'D' and e.sourceRef.id='" + initial.getId() + "'");
          if (chklineexistQry.list().size() > 0) {
            IR = chklineexistQry.list().get(0);
            if (new BigDecimal(selectedRow.getString("newquantity")).compareTo(BigDecimal.ZERO) == 0) {
              OBDal.getInstance().remove(IR);
            } else {
              IR.setQuantity(quantity);
              IR.setNotes(selectedRow.getString("notes"));
              OBDal.getInstance().save(IR);
            }
          } else {
            if (new BigDecimal(selectedRow.getString("newquantity")).compareTo(BigDecimal.ZERO) > 0) {
              // insertion in return to vendor line
              IR = OBProvider.getInstance().get(EscmInitialReceipt.class);
              IR.setOrganization(sinout.getOrganization());
              IR.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              IR.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              IR.setLineNo(line);
              IR.setQuantity(quantity);
              IR.setProduct(OBDal.getInstance()
                  .get(Product.class, selectedRow.getString("product")));
              IR.setImage(IR.getProduct().getImage());
              IR.setDescription(selectedRow.getString("description"));
              IR.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
              IR.setSupplierbatch(supplierBatch);
              // IR.setLineNumber(selectedRow.getLong("lineNo"));
              IR.setNotes(selectedRow.getString("notes"));
              IR.setGoodsShipment(sinout);
              IR.setSourceRef(initial);
              /*
               * returnVendor.setEscmInspection(null); OBQuery<ShipmentInOutLine> inoutline =
               * OBDal.getInstance().createQuery( ShipmentInOutLine.class, "escmInitialreceipt.id='"
               * + initial.getId() + "'"); if (inoutline != null && inoutline.list().size() > 0) {
               * returnVendor.setGoodsShipmentLine(inoutline.list().get(0)); } else {
               * returnVendor.setGoodsShipmentLine(null); }
               */
              IR.setAlertStatus(status);
              OBDal.getInstance().save(IR);
            }
          }
          OBDal.getInstance().flush();
        }
      }
      JSONObject successMessage = new JSONObject();
      successMessage.put("severity", "success");
      successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
      json.put("message", successMessage);
      return json;

    } catch (Exception e) {
      log.error("Exception in POReceiptReturnHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
