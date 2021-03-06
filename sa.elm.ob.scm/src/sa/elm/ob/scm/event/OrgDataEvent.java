package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.Escmproductquantity;

public class OrgDataEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      Escmproductquantity.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  // Organization should unique for one product
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      Escmproductquantity orgdata = (Escmproductquantity) event.getTargetInstance();
      OBQuery<Escmproductquantity> listquery = OBDal.getInstance().createQuery(
          Escmproductquantity.class,
          " organization.id='" + orgdata.getOrganization().getId() + "' and client.id='"
              + orgdata.getClient().getId() + "' and product.id='" + orgdata.getProduct().getId()
              + "'");

      if (listquery.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Org_Unique"));
      }

    } catch (OBException e) {
      log.debug("exception while creating orgdata" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
