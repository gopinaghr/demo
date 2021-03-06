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
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.MaterialIssueRequestLine;

/**
 * 
 * @author gopalakrishnan 21/03/2017
 * 
 */

public class CustodyTransferprocessHandler extends BaseActionHandler {
  /**
   * This servlet class responsible for add lines in Custody Transfer
   */
  private static Logger log = Logger.getLogger(CustodyTransferprocessHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject custodyLines = jsonparams.getJSONObject("custody_transfer");
      JSONArray selectedlines = custodyLines.getJSONArray("_selection");
      ShipmentInOut inout = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      ShipmentInOutLine inoutline = null;
      Connection conn = OBDal.getInstance().getConnection();
      long line = 10, custline = 10, mainLine = 10;
      final String inoutId = jsonRequest.getString("inpmInoutId");
      inout = OBDal.getInstance().get(ShipmentInOut.class, inoutId);

      // delete exisiting lines
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          log.debug("selectedRow:" + selectedRow);
          OBQuery<ShipmentInOutLine> chklineexistQry = OBDal.getInstance().createQuery(
              ShipmentInOutLine.class,
              "as e where e.shipmentReceipt.id='" + inoutId + "' " + "and e.product.id in ( '"
                  + selectedRow.getString("product") + "')");
          chklineexistQry.setMaxResult(1);
          log.debug("initial getWhereAndOrderBy:" + chklineexistQry.getWhereAndOrderBy());
          log.debug("initial size:" + chklineexistQry.list().size());
          if (chklineexistQry.list().size() > 0) {
            inoutline = chklineexistQry.list().get(0);
            inoutline.setUpdated(new Date());
            inoutline.setUpdatedBy(inout.getUpdatedBy());
            inoutline.setMovementQuantity(inoutline.getMovementQuantity().add(
                new BigDecimal(selectedRow.getString("quantity"))));
            OBDal.getInstance().save(inoutline);
          } else {

            if (selectedRow.getString("escmMaterialReqln") != null) {
              ps = conn
                  .prepareStatement(" select coalesce(max(line),0)+10 as lineno from m_inoutline where m_inout_id=?");
              ps.setString(1, inoutId);
              rs = ps.executeQuery();
              if (rs.next()) {
                line = rs.getLong("lineno");
                log.debug("line:" + line);
              }
              MaterialIssueRequestLine reqline = OBDal.getInstance().get(
                  MaterialIssueRequestLine.class, selectedRow.getString("escmMaterialReqln"));
              Product objProduct = OBDal.getInstance().get(Product.class,
                  selectedRow.getString("product"));
              inoutline = OBProvider.getInstance().get(ShipmentInOutLine.class);
              inoutline.setClient(inout.getClient());
              inoutline.setOrganization(inout.getOrganization());
              inoutline.setCreationDate(new java.util.Date());
              inoutline.setCreatedBy(inout.getCreatedBy());
              inoutline.setUpdated(new java.util.Date());
              inoutline.setUpdatedBy(inout.getUpdatedBy());
              inoutline.setActive(true);
              inoutline.setShipmentReceipt(inout);
              inoutline.setLineNo(line);
              inoutline.setProduct(objProduct);
              inoutline.setUOM(objProduct.getUOM());
              if (reqline != null)
                inoutline.setDescription(reqline.getDescription());
              inoutline.setMovementQuantity(new BigDecimal(selectedRow.getString("quantity")));

              // inoutline.setEscmCustodyItem(reqline.isCustodyItem());
              // inoutline.setEscmTransaction("A");
              OBQuery<Locator> locator = OBDal.getInstance().createQuery(
                  Locator.class,
                  " as e where e.warehouse.id='" + inout.getWarehouse().getId()
                      + "' and e.default='Y' ");
              locator.setMaxResult(1);
              if (locator.list().size() > 0) {
                inoutline.setStorageBin(locator.list().get(0));
                log.debug("getStorageBin:" + inoutline.getStorageBin());

              } else
                inoutline.setStorageBin(null);
              OBDal.getInstance().save(inoutline);
              OBDal.getInstance().flush();
            }

          }
          MaterialIssueRequestCustody custodydetial = OBDal.getInstance().get(
              MaterialIssueRequestCustody.class, selectedRow.getString("id"));

          ps = conn
              .prepareStatement(" select coalesce(max(line),0)+10 as line from escm_custody_transaction where m_inoutline_id  = ? ");
          ps.setString(1, inoutline.getId());
          rs = ps.executeQuery();
          if (rs.next()) {
            mainLine = rs.getLong("line");
            log.debug("line:" + line);
          }
          ps = conn
              .prepareStatement(" select coalesce(max(line2),0)+10 as lineno from escm_custody_transaction where escm_mrequest_custody_id  = ? ");
          ps.setString(1, custodydetial.getId());
          rs = ps.executeQuery();
          if (rs.next()) {
            custline = rs.getLong("lineno");
            log.debug("line:" + line);
          }
          Escm_custody_transaction custtransaction = OBProvider.getInstance().get(
              Escm_custody_transaction.class);
          ;
          custtransaction.setClient(inout.getClient());
          custtransaction.setOrganization(inout.getOrganization());
          custtransaction.setCreationDate(new java.util.Date());
          custtransaction.setCreatedBy(inout.getCreatedBy());
          custtransaction.setUpdated(new java.util.Date());
          custtransaction.setUpdatedBy(inout.getUpdatedBy());
          custtransaction.setActive(true);
          custtransaction.setGoodsShipmentLine(inoutline);
          custtransaction.setLine2(custline);
          custtransaction.setLineNo(mainLine);
          custtransaction.setDocumentNo(inout.getDocumentNo());
          custtransaction.setEscmMrequestCustody(custodydetial);
          custtransaction.setTransactiontype("TR");
          custtransaction.setTransactionDate(inout.getAccountingDate());
          custline++;
          mainLine++;
          OBDal.getInstance().save(custtransaction);
          OBDal.getInstance().flush();
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
      log.error("Exception in ReturnCustodyHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
