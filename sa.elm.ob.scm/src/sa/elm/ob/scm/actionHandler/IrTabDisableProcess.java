package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import sa.elm.ob.scm.MaterialIssueRequest;

public class IrTabDisableProcess extends BaseActionHandler {
  Logger log4j = Logger.getLogger(IrTabDisableProcess.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      // get the data as json
      final JSONObject jsonData = new JSONObject(data);
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");
      log4j.debug("recordid:" + recordId);
      HttpServletRequest request = RequestContext.get().getRequest();
      VariablesSecureApp vars = new VariablesSecureApp(request);

      JSONObject json = new JSONObject();
      OBContext.setAdminMode(true);
      int enablePrint = 0;
      Boolean ispreference = false;
      if (tabId.equals("CE947EDC9B174248883292F17F03BB32")) {// Material Issue Req
        if (!recordId.equals("")) {
          MaterialIssueRequest missreq = OBDal.getInstance().get(MaterialIssueRequest.class,
              recordId);
          try {
            String preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
                vars.getClient(), vars.getOrg(), vars.getUser(), vars.getRole(),
                "D8BA0A87790B4B67A86A8DF714525736");
            if (preferenceValue.equals("Y"))
              ispreference = true;
          } catch (PropertyException e) {
            ispreference = false;
          }
          if (ispreference) {
            if (missreq != null
                && (missreq.getWarehouse() != null && !missreq.getWarehouse().equals(""))) {
              if (missreq.getAlertStatus().equals("ESCM_TR")
                  || missreq.getAlertStatus().equals("ESCM_IP"))
                enablePrint = 1;
            }
          }
          json.put("receivingType", "");
        }
      }
      // Return tran(72A6B3CA5BE848ACA976304375A5B7A6) Custody
      // transfer(CB9A2A4C6DB24FD19D542A78B07ED6C1)
      else if (tabId.equals("72A6B3CA5BE848ACA976304375A5B7A6")
          || tabId.equals("CB9A2A4C6DB24FD19D542A78B07ED6C1")) {
        if (!recordId.equals("")) {
          ShipmentInOut poreceipt = OBDal.getInstance().get(ShipmentInOut.class, recordId);
          if (poreceipt != null) {
            if (poreceipt.getDocumentStatus().equals("CO")
                || poreceipt.getDocumentStatus().equals("DR")) {
              OBQuery<ShipmentInOutLine> returntranline = OBDal.getInstance().createQuery(
                  ShipmentInOutLine.class, "shipmentReceipt.id='" + recordId + "'");
              if (returntranline.list() != null && returntranline.list().size() > 0) {
                enablePrint = 1;
              }
              json.put("receivingType", "");
            }
          }
        }
      } else if (tabId.equals("922927563BFC48098D17E4DC85DD504C")) {// issue return tran
        if (!recordId.equals("")) {
          ShipmentInOut poreceipt = OBDal.getInstance().get(ShipmentInOut.class, recordId);
          if (poreceipt != null) {
            if (poreceipt.getDocumentStatus().equals("CO"))
              enablePrint = 1;
          }
          json.put("receivingType", "");
        }
      } else if (tabId.equals("9A4225DDEFFD40C8BFA386059CA93DEC")) {// Inventory counting line
        if (!recordId.equals("")) {
          InventoryCount Invcount = OBDal.getInstance().get(InventoryCount.class, recordId);
          if (Invcount != null) {
            if (Invcount.getEscmStatus().equals("CO"))
              enablePrint = 1;
          }
          json.put("receivingType", "");
        }
      }
      // Return Transaction line (0C0819F5D78A401A916BDD8ADB30E4EF) and Issue Return Transaction
      // line (5B16AE5DFDEF47BB9518CDD325F31DFF)
      else if (tabId.equals("0C0819F5D78A401A916BDD8ADB30E4EF")
          || tabId.equals("5B16AE5DFDEF47BB9518CDD325F31DFF")) {
        if (!recordId.equals("")) {
          ShipmentInOut shipment = OBDal.getInstance().get(ShipmentInOut.class, recordId);
          if (shipment != null) {
            if (shipment.getDocumentStatus().equals("CO"))
              enablePrint = 1;
          }
          if (tabId.equals("0C0819F5D78A401A916BDD8ADB30E4EF")) {
            json.put("receivingType", "INR");
          } else if (tabId.equals("5B16AE5DFDEF47BB9518CDD325F31DFF")) {
            json.put("receivingType", "IRT");
          }

        }
      }
      // custody transaction tab under Return Transaction (DD6AB8A564D5482795B0976F6A68FBC5) and
      // custody transaction tab under Return Transaction (D4E9D5A2F73E4A15AEA52FD9A5A57902)
      else if (tabId.equals("DD6AB8A564D5482795B0976F6A68FBC5")
          || tabId.equals("D4E9D5A2F73E4A15AEA52FD9A5A57902")) {
        if (!recordId.equals("")) {
          ShipmentInOutLine shipment = OBDal.getInstance().get(ShipmentInOutLine.class, recordId);
          if (shipment != null) {
            if (shipment.getShipmentReceipt().getDocumentStatus().equals("CO"))
              enablePrint = 1;
          }
          if (tabId.equals("DD6AB8A564D5482795B0976F6A68FBC5")) {
            json.put("receivingType", "INR");
          }
          if (tabId.equals("D4E9D5A2F73E4A15AEA52FD9A5A57902")) {
            json.put("receivingType", "IRT");
          }
        }
      } else {
        if (!recordId.equals("")) {
          ShipmentInOut poreceipt = OBDal.getInstance().get(ShipmentInOut.class, recordId);
          if (poreceipt != null) {
            if (!poreceipt.getDocumentStatus().equals("DR")
                && !poreceipt.getEscmReceivingtype().equals("IRT")
                && !poreceipt.getEscmReceivingtype().equals("RET")
                && !poreceipt.getEscmReceivingtype().equals("INR"))
              enablePrint = 1;
            json.put("receivingType", poreceipt.getEscmReceivingtype());

          } else {
            if (!recordId.equals("")) {
              ShipmentInOutLine inoutline = OBDal.getInstance().get(ShipmentInOutLine.class,
                  recordId);
              if (inoutline != null) {
                ShipmentInOut inout = inoutline.getShipmentReceipt();
                if (inout != null) {
                  json.put("receivingType", inout.getEscmReceivingtype());
                }
              }
            }
          }
        }
      }
      if (enablePrint == 1)
        json.put("IsDraft", 1);
      else
        json.put("IsDraft", 0);
      log4j.debug("json:" + json);
      OBContext.restorePreviousMode();
      return json;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}