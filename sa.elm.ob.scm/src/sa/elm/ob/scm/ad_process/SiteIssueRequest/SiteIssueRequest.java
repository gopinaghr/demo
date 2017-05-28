package sa.elm.ob.scm.ad_process.SiteIssueRequest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.plm.Product;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 10/04/2017
 */

public class SiteIssueRequest extends DalBaseProcess {

  /**
   * This servlet class was responsible for Site Issue Request Submission Process with Approval
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(SiteIssueRequest.class);
  private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String appstatus = "";
    boolean errorFlag = false;
    boolean allowUpdate = false;
    String productname = null;
    String Status = "";

    log.debug("entering into Requisition Submit");
    try {
      OBContext.setAdminMode();
      String strRequisitionId = (String) bundle.getParams().get("Escm_Material_Request_ID");
      MaterialIssueRequest objRequest = OBDal.getInstance().get(MaterialIssueRequest.class,
          strRequisitionId);
      String DocStatus = objRequest.getAlertStatus();
      String DocAction = objRequest.getEscmSmirAction();
      NextRoleByRuleVO nextApproval = null;
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objRequest.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      Date currentDate = new Date();
      String comments = (String) bundle.getParams().get("notes").toString(), query = "", sql = "";
      int count = 0;
      Boolean allowDelegation = false, chkRoleIsInDocRul = false, chkSubRolIsInFstRolofDR = false;

      if (!objRequest.getRole().getId().equals(roleId)) {
        OBQuery<MaterialIssueRequestHistory> history = OBDal.getInstance().createQuery(
            MaterialIssueRequestHistory.class,
            " as e where e.escmMaterialRequest.id='" + strRequisitionId
                + "' order by e.creationDate desc ");
        history.setMaxResult(1);
        if (history.list().size() > 0) {
          MaterialIssueRequestHistory apphistory = history.list().get(0);
          if (apphistory.getRequestreqaction().equals("REV")) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_AlreadyPreocessed_Approved@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // chk available remaining qty
      for (MaterialIssueRequestLine line : objRequest.getEscmMaterialReqlnList()) {
        query = " select coalesce((ln.quantity-coalesce(ln.sitereqissuedqty,0)- coalesce(inprg.inprgqty,0) ) ,0) as remainingqty,ln.escm_initialreceipt_id ,prd.name "
            + " from escm_initialreceipt  ln    left join m_product prd on prd.m_product_id= ln.m_product_id left join (  SELECT sum(reqln.requested_qty) AS inprgqty, reqln.escm_initialreceipt_id "
            + "      FROM escm_material_request req    LEFT JOIN escm_material_reqln reqln ON reqln.escm_material_request_id::text = req.escm_material_request_id::text "
            + "  WHERE req.status::text = 'ESCM_IP'::text AND req.issiteissuereq = 'Y'::bpchar  and  "
            + "  reqln.escm_material_reqln_id  <>  ?  "
            + "  GROUP BY reqln.escm_initialreceipt_id)  inprg ON inprg.escm_initialreceipt_id::text = ln.escm_initialreceipt_id::text "
            + "  where ln.escm_initialreceipt_id = ? ";
        log.debug("Check query:" + query);
        ps = conn.prepareStatement(query);
        ps.setString(1, line.getId());
        ps.setString(2, line.getEscmInitialreceipt().getId());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getBigDecimal("remainingqty").compareTo(line.getRequestedQty()) < 0) {
            errorFlag = true;
            String msg = OBMessageUtils.messageBD("ESCM_SMIR_ReqQtyExcRemQty");
            msg = msg.replace("%", rs.getString("remainingqty"));
            msg = msg.replace("@", rs.getString("name"));
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", msg);
            bundle.setResult(result);
            return;
          }
        }
      }
      // check role is present in document rule or not
      if (DocStatus.equals("DR")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, orgId, userId, roleId, Resource.MATERIAL_ISSUE_REQUEST, BigDecimal.ZERO);
        log.debug("chkRoleIsInDocRul:" + chkRoleIsInDocRul);
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");// ESCM_RoleIsNotIncInDocRule
          bundle.setResult(result);
          return;
        }
      }
      // chk submitting role is in first role in document rule
      if (DocStatus.equals("DR")) {
        chkSubRolIsInFstRolofDR = UtilityDAO.chkSubRolIsInFstRolofDR(OBDal.getInstance()
            .getConnection(), clientId, orgId, userId, roleId, Resource.MATERIAL_ISSUE_REQUEST,
            BigDecimal.ZERO);
        log.debug("chkSubRolIsInFstRolofDR:" + chkSubRolIsInFstRolofDR);
        if (!chkSubRolIsInFstRolofDR) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Role_NotFundsReserve_submit@");
          bundle.setResult(result);
          return;
        }
      }

      nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
          clientId, orgId, roleId, userId, Resource.MATERIAL_ISSUE_REQUEST, objRequest.getRole()
              .getId());
      /*
       * nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
       * orgId, roleId, userId, Resource.MATERIAL_ISSUE_REQUEST, BigDecimal.ZERO);
       */
      // check lines to submit
      if (objRequest.getEscmMaterialReqlnList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_No_Requisition_Lines@");
        bundle.setResult(result);
        return;
      }

