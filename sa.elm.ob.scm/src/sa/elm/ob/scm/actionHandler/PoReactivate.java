package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.scm.EscmAddreceipt;
import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.utility.util.Utility;

public class PoReactivate implements Process {
  private final static OBError obError = new OBError();
  private static Logger log4j = Logger.getLogger(PoReactivate.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    Connection connection = null;
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    Boolean flag = false;
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log4j.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }
    final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
    log4j.debug("receiptId:" + receiptId);
    try {
      OBContext.setAdminMode(true);
      Connection conn = OBDal.getInstance().getConnection();
      PreparedStatement ps = null;
      ResultSet rs = null;
      ShipmentInOut header = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      String query2 = null;
      PreparedStatement ps1 = null;
      ResultSet rs1 = null;
      boolean errorFlag = false;
      OBQuery<EscmAddreceipt> addReceipt = OBDal.getInstance().createQuery(EscmAddreceipt.class,
          "receipt.id='" + receiptId + "'");
      if (addReceipt.list() != null && addReceipt.list().size() > 0) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("Escm_Po_React_Err"));
        bundle.setResult(obError);
        return;
      } else {
        OBQuery<EscmInitialReceipt> receipt = OBDal.getInstance().createQuery(
            EscmInitialReceipt.class, "goodsShipment.id='" + receiptId + "'");
        List<EscmInitialReceipt> receiptList = new ArrayList<EscmInitialReceipt>();
        receiptList = receipt.list();

        if (header.getEscmReceivingtype().equals("SR")) {
          OBQuery<MaterialIssueRequestLine> reqline = OBDal.getInstance().createQuery(
              MaterialIssueRequestLine.class,
              " escmInitialreceipt.id in ( select e.id from Escm_InitialReceipt e where e.goodsShipment.id='"
                  + header.getId() + "')");
          if (reqline.list().size() > 0) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_SRCantReactivate@");
            bundle.setResult(result);
            // OBDal.getInstance().rollbackAndClose();
            return;
          }

          for (EscmInitialReceipt ir : receiptList) {
            ir.setDeliveredQty(BigDecimal.ZERO);
            OBDal.getInstance().save(ir);
            if (((((ir.getAcceptedQty().add(ir.getRejectedQty())).add(ir.getDeliveredQty())).add(ir
                .getReturnQty())).add(ir.getReturnQuantity())).compareTo(BigDecimal.ZERO) > 0) {
              obError.setType("Error");
              obError.setTitle("Error");
              obError.setMessage(OBMessageUtils.messageBD("Escm_Po_React_Err"));
              bundle.setResult(obError);
              return;
            }
          }

        } else if ((header.isEscmIscustodyTransfer() == null || !header.isEscmIscustodyTransfer())
            && header.getEscmReceivingtype().equals("IR")) {
          for (EscmInitialReceipt ir : receiptList) {
            if (ir.getProduct().isStocked()) {
              if (ir.getProduct().isEscmNoinspection()) {
                ir.setAcceptedQty(BigDecimal.ZERO);
              }
            } else {
              ir.setDeliveredQty(BigDecimal.ZERO);
            }
            OBDal.getInstance().save(ir);
            if (((((ir.getAcceptedQty().add(ir.getRejectedQty())).add(ir.getDeliveredQty())).add(ir
                .getReturnQty())).add(ir.getReturnQuantity())).compareTo(BigDecimal.ZERO) > 0) {
              obError.setType("Error");
              obError.setTitle("Error");
              obError.setMessage(OBMessageUtils.messageBD("Escm_Po_React_Err"));
              bundle.setResult(obError);
              return;
            }
          }
        } else if (header.getEscmReceivingtype().equals("INS")) {
          int inscount = reactivateInspectionProcess(connection, header.getClient().getId(), header
              .getOrganization().getId(), header, bundle, vars);
          log4j.debug("count:" + inscount);
          if (inscount == 2) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_ProcessFailed(InspectionUsed)@");
            bundle.setResult(result);
            OBDal.getInstance().rollbackAndClose();
            return;
          } else if (inscount == 1) {
            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@Escm_Ir_complete_success@");
            bundle.setResult(result);
            header.setEscmDocstatus("DR");
            header.setDocumentStatus("DR");
            header.setDocumentAction("CO");
            header.setProcessed(false);
            OBDal.getInstance().save(header);
            OBDal.getInstance().flush();
            return;
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_ProcessFailed@");
            bundle.setResult(result);
            return;
          }
        } else if (header.getEscmReceivingtype().equals("RET")) {
          for (EscmInitialReceipt ir : receiptList) {
            EscmInitialReceipt initial = OBDal.getInstance().get(EscmInitialReceipt.class,
                ir.getSourceRef().getId());
            if (ir.getAlertStatus().equals("I")) {
              initial.setReturnQuantity((initial.getReturnQuantity().subtract(ir.getQuantity())));
            } else if (ir.getAlertStatus().equals("A")) {
              initial.setReturnQty((initial.getReturnQty().subtract(ir.getQuantity())));
              initial.setAcceptedQty(initial.getAcceptedQty().add(ir.getQuantity()));
            } else if (ir.getAlertStatus().equals("R")) {
              initial.setReturnQty((initial.getReturnQty().subtract(ir.getQuantity())));
              initial.setRejectedQty(initial.getRejectedQty().add(ir.getQuantity()));
            } else if (ir.getAlertStatus().equals("D")) {
              if (initial.getGoodsShipment().getEscmReceivingtype().equals("SR")) {
                initial.setReturnQty((initial.getReturnQty().subtract(ir.getQuantity())));
                initial.setDeliveredQty(initial.getDeliveredQty().add(ir.getQuantity()));
              } else {
                initial.setReturnQty((initial.getReturnQty().subtract(ir.getQuantity())));
                initial.setDeliveredQty(initial.getDeliveredQty().add(ir.getQuantity()));
                // while complete inset in m_inout_line.
                // while reactivate delete line in transaction and m_inout_lines.
                OBQuery<ShipmentInOutLine> sline = OBDal.getInstance().createQuery(
                    ShipmentInOutLine.class,
                    "escmInitialreceipt.id='" + ir.getId() + "' and shipmentReceipt.id='"
                        + ir.getGoodsShipment().getId() + "'");
                if (sline.list() != null && sline.list().size() > 0) {
                  String slineId = sline.list().get(0).getId();
                  OBQuery<MaterialTransaction> Mtrans = OBDal.getInstance().createQuery(
                      MaterialTransaction.class, "goodsShipmentLine.id='" + slineId + "'");
                  if (Mtrans.list() != null && Mtrans.list().size() > 0) {
                    MaterialTransaction trans = Mtrans.list().get(0);
                    OBDal.getInstance().remove(trans);
                  }
                  header.setEscmDocstatus("DR");
                  header.setDocumentStatus("DR");
                  header.setProcessed(false);
                  header.setDocumentAction("CO");
                  OBDal.getInstance().save(header);
                  ShipmentInOutLine line = sline.list().get(0);
                  OBDal.getInstance().remove(line);
                }
                OBDal.getInstance().save(initial);
                // Then insert in m_inout_line and use in transaction.
              }
              OBDal.getInstance().flush();
            }
          }
        } else if (header.getEscmReceivingtype().equals("DEL")) {
          int inscount = reactivateDeliveryProcess(connection, header.getClient().getId(), header
              .getOrganization().getId(), header, bundle, vars);
          log4j.debug("count:" + inscount);
          if (inscount == 2) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_ProcessFailed(DeliveryUsed)@");
            bundle.setResult(result);
            // OBDal.getInstance().rollbackAndClose();
            return;
          } else if (inscount == -1) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_StorageDetail_QtyonHand@");
            bundle.setResult(result);
            // OBDal.getInstance().rollbackAndClose();
            return;
          } else if (inscount == 1) {
            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@Escm_Ir_complete_success@");
            bundle.setResult(result);
            header.setEscmDocstatus("DR");
            header.setDocumentStatus("DR");
            header.setDocumentAction("CO");
            header.setProcessed(false);
            OBDal.getInstance().save(header);
            OBDal.getInstance().flush();
            return;
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_ProcessFailed@");
            bundle.setResult(result);
            return;
          }
        } else if (header.getEscmReceivingtype().equals("INR")) {
          for (ShipmentInOutLine inout : header.getMaterialMgmtShipmentInOutLineList()) {
            for (Escm_custody_transaction transaction : inout.getEscmCustodyTransactionList()) {
              if (transaction.getReturnDate() != null) {
                flag = true;
                break;
              }
            }
          }
          if (flag) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Reactivate_RT@");
            bundle.setResult(result);
            return;
          } else {
            for (ShipmentInOutLine line : header.getMaterialMgmtShipmentInOutLineList()) {
              OBQuery<MaterialTransaction> transaction = OBDal.getInstance().createQuery(
                  MaterialTransaction.class,
                  " as e where e.goodsShipmentLine.id='" + line.getId() + "'");
              if (transaction.list().size() > 0) {
                for (MaterialTransaction transc : transaction.list()) {
                  MaterialTransaction transactionhd = transc;
                  int chkqty = Utility.ChkStoragedetOnhandQtyNeg(
                      transactionhd.getProduct().getId(), transactionhd.getStorageBin().getId(),
                      transactionhd.getMovementQuantity(), transactionhd.getClient().getId());
                  if (chkqty == -1) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_StorageDetail_QtyonHand@");
                    bundle.setResult(result);
                    return;
                  } else {
                    OBDal.getInstance().remove(transactionhd);
                    OBDal.getInstance().flush();
                  }
                }
              }
              header.setEscmDocstatus("DR");
              header.setDocumentStatus("DR");
              header.setEscmDocaction("CO");
              OBQuery<Escm_custody_transaction> custran = OBDal.getInstance().createQuery(
                  Escm_custody_transaction.class,
                  " as e where e.goodsShipmentLine.id='" + line.getId() + "'");
              if (custran.list().size() > 0) {
                for (Escm_custody_transaction tr : custran.list()) {
                  tr.setProcessed(false);
                  OBDal.getInstance().save(tr);
                  OBDal.getInstance().flush();
                  OBQuery<Escm_custody_transaction> custransa = OBDal.getInstance().createQuery(
                      Escm_custody_transaction.class,
                      " as e where e.escmMrequestCustody.id='"
                          + tr.getEscmMrequestCustody().getId()
                          + "' and e.isProcessed = 'Y' order by e.creationDate desc ");
                  custransa.setMaxResult(1);
                  if (custransa.list().size() > 0) {
                    Escm_custody_transaction custransaction = custransa.list().get(0);
                    custransaction.setReturnDate(null);
                    if (custransaction.getTransactiontype().equals("MA")) {
                      custransaction.getEscmMrequestCustody().setAlertStatus("MA");
                      custransaction.getEscmMrequestCustody().setBeneficiaryIDName(
                          custransaction.getBname());
                      custransaction.getEscmMrequestCustody().setBeneficiaryType(
                          custransaction.getBtype());
                    } else {
                      custransaction.getEscmMrequestCustody().setAlertStatus("IU");

                    }
                  }

                }
              }
            }
          }
        }
        // Issue Return Transaction window 'Reactivate' Process
        else if (header.getEscmReceivingtype().equals("IRT")) {
          for (ShipmentInOutLine inout : header.getMaterialMgmtShipmentInOutLineList()) {
            for (Escm_custody_transaction transaction : inout.getEscmCustodyTransactionList()) {
              if (transaction.getReturnDate() != null) {
                flag = true;
                break;
              }
            }
          }
          if (flag) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_IRT_Can't_Reactivate@");
            bundle.setResult(result);
            return;
          } else {
            for (ShipmentInOutLine line : header.getMaterialMgmtShipmentInOutLineList()) {
              OBQuery<MaterialTransaction> transaction = OBDal.getInstance().createQuery(
                  MaterialTransaction.class,
                  " as e where e.goodsShipmentLine.id='" + line.getId() + "'");
              if (transaction.list().size() > 0) {
                for (MaterialTransaction transc : transaction.list()) {
                  MaterialTransaction transactionhd = transc;
                  int chkqty = Utility.ChkStoragedetOnhandQtyNeg(
                      transactionhd.getProduct().getId(), transactionhd.getStorageBin().getId(),
                      transactionhd.getMovementQuantity(), transactionhd.getClient().getId());
                  if (chkqty == -1) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_StorageDetail_QtyonHand@");
                    bundle.setResult(result);
                    return;
                  } else {
                    OBDal.getInstance().remove(transactionhd);
                    OBDal.getInstance().flush();
                  }
                }
              }
              header.setEscmDocstatus("DR");
              header.setDocumentStatus("DR");
              header.setDocumentAction("CO");
              OBQuery<Escm_custody_transaction> custran = OBDal.getInstance().createQuery(
                  Escm_custody_transaction.class,
                  " as e where e.goodsShipmentLine.id='" + line.getId() + "'");
              if (custran.list().size() > 0) {
                for (Escm_custody_transaction tr : custran.list()) {
                  tr.getEscmMrequestCustody().setAlertStatus("RET");
                  tr.setProcessed(false);
                  OBDal.getInstance().save(tr);
                  OBDal.getInstance().flush();
                  OBQuery<Escm_custody_transaction> custransa = OBDal.getInstance().createQuery(
                      Escm_custody_transaction.class,
                      " as e where e.escmMrequestCustody.id='"
                          + tr.getEscmMrequestCustody().getId()
                          + "' and e.isProcessed = 'Y' order by e.creationDate desc ");
                  custransa.setMaxResult(1);
                  if (custransa.list().size() > 0) {
                    Escm_custody_transaction custransaction = custransa.list().get(0);
                    custransaction.setReturnDate(null);
                    custransaction.getEscmMrequestCustody().setBeneficiaryIDName(
                        custransaction.getBname());
                    custransaction.getEscmMrequestCustody().setBeneficiaryType(
                        custransaction.getBtype());
                  }

                }
              }
            }

          }

        } else if (header.isEscmIscustodyTransfer() && header.getDocumentStatus().equals("CO")) {
          for (ShipmentInOutLine line : header.getMaterialMgmtShipmentInOutLineList()) {
            for (Escm_custody_transaction trans : line.getEscmCustodyTransactionList()) {
              if (trans.getReturnDate() != null) {
                flag = true;
                break;
              }
            }
          }
          if (flag) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_IRT_Can't_Reactivate@");
            bundle.setResult(result);
            return;
          } else {
            for (ShipmentInOutLine line : header.getMaterialMgmtShipmentInOutLineList()) {
              for (Escm_custody_transaction trans : line.getEscmCustodyTransactionList()) {
                trans.setProcessed(false);
                OBDal.getInstance().save(trans);
                OBDal.getInstance().flush();
                OBQuery<Escm_custody_transaction> custransa = OBDal.getInstance().createQuery(
                    Escm_custody_transaction.class,
                    " as e where e.escmMrequestCustody.id='"
                        + trans.getEscmMrequestCustody().getId()
                        + "' and e.isProcessed = 'Y' order by e.creationDate desc ");
                custransa.setMaxResult(1);
                if (custransa.list().size() > 0) {
                  Escm_custody_transaction custransaction = custransa.list().get(0);
                  custransaction.setReturnDate(null);
                  custransaction.getEscmMrequestCustody().setBeneficiaryIDName(
                      custransaction.getBname());
                  custransaction.getEscmMrequestCustody().setBeneficiaryType(
                      custransaction.getBtype());
                }
              }
            }
            header.setEscmDocstatus("DR");
            header.setDocumentStatus("DR");
            header.setDocumentAction("CO");
            header.setEscmCtdocaction("CO");
            header.setProcessed(false);
            OBDal.getInstance().save(header);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();

            obError.setType("Success");
            obError.setTitle("Success");
            obError.setMessage(OBMessageUtils.messageBD("ESCM_CT_Rea_Success"));
            bundle.setResult(obError);
            return;
          }
        }
        header.setEscmDocstatus("DR");
        header.setDocumentStatus("DR");
        header.setDocumentAction("CO");
        header.setEscmCtdocaction("CO");
        header.setProcessed(false);
        OBDal.getInstance().save(header);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();

        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage(OBMessageUtils.messageBD("Escm_Po_React_succ"));
        bundle.setResult(obError);
        return;
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log4j.error("exception in po reactivate:", e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param objInout
   * @return Inspected used in Delivery then 2 ,process success return count 1, process error return
   *         count 0
   */
  public static int reactivateInspectionProcess(Connection con, String clientId, String orgId,
      ShipmentInOut objInout, ProcessBundle bundle, VariablesSecureApp vars) throws OBException,
      SQLException, HibernateException {
    String objId = objInout.getId();
    int count = 1;
    String query = "", query1 = "", query2 = "";
    PreparedStatement ps1 = null, ps = null, ps2 = null;
    Boolean isProceed = true;
    ResultSet rs = null, rs1 = null, rs2 = null;

    try {
      OBContext.setAdminMode(true);
      // check Inspected Receipt Used in Delivery
      query = " select distinct escm_addreceipt_id from escm_addreceipt where inspection ='"
          + objId + "'";
      ps = con.prepareStatement(query);
      log4j.debug("InspectionCheck:" + ps.toString());
      rs = ps.executeQuery();
      if (rs.next()) {
        isProceed = false;
        count = 2;
      }
      query1 = " select sum(quantity) as qty,source_ref from escm_initialreceipt where m_inout_id='"
          + objId + "' " + " and status='A' group by source_ref ";
      ps1 = con.prepareStatement(query1);
      rs1 = ps1.executeQuery();
      while (rs1.next()) {
        EscmInitialReceipt objIrReceipt = OBDal.getInstance().get(EscmInitialReceipt.class,
            rs1.getString("source_ref"));
        if (objIrReceipt.getAcceptedQty().compareTo(rs1.getBigDecimal("qty")) < 0) {
          isProceed = false;
          count = 2;
          break;
        }
      }
      // update RejectQty in Initial Receipt
      query2 = " select sum(quantity) as qty,source_ref from escm_initialreceipt where m_inout_id='"
          + objId + "' " + " and status='R' group by source_ref ";
      ps2 = con.prepareStatement(query2);
      rs2 = ps2.executeQuery();
      while (rs2.next()) {
        EscmInitialReceipt objIrReceipt = OBDal.getInstance().get(EscmInitialReceipt.class,
            rs2.getString("source_ref"));
        if (objIrReceipt.getRejectedQty().compareTo(rs2.getBigDecimal("qty")) < 0) {
          isProceed = false;
          count = 2;
          break;
        }
      }
      rs.close();
      OBDal.getInstance().flush();

      if (isProceed) {
        // update AcceptQty in Initial receipt
        query1 = " select sum(quantity) as qty,source_ref from escm_initialreceipt where m_inout_id='"
            + objId + "' " + " and status='A' group by source_ref ";
        ps1 = con.prepareStatement(query1);
        rs1 = ps1.executeQuery();
        while (rs1.next()) {
          EscmInitialReceipt objIrReceipt = OBDal.getInstance().get(EscmInitialReceipt.class,
              rs1.getString("source_ref"));
          objIrReceipt.setAcceptedQty(objIrReceipt.getAcceptedQty().subtract(
              (rs1.getBigDecimal("qty"))));
          OBDal.getInstance().save(objIrReceipt);
        }
        rs1.close();
        // update RejectQty in Initial Receipt
        query2 = " select sum(quantity) as qty,source_ref from escm_initialreceipt where m_inout_id='"
            + objId + "' " + " and status='R' group by source_ref ";
        ps2 = con.prepareStatement(query2);
        rs2 = ps2.executeQuery();
        while (rs2.next()) {
          EscmInitialReceipt objIrReceipt = OBDal.getInstance().get(EscmInitialReceipt.class,
              rs2.getString("source_ref"));
          objIrReceipt.setRejectedQty(objIrReceipt.getRejectedQty().subtract(
              (rs2.getBigDecimal("qty"))));
          OBDal.getInstance().save(objIrReceipt);
        }
        rs2.close();
        count = 1;
      }

    } catch (Exception e) {
      count = 0;
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log4j.error("Exception in reactivateInspectionProcess: ", e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param objInout
   * @return Delivery used in inspect then 2 ,process success return count 1, process error return
   *         count 0
   */
  public static int reactivateDeliveryProcess(Connection con, String clientId, String orgId,
      ShipmentInOut objInout, ProcessBundle bundle, VariablesSecureApp vars) throws OBException,
      SQLException, HibernateException {
    String objId = objInout.getId();
    int count = 1;
    String query = "", query1 = "", query2 = "";
    PreparedStatement ps1 = null, ps = null, ps2 = null;
    Boolean isProceed = true;
    ResultSet rs = null, rs1 = null, rs2 = null;

    try {
      OBContext.setAdminMode(true);
      // check Inspected Receipt Used in Delivery
      query = " select distinct escm_addreceipt_id from escm_addreceipt where delivery ='" + objId
          + "'";
      ps = con.prepareStatement(query);
      log4j.debug("deliveryCheck:" + ps.toString());
      rs = ps.executeQuery();
      if (rs.next()) {
        isProceed = false;
        count = 2;
      }
      rs.close();
      OBDal.getInstance().flush();

      query1 = " select sum(quantity) as qty,source_ref,escm_initialreceipt_id from escm_initialreceipt where m_inout_id='"
          + objId + "' " + "  group by source_ref,escm_initialreceipt_id ";
      ps1 = con.prepareStatement(query1);
      log4j.debug("deliveryCheck1:" + ps1.toString());
      rs1 = ps1.executeQuery();
      while (rs1.next()) {
        EscmInitialReceipt currobjIrReceipt = OBDal.getInstance().get(EscmInitialReceipt.class,
            rs1.getString("escm_initialreceipt_id"));

        EscmInitialReceipt objIrReceipt = OBDal.getInstance().get(EscmInitialReceipt.class,
            rs1.getString("source_ref"));
        if (objIrReceipt.getDeliveredQty().compareTo(rs1.getBigDecimal("qty")) < 0) {
          String msg = OBMessageUtils.messageBD("ESCM_AvlQtyLessThanPrsQty");
          msg = msg.replace("%",
              (rs1.getBigDecimal("qty").subtract(objIrReceipt.getReturnQty())).toString());
          log4j.debug("msg:" + msg);
          currobjIrReceipt.setFailurereason(msg);
          OBDal.getInstance().save(currobjIrReceipt);
          OBDal.getInstance().flush();
          isProceed = false;
          count = 2;
          break;
        }
      }

      if (isProceed) {
        // update AcceptQty in Initial receipt
        query1 = " select sum(quantity) as qty,source_ref from escm_initialreceipt where m_inout_id='"
            + objId + "' " + "  group by source_ref ";
        ps1 = con.prepareStatement(query1);
        log4j.debug("deliveryCheck1:" + ps1.toString());
        rs1 = ps1.executeQuery();
        while (rs1.next()) {
          EscmInitialReceipt objIrReceipt = OBDal.getInstance().get(EscmInitialReceipt.class,
              rs1.getString("source_ref"));
          objIrReceipt.setDeliveredQty(objIrReceipt.getDeliveredQty().subtract(
              (rs1.getBigDecimal("qty"))));
          objIrReceipt
              .setAcceptedQty(objIrReceipt.getAcceptedQty().add((rs1.getBigDecimal("qty"))));
          OBDal.getInstance().save(objIrReceipt);
        }
        rs1.close();

        OBQuery<ShipmentInOutLine> inoutline = OBDal.getInstance().createQuery(
            ShipmentInOutLine.class, " as e where e.shipmentReceipt.id='" + objInout.getId() + "'");
        log4j.debug("transaction:" + inoutline.list().size());
        if (inoutline.list().size() > 0) {
          objInout.setEscmDocstatus("DR");
          objInout.setDocumentStatus("DR");
          objInout.setDocumentAction("CO");
          objInout.setProcessed(false);
          OBDal.getInstance().save(objInout);

          for (ShipmentInOutLine line : inoutline.list()) {
            ShipmentInOutLine shipline = line;
            OBQuery<MaterialTransaction> transaction = OBDal.getInstance().createQuery(
                MaterialTransaction.class,
                " as e where e.goodsShipmentLine.id='" + shipline.getId() + "'");
            if (transaction.list().size() > 0) {
              for (MaterialTransaction tran : transaction.list()) {
                MaterialTransaction transactionhd = tran;
                int chkqty = Utility.ChkStoragedetOnhandQtyNeg(transactionhd.getProduct().getId(),
                    transactionhd.getStorageBin().getId(), transactionhd.getMovementQuantity(),
                    transactionhd.getClient().getId());
                if (chkqty == -1) {
                  return -1;
                } else {
                  OBDal.getInstance().remove(transactionhd);
                  OBDal.getInstance().flush();
                }
              }
            }
            OBDal.getInstance().remove(shipline);
            OBDal.getInstance().flush();
          }
        }
        count = 1;
      }

    } catch (Exception e) {
      count = 0;
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log4j.error("Exception in reactivateDeliveryProcess: ", e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }
}
