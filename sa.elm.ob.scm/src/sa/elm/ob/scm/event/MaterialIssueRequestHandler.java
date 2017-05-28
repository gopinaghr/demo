package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.domain.Preference;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gopalakrishnan on 10/03/2017
 * 
 */
public class MaterialIssueRequestHandler extends EntityPersistenceEventObserver {
  /**
   * This Class is responsible for business events in Escm_Material_Request Table
   */
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      MaterialIssueRequest.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());
  HttpServletRequest request = RequestContext.get().getRequest();
  VariablesSecureApp vars = new VariablesSecureApp(request);

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Long code = Long.valueOf("0001");
      String newCode = "", sequence = "";
      boolean sequenceexists = false;
      MaterialIssueRequest objRequest = (MaterialIssueRequest) event.getTargetInstance();
      final Property DocNo = entities[0].getProperty(MaterialIssueRequest.PROPERTY_DOCNO);
      final Property SpecRef = entities[0].getProperty(MaterialIssueRequest.PROPERTY_DOCUMENTNO);
      final Property requesterRole = entities[0].getProperty(MaterialIssueRequest.PROPERTY_ROLE);
      // get role
      event.setCurrentState(requesterRole, OBContext.getOBContext().getRole());
      UserRoles objUserRole = objRequest.getUpdatedBy().getADUserRolesList().get(0);
      if (objUserRole != null) {
        Role objRole = objUserRole.getRole();
        log.debug("objRole:" + objRole.getName());
        // check role is warehouse Keeper
        OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(
            Preference.class,
            "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' and e.visibleAtRole.id='"
                + objRole.getId() + "'");
        if (preQuery.list().size() > 0 && objRequest.getWarehouse() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_WarehouseEmpty"));
        }

      }

      /*
       * // assign doc no // 1438H0001 DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
       * Date now = new Date(); Date todaydate = null; try { todaydate =
       * dateFormat.parse(dateFormat.format(now)); } catch (ParseException e) { // TODO
       * Auto-generated catch block e.printStackTrace(); } SimpleDateFormat dateYearFormat = new
       * SimpleDateFormat("yyyy-MM-dd"); String hijiridate =
       * convertTohijriDate(dateYearFormat.format(todaydate)); int year =
       * Integer.parseInt(hijiridate.split("-")[2]); log.debug("Year:" + year);
       * OBQuery<MaterialIssueRequest> objIrQry = OBDal.getInstance().createQuery(
       * MaterialIssueRequest.class, "as e where e.organization.id='" +
       * objRequest.getOrganization().getId() + "' order by e.creationDate desc");
       * objIrQry.setMaxResult(1); if (objIrQry.list().size() > 0) { code =
       * objIrQry.list().get(0).getDocno(); if (code != null) { newCode =
       * String.valueOf(year).concat("H").concat(String.format("%04d", code + 1));
       * event.setCurrentState(DocNo, code + 1); } else { newCode = String.valueOf(year).concat("H")
       * .concat(String.format("%04d", Long.valueOf("0001"))); event.setCurrentState(DocNo,
       * Long.valueOf("0001")); } } else { newCode =
       * String.valueOf(year).concat("H").concat(String.format("%04d", code));
       * event.setCurrentState(DocNo, code); }
       */
      // set new Spec No
      sequence = Utility.getTransactionSequence(objRequest.getOrganization().getId(), "MIR");
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        sequenceexists = Utility.chkTransactionSequence(objRequest.getOrganization().getId(),
            "MIR", sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
        event.setCurrentState(SpecRef, sequence);
      }

      if (objRequest.getBeneficiaryType().equals("D")
          || objRequest.getBeneficiaryType().equals("E")
          || objRequest.getBeneficiaryType().equals("S")) {
        if (objRequest.getBeneficiaryIDName() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_BenMandatory"));
        }
      }
      try {
        String preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
            vars.getClient(), vars.getOrg(), vars.getUser(), vars.getRole(),
            "D8BA0A87790B4B67A86A8DF714525736");
        if (preferenceValue.equals("Y")) {
          if ((objRequest.getWarehouse() != null && !objRequest.getWarehouse().equals("") && objRequest
              .getWarehouse().getEscmWarehouseType().equals("RTW"))
              && (objRequest.getEscmIssuereason() == null || objRequest.getEscmIssuereason()
                  .equals(""))) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_IssueReason"));
          }
        }
      } catch (PropertyException e) {
        log.error(" Exception while creating MaterialIssueRequest with warehousekeeper role: " + e);
      }

    } catch (OBException e) {
      log.error(" Exception while creating MaterialIssueRequest : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      MaterialIssueRequest objRequest = (MaterialIssueRequest) event.getTargetInstance();
      final Property status = entities[0].getProperty(MaterialIssueRequest.PROPERTY_ALERTSTATUS);
      final Property benType = entities[0]
          .getProperty(MaterialIssueRequest.PROPERTY_BENEFICIARYTYPE);

      // get role
      if (!event.getCurrentState(status).toString().equals("DR")) {
        UserRoles objUserRole = objRequest.getUpdatedBy().getADUserRolesList().get(0);
        if (objUserRole != null) {
          Role objRole = objUserRole.getRole();
          // check role is warehouse Keeper
          OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(
              Preference.class,
              "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' and e.visibleAtRole.id='"
                  + objRole.getId() + "'");
          if (preQuery.list().size() > 0 && objRequest.getWarehouse() == null) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_WarehouseEmpty"));
          }

        }
      }
      if (event.getCurrentState(benType).toString().equals("D")
          || event.getCurrentState(benType).toString().equals("E")
          || event.getCurrentState(benType).toString().equals("S")) {
        if (objRequest.getBeneficiaryIDName() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_BenMandatory"));
        }
      }
      try {
        String preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
            vars.getClient(), vars.getOrg(), vars.getUser(), vars.getRole(),
            "D8BA0A87790B4B67A86A8DF714525736");
        if (preferenceValue.equals("Y")) {
          if ((objRequest.getWarehouse() != null && !objRequest.getWarehouse().equals("") && objRequest
              .getWarehouse().getEscmWarehouseType().equals("RTW"))
              && (objRequest.getEscmIssuereason() == null || objRequest.getEscmIssuereason()
                  .equals(""))) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_IssueReason"));
          }
        }
      } catch (PropertyException e) {
        log.error(" Exception while updating MaterialIssueRequest with warehousekeeper role: " + e);
      }
    } catch (OBException e) {
      log.error("Exception while updating MaterialIssueRequest: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      MaterialIssueRequest objRequest = (MaterialIssueRequest) event.getTargetInstance();
      OBQuery<MaterialIssueRequestHistory> appQuery = OBDal.getInstance().createQuery(
          MaterialIssueRequestHistory.class,
          "as e where e.escmMaterialRequest.id='" + objRequest.getId()
              + "' order by creationDate desc");
      appQuery.setMaxResult(1);
      if (appQuery.list().size() > 0) {
        MaterialIssueRequestHistory objLastLine = appQuery.list().get(0);
        if (objLastLine.getRequestreqaction().equals("REJ")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Rejected"));
        }
      }
      if (objRequest.getSpecNo() != null && StringUtils.isNotEmpty(objRequest.getSpecNo())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Spec_Generated(Error)"));
      }
      if (objRequest.getAlertStatus().equals("ESCM_IP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_IR_InProgress"));
      }
      if (objRequest.getAlertStatus().equals("ESCM_TR")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Approved"));
      }
      if (objRequest.getEscmMaterialrequestHistList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Submitted"));
      }

    } catch (Exception e) {
      log.error(" Exception while Deleting IssueRequest : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public String convertTohijriDate(String gregDate) {
    String hijriDate = "";
    try {
      SQLQuery gradeQuery = OBDal
          .getInstance()
          .getSession()
          .createSQLQuery(
              "select eut_convert_to_hijri(to_char(to_timestamp('" + gregDate
                  + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD  HH24:MI:SS'))");
      if (gradeQuery.list().size() > 0) {
        Object row = (Object) gradeQuery.list().get(0);
        hijriDate = (String) row;
        log.debug("ConvertedDate:" + (String) row);
      }
    }

    catch (final Exception e) {
      log.error("Exception in convertTohijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }
}
