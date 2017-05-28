/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.scm.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;

public class POReceiptHeaderCallout extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    String strMWarehouseId = info.vars.getStringParameter("inpmWarehouseId");
    String inpLastFieldChanged = info.vars.getStringParameter("inpLastFieldChanged");
    String strOrgid = info.vars.getStringParameter("inpadOrgId");
    String strClientId = info.vars.getStringParameter("inpadClientId");
    String strFromBname = info.vars.getStringParameter("inpemEscmBname");
    String strporeference = info.vars.getStringParameter("inpporeference");
    String strFromBtype = info.vars.getStringParameter("inpemEscmBtype");
    String strToBtype = info.vars.getStringParameter("inpemEscmTobeneficiary");
    String strIssueReason = info.vars.getStringParameter("inpemEscmIssuereason");
    String strToBName = info.vars.getStringParameter("inpemEscmTobenefiName");
    String strReceivingtype = info.vars.getStringParameter("inpemEscmReceivingtype");
    String strIsCustodyTransfer = info.vars.getStringParameter("inpemEscmIscustodyTransfer");
    boolean updateWarehouse = true;
    FieldProvider[] td = null;
    String inpTabId = info.vars.getStringParameter("inpTabId");
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    String sqlquery = "";
    String query = "";
    log4j.debug("inpTabId " + inpTabId);
    log4j.debug("strMWarehouseId " + strMWarehouseId);
    log4j.debug("inpLastFieldChanged " + inpLastFieldChanged);
    log4j.debug("strIsCustodyTransfer: " + strIsCustodyTransfer);
    User currentuser = OBContext.getOBContext().getUser();
    /*
     * try {
     * 
     * if (inpLastFieldChanged.equals("inpadOrgId")) { if (inpTabId.equals("296")) { ComboTableData
     * comboTableData = new ComboTableData(info.vars, this, "18", "M_Warehouse_ID", "197",
     * strIsSOTrx.equals("Y") ? "C4053C0CD3DC420A9924F24FC1F860A0" :
     * "301AAE6E09C5402198813447D752EF59", Utility.getReferenceableOrg(info.vars,
     * info.vars.getStringParameter("inpadOrgId")), Utility.getContext(this, info.vars,
     * "#User_Client", info.getWindowId()), 0); Utility.fillSQLParameters(this, info.vars, null,
     * comboTableData, info.getWindowId(), ""); td = comboTableData.select(false); comboTableData =
     * null; } else if ((inpTabId.equals("72A6B3CA5BE848ACA976304375A5B7A6") || inpTabId
     * .equals("922927563BFC48098D17E4DC85DD504C"))) { ComboTableData comboTableData = new
     * ComboTableData(info.vars, this, "18", "M_Warehouse_ID", "197", strIsSOTrx.equals("Y") ?
     * "552CF3354797470F9535869BF731C775" : "552CF3354797470F9535869BF731C775",
     * Utility.getReferenceableOrg(info.vars, info.vars.getStringParameter("inpadOrgId")),
     * Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
     * Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), ""); td
     * = comboTableData.select(false); comboTableData = null; } if (td != null && td.length > 0) {
     * for (int i = 0; i < td.length; i++) { if (td[i].getField("id").equals(strMWarehouseId)) {
     * updateWarehouse = false; break; } } if (updateWarehouse) { info.addResult("inpmWarehouseId",
     * td[0].getField("id")); } } else { info.addResult("inpmWarehouseId", null); } } } catch
     * (Exception ex) { throw new ServletException(ex); }
     */

    if (strIsCustodyTransfer.equals("Y") && inpTabId.equals("CB9A2A4C6DB24FD19D542A78B07ED6C1")) {
      log4j.debug("Exists: " + strIsCustodyTransfer);

      if (inpLastFieldChanged.equals("inpadOrgId")) {
        try {

          sql = "select c_bpartner.c_bpartner_id as bpartnerid,c_bpartner_location.c_bpartner_location_id as locationid from c_bpartner "
              + "left join c_bpartner_location on c_bpartner_location.c_bpartner_id = c_bpartner.c_bpartner_id "
              + "where c_bpartner.em_escm_defaultpartner='Y' and c_bpartner.ad_client_id='"
              + strClientId + "'";
          st = conn.prepareStatement(sql);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpcBpartnerId", rs.getString("bpartnerid").toString());
            info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
            info.addResult("inpcDoctypeId", "FF8080812C2ABFC6012C2B3BDF4A004E");
          }

          sql = "select m_warehouse_id from m_warehouse where isactive='Y' and ad_client_id='"
              + strClientId + "' limit 1";
          st = conn.prepareStatement(sql);
          rs = st.executeQuery();
          if (rs.next()) {
            info.addResult("inpmWarehouseId", rs.getString("m_warehouse_id"));
          }

          Boolean ispreference = Preferences.existsPreference("ESCM_Inventory_Control", true, null,
              null, currentuser.getId(), null, null);
          log4j.debug("ispreference :" + ispreference);
          if (!ispreference) {
            info.addResult("inpemEscmBtype", "E");
            if (currentuser.getBusinessPartner() != null) {
              strFromBname = currentuser.getBusinessPartner().getId();
              info.addResult("inpemEscmBname", currentuser.getBusinessPartner().getId());
            }
            info.addResult("inpemEscmCtsender", currentuser.getId());
          }

        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      /*
       * if (inpLastFieldChanged.equals("inpemEscmBtype")) {
       * 
       * log4j.debug("currentuser :" + currentuser); strFromBtype = "E";
       * log4j.debug("getBusinessPartner :" + currentuser.getBusinessPartner()); if
       * (currentuser.getBusinessPartner() != null) { strFromBname =
       * currentuser.getBusinessPartner().getId(); log4j.debug("strFromBtype :" + strFromBtype);
       * info.addResult("inpemEscmBname", currentuser.getBusinessPartner().getId()); }
       * info.addResult("inpemEscmCtsender", currentuser.getId());
       * 
       * }
       */
      if (inpLastFieldChanged.equals("inpemEscmBname")) {
        if (strFromBtype.equals("E")) {
          BusinessPartner objBpartner = OBDal.getInstance()
              .get(BusinessPartner.class, strFromBname);
          // Task No.4812 info.addResult("inpemEscmFromemployee", objBpartner.getName());
          OBQuery<User> userlist = OBDal.getInstance().createQuery(User.class,
              " businessPartner.id='" + objBpartner.getId() + "'");
          if (userlist.list().size() > 0) {
            User user = userlist.list().get(0);

            info.addResult("inpemEscmCtsender", user.getId());
          }
        }

      } else if (inpLastFieldChanged.equals("inpemEscmTobenefiName")) {

        if (strToBtype.equals("E")) {
          BusinessPartner objBpartner = OBDal.getInstance().get(BusinessPartner.class, strToBName);
          // Task No.4812 info.addResult("inpemEscmToemployee", objBpartner.getName());
          OBQuery<User> userlist = OBDal.getInstance().createQuery(User.class,
              " businessPartner.id='" + objBpartner.getId() + "'");
          if (userlist.list().size() > 0) {
            User user = userlist.list().get(0);

            info.addResult("inpemEscmCtreclinemng", user.getId());
          }
        }
      }
    }

    if (inpLastFieldChanged.equals("inpadOrgId")
        || inpLastFieldChanged.equals("inpemEscmReceivingtype")
        && !inpTabId.equals("CB9A2A4C6DB24FD19D542A78B07ED6C1")) {
      if (strReceivingtype.equals("IR")) {
        try {
          sql = "select EM_Escm_Warehousereceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IR' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmWarehousereceiver", rs.getString("EM_Escm_Warehousereceiver"));

          } else {
            info.addResult("inpemEscmWarehousereceiver", null);
          }

          query = "select em_escm_warehousekeeper from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IR' order by updated desc limit 1";
          st = conn.prepareStatement(query);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmWarehousekeeper", rs.getString("em_escm_warehousekeeper"));

          } else {
            info.addResult("inpemEscmWarehousekeeper", null);
          }

          sqlquery = "select em_escm_inventorymgr from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IR' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInventorymgr", rs.getString("em_escm_inventorymgr"));

          } else {
            info.addResult("inpemEscmInventorymgr", null);
          }

        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else if (strReceivingtype.equals("DEL")) {
        try {
          sql = "select em_escm_delwhreceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'DEL' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmDelwhreceiver", rs.getString("em_escm_delwhreceiver"));

          } else {
            info.addResult("inpemEscmDelwhreceiver", null);
          }

          query = "select em_escm_delwhkeeper from m_inout where ad_org_id = ?  and em_escm_receivingtype = 'DEL' order by updated desc limit 1";
          st = conn.prepareStatement(query);
          st.setString(1, strOrgid);
          rs = st.executeQuery();
          if (rs.next()) {
            info.addResult("inpemEscmDelwhkeeper", rs.getString("em_escm_delwhkeeper"));

          } else {
            info.addResult("inpemEscmDelwhkeeper", null);
          }
          sqlquery = "select em_escm_delinvmgr from m_inout where ad_org_id = ?  and em_escm_receivingtype = 'DEL' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();
          if (rs.next()) {
            info.addResult("inpemEscmDelinvmgr", rs.getString("em_escm_delinvmgr"));

          } else {
            info.addResult("inpemEscmDelinvmgr", null);
          }

        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else if (strReceivingtype.equals("INS")) {
        try {
          sql = "select em_escm_inswhkeeper from m_inout where ad_org_id = ?  and em_escm_receivingtype = 'INS' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInswhkeeper", rs.getString("em_escm_inswhkeeper"));

          } else {
            info.addResult("inpemEscmInswhkeeper", null);
          }

          query = "select em_escm_inswhreceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INS'  order by updated desc limit 1";
          st = conn.prepareStatement(query);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInswhreceiver", rs.getString("em_escm_inswhreceiver"));

          } else {
            info.addResult("inpemEscmInswhreceiver", null);
          }
          sqlquery = "select em_escm_insinvmgr from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INS' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();
          if (rs.next()) {
            info.addResult("inpemEscmInsinvmgr", rs.getString("em_escm_insinvmgr"));

          } else {
            info.addResult("inpemEscmInsinvmgr", null);
          }

          sqlquery = "select em_escm_inventoryctrl from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INS' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();
          if (rs.next()) {
            info.addResult("inpemEscmInventoryctrl", rs.getString("em_escm_inventoryctrl"));
          } else {
            info.addResult("inpemEscmInventoryctrl", null);
          }
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else if (strReceivingtype.equals("INR")) {
        try {
          sql = "select c_bpartner.c_bpartner_id as bpartnerid,c_bpartner_location.c_bpartner_location_id as locationid from c_bpartner "
              + "left join c_bpartner_location on c_bpartner_location.c_bpartner_id = c_bpartner.c_bpartner_id "
              + "where c_bpartner.em_escm_defaultpartner='Y' and c_bpartner.ad_client_id='"
              + strClientId + "' limit 1";
          st = conn.prepareStatement(sql);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpcBpartnerId", rs.getString("bpartnerid"));
            info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
            info.addResult("inpcDoctypeId", "FF8080812C2ABFC6012C2B3BDF4A004E");
          }
          sql = "select EM_Escm_Warehousereceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INR' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmWarehousereceiver", rs.getString("EM_Escm_Warehousereceiver"));

          } else {
            info.addResult("inpemEscmWarehousereceiver", null);
          }
          sqlquery = "select em_escm_inventorymgr from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INR' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInventorymgr", rs.getString("em_escm_inventorymgr"));

          } else {
            info.addResult("inpemEscmInventorymgr", null);
          }
          sqlquery = "select EM_Escm_Appauthority from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INR' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmAppauthority", rs.getString("EM_Escm_Appauthority"));

          } else {
            info.addResult("inpemEscmAppauthority", null);
          }
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else if (strReceivingtype.equals("IRT")) {
        try {
          sql = "select c_bpartner.c_bpartner_id as bpartnerid,c_bpartner_location.c_bpartner_location_id as locationid from c_bpartner "
              + "left join c_bpartner_location on c_bpartner_location.c_bpartner_id = c_bpartner.c_bpartner_id "
              + "where c_bpartner.em_escm_defaultpartner='Y' and c_bpartner.ad_client_id='"
              + strClientId + "' limit 1";
          st = conn.prepareStatement(sql);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpcBpartnerId", rs.getString("bpartnerid"));
            info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
            info.addResult("inpcDoctypeId", "FF8080812C2ABFC6012C2B3BDF4A004E");
          }
          sql = "select EM_Escm_Warehousereceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IRT' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmWarehousereceiver", rs.getString("EM_Escm_Warehousereceiver"));

          } else {
            info.addResult("inpemEscmWarehousereceiver", null);
          }
          sqlquery = "select em_escm_inventorymgr from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IRT' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInventorymgr", rs.getString("em_escm_inventorymgr"));

          } else {
            info.addResult("inpemEscmInventorymgr", null);
          }
          sqlquery = "select EM_Escm_Appauthority from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IRT' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmAppauthority", rs.getString("EM_Escm_Appauthority"));

          } else {
            info.addResult("inpemEscmAppauthority", null);
          }
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else if (strReceivingtype.equals("LD")) {
        try {
          sql = "select c_bpartner.c_bpartner_id as bpartnerid,c_bpartner_location.c_bpartner_location_id as locationid from c_bpartner "
              + "left join c_bpartner_location on c_bpartner_location.c_bpartner_id = c_bpartner.c_bpartner_id "
              + "where c_bpartner.em_escm_defaultpartner='Y' and c_bpartner.ad_client_id='"
              + strClientId + "' limit 1";
          st = conn.prepareStatement(sql);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpcBpartnerId", rs.getString("bpartnerid"));
            info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
            info.addResult("inpcDoctypeId", "FF8080812C2ABFC6012C2B3BDF4A004E");
          }

        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    if (inpLastFieldChanged.equals("inpporeference") && inpTabId.equals("296")) {
      try {
        sql = " select c_bpartner_id as bpartnerid,c_bpartner_location_id as locationid,c_doctype_id, eut_convert_to_hijri(to_char(dateacct,'yyyy-MM-dd'))  as dateacct from m_inout "
            + " where em_escm_receivingtype in ('SR','IR','INS','DEL','RET') and poreference= ? and ad_client_id= ? and ad_org_id= ?"
            + " order by created desc limit 1";
        st = conn.prepareStatement(sql);
        st.setString(1, strporeference);
        st.setString(2, strClientId);
        st.setString(3, strOrgid);
        rs = st.executeQuery();

        if (rs.next()) {
          info.addResult("inpcBpartnerId", rs.getString("bpartnerid"));
          info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
          info.addResult("inpcDoctypeId", rs.getString("c_doctype_id"));
          info.addResult("inpdateacct", rs.getString("dateacct"));
        }
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
    if (inpLastFieldChanged.equals("inpemEscmIssuereason")
        && (inpTabId.equals("922927563BFC48098D17E4DC85DD504C")
            || inpTabId.equals("72A6B3CA5BE848ACA976304375A5B7A6") || inpTabId
              .equals("CB9A2A4C6DB24FD19D542A78B07ED6C1"))) {
      if (strIssueReason.equals("MA") || strIssueReason.equals("SA") || strIssueReason.equals("OB")) {
        info.addResult("inpemEscmBtype", "");
        info.addResult("inpemEscmBname", "");
      }
    }
  }
}
