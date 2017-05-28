package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.scm.ESCMPurchaseReqAppHist;

/**
 * 
 * @author Gopalakrishnan on 15/02/2017
 * 
 */
public class RequisitionEvent extends EntityPersistenceEventObserver {
  /**
   * This Class was responsible for business events in M_Requisition Table
   */
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      Requisition.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Requisition objRequisition = (Requisition) event.getTargetInstance();
      OBQuery<ESCMPurchaseReqAppHist> appQuery = OBDal.getInstance()
          .createQuery(
              ESCMPurchaseReqAppHist.class,
              "as e where e.requisition.id='" + objRequisition.getId()
                  + "' order by creationDate desc");
      appQuery.setMaxResult(1);
      if (appQuery.list().size() > 0) {
        ESCMPurchaseReqAppHist objLastLine = appQuery.list().get(0);
        if (objLastLine.getPurchasereqaction().equals("REJ")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Requistion_Rejected"));
        }
      }

      if (objRequisition.getEscmDocStatus().equals("ESCM_IP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_InProgress"));
      }
      if (objRequisition.getEscmDocStatus().equals("ESCM_AP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_Approved"));
      }
      if (objRequisition.getESCMPurchaseReqAppHistList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_Submitted"));
      }

    } catch (Exception e) {
      log.error(" Exception while Deleting Requisition : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