      // check current role associated with document rule for approval flow
      if (!DocStatus.equals("DR") && !DocStatus.equals("ESCM_RJD")) {
        if (objRequest.getEUTNextRole() != null) {
          java.util.List<EutNextRoleLine> li = objRequest.getEUTNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (objRequest.getEUTNextRole() != null) {
          sql = "";
          Connection con = OBDal.getInstance().getConnection();
          PreparedStatement st = null;
          ResultSet rs1 = null;
          sql = "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
              + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_112'";
          st = con.prepareStatement(sql);
          rs1 = st.executeQuery();
          while (rs1.next()) {
            String roleid = rs1.getString("ad_role_id");
            if (roleid.equals(roleId)) {
              allowDelegation = true;
              break;
            }
          }
        }
        if (!allowUpdate && !allowDelegation) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }
      // throw the error message while 2nd user try to approve while 1st user already reworked that
      // record with same role
      if ((!vars.getUser().equals(objRequest.getCreatedBy().getId()))
          && DocStatus.equals("ESCM_RJD")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // throw an error in case if approver try to approving the record while the submit User is
      // already revoked the record
      if ((!vars.getUser().equals(objRequest.getCreatedBy().getId())) && DocStatus.equals("DR")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      /**
       ** Final Approval Quantity Check **
       */
      if (vars.getRole() != null) {
        // check role is warehouse Keeper then check qty
        OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(
            Preference.class,
            "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' and e.visibleAtRole.id='"
                + vars.getRole() + "'");
        if (preQuery.list().size() > 0) {
          // check Warehouse is empty
          if (objRequest.getWarehouse() == null) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_WarehouseEmpty@");
            bundle.setResult(result);
            return;
          }
          /*
           * // check custody product delivered qty // with custody lines if
           * (objRequest.getBeneficiaryType().equals("D") ||
           * objRequest.getBeneficiaryType().equals("E") ||
           * objRequest.getBeneficiaryType().equals("S")) { query =
           * "select ln.escm_material_reqln_id,coalesce(ccount,0) as ccount,prd.name as product from escm_material_reqln ln "
           * + " left join m_product prd on prd.m_product_id=ln.m_product_id  " +
           * " join (select escm_deflookups_typeln_id from escm_deflookups_type lt " +
           * " join escm_deflookups_typeln ltl on ltl.escm_deflookups_type_id=lt.escm_deflookups_type_id "
           * +
           * "  where lt.reference='PST' and ltl.value='CUS') cusref on cusref.escm_deflookups_typeln_id=prd.em_escm_stock_type "
           * +
           * " left join (select count(escm_mrequest_custody_id) as ccount,escm_material_reqln_id  from escm_mrequest_custody "
           * +
           * " group by escm_material_reqln_id ) cc on cc.escm_material_reqln_id=ln.escm_material_reqln_id "
           * + "  where ln.escm_material_request_id='" + strRequisitionId + "' " +
           * " and coalesce(ccount,0) <> coalesce(delivered_qty,0)"; ps =
           * conn.prepareStatement(query); rs = ps.executeQuery(); while (rs.next()) { if
           * (productname != null) productname += "," + rs.getString("product"); else productname =
           * rs.getString("product"); errorFlag = true; } if (errorFlag) { Status =
           * OBMessageUtils.messageBD("ESCM_CustodyQuantity_Less"); Status = Status.replace("%",
           * productname); OBDal.getInstance().rollbackAndClose(); obError.setType("Error");
           * obError.setTitle("Error"); obError.setMessage(Status); bundle.setResult(obError);
           * return;
           * 
           * }
           * 
           * }
           */
          query = "select (coalesce(mrec.quantity,0)-coalesce(sitereqissuedqty,0)) as avstock ,usedrec.uqty,mreq.escm_material_reqln_id as lnid "
              + " from escm_material_reqln mreq "
              + " left join escm_initialreceipt mrec on mreq.escm_initialreceipt_id=mrec.escm_initialreceipt_id "
              + " left join escm_material_request mmir on mmir.escm_material_request_id= mreq.escm_material_request_id "
              + " left join (select sum(req.delivered_qty) as uqty,rec.escm_initialreceipt_id from escm_material_reqln req "
              + " left join escm_initialreceipt rec on req.escm_initialreceipt_id=rec.escm_initialreceipt_id "
              + " left join escm_material_request mir on mir.escm_material_request_id= req.escm_material_request_id "
              + " where 1=1 and mir.status <>'DR' "
              + " group by rec.escm_initialreceipt_id ) usedrec on usedrec.escm_initialreceipt_id=mrec.escm_initialreceipt_id "
              + " where coalesce((usedrec.uqty),0) > coalesce(mrec.quantity,0) and  mreq.escm_material_request_id='"
              + strRequisitionId + "' ";
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            errorFlag = true;
            MaterialIssueRequestLine line = OBDal.getInstance().get(MaterialIssueRequestLine.class,
                rs.getString("lnid"));
            line.setFailureReason("Available quantity is " + rs.getString("avstock"));
            OBDal.getInstance().save(line);
            OBDal.getInstance().flush();
          }
          if (errorFlag) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_SIMRequestQty_Exceeds@");
            bundle.setResult(result);
            return;
          }
          query = "select (coalesce(mrec.quantity,0)-coalesce(sitereqissuedqty,0)) as avstock ,usedrec.uqty,mreq.escm_material_reqln_id as lnid "
              + " from escm_material_reqln mreq "
              + " left join escm_initialreceipt mrec on mreq.escm_initialreceipt_id=mrec.escm_initialreceipt_id "
              + " left join escm_material_request mmir on mmir.escm_material_request_id= mreq.escm_material_request_id "
              + " left join (select sum(req.delivered_qty) as uqty,rec.escm_initialreceipt_id from escm_material_reqln req "
              + " left join escm_initialreceipt rec on req.escm_initialreceipt_id=rec.escm_initialreceipt_id "
              + " left join escm_material_request mir on mir.escm_material_request_id= req.escm_material_request_id "
              + " where 1=1 and mir.status <>'DR' "
              + " group by rec.escm_initialreceipt_id ) usedrec on usedrec.escm_initialreceipt_id=mrec.escm_initialreceipt_id "
              + " where coalesce((usedrec.uqty),0) <= coalesce(mrec.quantity,0) and  mreq.escm_material_request_id='"
              + strRequisitionId + "' ";
          log.debug("Check query:" + query);
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            MaterialIssueRequestLine line = OBDal.getInstance().get(MaterialIssueRequestLine.class,
                rs.getString("lnid"));
            line.setFailureReason("");
            OBDal.getInstance().save(line);
            OBDal.getInstance().flush();
          }

        }
      }
      if (!errorFlag) {
        if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_RJD")) && DocAction.equals("CO")) {
          appstatus = "SUB";
        } else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
          appstatus = "AP";
        }
        count = updateHeaderStatus(conn, clientId, orgId, roleId, userId, objRequest, appstatus,
            comments, currentDate, vars, nextApproval, Lang);
        log.debug("count:" + count);
        if (count == 2) {
          String existingDocNo = "1000000001";
          int custodyCount = 0, deliveredQty = 0;
          // final Approval Flow
          // entry in Transaction
          // get Line List
          // get recent tag number
          OBQuery<MaterialIssueRequestCustody> objCustodyQry = OBDal.getInstance().createQuery(
              MaterialIssueRequestCustody.class,
              "as e where e.organization.id='" + objRequest.getOrganization().getId()
                  + "' order by creationDate desc");
          objCustodyQry.setMaxResult(1);
          if (objCustodyQry.list().size() > 0) {
            MaterialIssueRequestCustody recentObj = objCustodyQry.list().get(0);
            if (recentObj.getDocumentNo() != null
                && StringUtils.isNotEmpty(recentObj.getDocumentNo()))
              existingDocNo = String.valueOf(Integer.parseInt(recentObj.getDocumentNo()) + 1);
          }

          // make custody for only custody products
          query = " select escm_material_reqln_id from escm_material_reqln ln "
              + " join m_product prd on prd.m_product_id=ln.m_product_id "
              + " join (select escm_deflookups_typeln_id from escm_deflookups_type lt "
              + " join escm_deflookups_typeln ltl on ltl.escm_deflookups_type_id=lt.escm_deflookups_type_id "
              + " where lt.reference='PST' and ltl.value='CUS') cusref on cusref.escm_deflookups_typeln_id=prd.em_escm_stock_type "
              + " where ln.escm_material_request_id='" + strRequisitionId + "'";
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            // check already existing custody line count
            MaterialIssueRequestLine objLineList = OBDal.getInstance().get(
                MaterialIssueRequestLine.class, rs.getString("escm_material_reqln_id"));
            custodyCount = objLineList.getEscmMrequestCustodyList().size();
            deliveredQty = objLineList.getDeliveredQantity().intValue();
            log.debug("existingDocNo:" + existingDocNo);
            log.debug("deliveredQty:" + deliveredQty);
            // no custody line insert the custodies line
            if (custodyCount == 0) {
              for (int i = 1; i <= deliveredQty; i++) {
                // get existing tag no
                MaterialIssueRequestCustody objCustody = OBProvider.getInstance().get(
                    MaterialIssueRequestCustody.class);
                Product objProduct = OBDal.getInstance().get(Product.class,
                    objLineList.getProduct().getId());
                objCustody.setProductCategory(objProduct.getProductCategory());
                objCustody.setDocumentNo(existingDocNo);
                objCustody.setQuantity(BigDecimal.ONE);
                objCustody.setDescription(objLineList.getDescription());
                objCustody.setAlertStatus("IU");
                objCustody.setOrganization(objLineList.getOrganization());
                objCustody.setEscmMaterialReqln(objLineList);
                objCustody.setProduct(objProduct);
                if (objProduct.getEscmCusattribute() != null)
                  objCustody.setAttributeSet(objProduct.getEscmCusattribute());
                objCustody.setBeneficiaryType(objRequest.getBeneficiaryType());
                objCustody.setBeneficiaryIDName(objRequest.getBeneficiaryIDName());
                objCustody.setProcurement(objLineList.getConditions());
                OBDal.getInstance().save(objCustody);
                // create Custody Transaction
                Escm_custody_transaction objCustodyhistory = OBProvider.getInstance().get(
                    Escm_custody_transaction.class);
                objCustodyhistory.setLineNo(Long.valueOf(10));
                objCustodyhistory.setDocumentNo(objRequest.getDocumentNo());
                objCustodyhistory.setOrganization(objCustody.getOrganization());
                objCustodyhistory.setBname(objRequest.getBeneficiaryIDName());
                objCustodyhistory.setBtype(objRequest.getBeneficiaryType());
                objCustodyhistory.setEscmMrequestCustody(objCustody);
                objCustodyhistory.setTransactionDate(objRequest.getTransactionDate());
                objCustodyhistory.setTransactiontype("IE");
                objCustodyhistory.setProcessed(true);
                objCustodyhistory.setLine2(Long.valueOf(10));
                OBDal.getInstance().save(objCustodyhistory);
                OBDal.getInstance().flush();
                existingDocNo = String.valueOf(Integer.parseInt(existingDocNo) + 1);

              }
            }
          }

          // ----------------------------------
          for (MaterialIssueRequestLine objRequestLine : objRequest.getEscmMaterialReqlnList()) {
            if (objRequestLine.getDeliveredQantity().compareTo(BigDecimal.ZERO) == 1) {
              // ----update in po receipt
              EscmInitialReceipt objInitiaReceipt = objRequestLine.getEscmInitialreceipt();
              objInitiaReceipt.setSitereqissuedqty(objInitiaReceipt.getSitereqissuedqty().add(
                  objRequestLine.getDeliveredQantity()));
              OBDal.getInstance().save(objInitiaReceipt);
            }
          }

          OBDal.getInstance().flush();
        }
        if (count > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else {
          errorFlag = false;
        }
      }
      if (errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("Process Failed");
      }
      bundle.setResult(obError);
      OBDal.getInstance().save(objRequest);
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.MATERIAL_ISSUE_REQUEST);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Exeception in Site Material Issue Request Submit:" + e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, MaterialIssueRequest objRequest, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars, NextRoleByRuleVO nextApproval, String Lang) {
    String requistionId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.SiteIssueRequest, strRoleId = "";
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = objRequest.getCreatedBy();
    try {
      OBContext.setAdminMode();

      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);
      NextRoleByRuleVO objnextApproval = nextApproval;
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      isDirectApproval = isDirectApproval(objRequest.getId(), roleId);
      strRoleId = objRequest.getRole().getId();

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      if ((objRequest.getEUTNextRole() == null)) {
        objnextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
            clientId, orgId, roleId, userId, Resource.MATERIAL_ISSUE_REQUEST, strRoleId);
      } else {
        if (isDirectApproval) {
          objnextApproval = NextRoleByRule.getRequesterNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId,
              Resource.MATERIAL_ISSUE_REQUEST, strRoleId);

          if (objnextApproval != null && objnextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, objnextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(OBDal.getInstance()
                    .getConnection(), clientId, orgId, userRole.list().get(0).getUserContact()
                    .getId(), Resource.MATERIAL_ISSUE_REQUEST, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(OBDal.getInstance()
                    .getConnection(), clientId, orgId, delegatedFromRole, delegatedToRole, userId,
                    Resource.MATERIAL_ISSUE_REQUEST, 0.00);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            objnextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, delegatedFromRole, userId, Resource.MATERIAL_ISSUE_REQUEST, 0.00);
          }
        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, userId, Resource.MATERIAL_ISSUE_REQUEST, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            objnextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance()
                .getConnection(), clientId, orgId, delegatedFromRole, delegatedToRole, userId,
                Resource.MATERIAL_ISSUE_REQUEST, 0.00);
        }
      }

      if (objnextApproval != null && objnextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, objnextApproval.getNextRoleId());
        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequest.setAlertStatus("ESCM_IP");
        objRequest.setEUTNextRole(nextRole);
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
            AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(
              Alert.class,
              "as e where e.referenceSearchKey='" + objRequest.getId()
                  + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
            }
          }
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.smir.wfa", Lang)
              + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
                objNextRoleLine.getRole().getId(), "", objRequest.getClient().getId(), Description,
                "NEW", alertWindow);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                    + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                    + "' and hd.date >='" + currentDate + "' and e.documentType='EUT_112'");
            if (delegationln != null && delegationln.list().size() > 0) {
              if (pendingapproval != null)
                pendingapproval += "/" + delegationln.list().get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL, delegationln
                    .list().get(0).getUserContact().getName());
            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }
        objRequest.setEscmSmirAction("AP");
        if (pendingapproval == null)
          pendingapproval = objnextApproval.getStatus();

        log.debug("doc sts:" + objRequest.getAlertStatus() + "action:" + objRequest.getEscmAction());
        count = 1; // Waiting For Approval flow

      } else {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequest.setAlertStatus("ESCM_TR");
        Role objCreatedRole = null;
        if (objRequest.getCreatedBy().getADUserRolesList().size() > 0) {
          objCreatedRole = objRequest.getCreatedBy().getADUserRolesList().get(0).getRole();
        }
        // delete alert for approval alerts
        OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            "as e where e.referenceSearchKey='" + objRequest.getId() + "' and e.alertStatus='NEW'");
        if (alertQuery.list().size() > 0) {
          for (Alert objAlert : alertQuery.list()) {
            objAlert.setAlertStatus("SOLVED");
          }
        }
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
            AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
        // check and insert recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.smir.approved",
            Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(), "",
            objRequest.getCreatedBy().getId(), objRequest.getClient().getId(), Description, "NEW",
            alertWindow);
        objRequest.setEUTNextRole(null);
        objRequest.setEscmSmirAction("PD");
        count = 2; // Final Approval Flow
      }

      OBDal.getInstance().save(objRequest);
      requistionId = objRequest.getId();
      if (!StringUtils.isEmpty(requistionId)) {
        JSONObject historyData = new JSONObject();

        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", requistionId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
        historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.MATERIAL_ISSUE_REQUEST);

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in Site Material IssueRequest: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  public boolean isDirectApproval(String RequestId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(req.escm_material_request_id) from escm_material_request req join eut_next_role rl on "
          + "req.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and req.escm_material_request_id = ? and li.ad_role_id =?";

      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, RequestId);
        ps.setString(2, roleId);

        rs = ps.executeQuery();

        if (rs.next()) {
          if (rs.getInt("count") > 0)
            return true;
          else
            return false;
        } else
          return false;
      } else
        return false;
    } catch (Exception e) {
      log.error("Exception in isDirectApproval " + e.getMessage());
      return false;
    }
  }
}