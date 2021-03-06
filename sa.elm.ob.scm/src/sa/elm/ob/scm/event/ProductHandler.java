package sa.elm.ob.scm.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;

/**
 * 
 * @author Gopalakrishnan on 01/03/2017
 * 
 */
public class ProductHandler extends EntityPersistenceEventObserver {
  /**
   * This Class is responsible for business events in M_Product Table
   */
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Product.ENTITY_NAME) };

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
      Product objProduct = (Product) event.getTargetInstance();
      OBQuery<Product> appQuery = OBDal.getInstance().createQuery(
          Product.class,
          "as e where e.productCategory.id='" + objProduct.getProductCategory().getId()
              + "' order by creationDate desc");
      appQuery.setMaxResult(1);
      if (appQuery.list().size() > 0) {
        Product objLastProduct = appQuery.list().get(0);
        if (!objLastProduct.getId().equals(objProduct.getId())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Product_Delete(NotLatest)"));
        }
      }

    } catch (Exception e) {
      log.error(" Exception while Deleting Product  : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Long code = Long.valueOf("0001");
      String newCode = "";
      Product objProduct = (Product) event.getTargetInstance();
      final Property objSearchKey = entities[0].getProperty(Product.PROPERTY_SEARCHKEY);
      final Property objDocNo = entities[0].getProperty(Product.PROPERTY_ESCMDOCNO);
      ProductCategory objLeaf = objProduct.getProductCategory();
      ProductCategory objElement = objProduct.getProductCategory().getEscmProductCategory();
      // get Existing code
      OBQuery<Product> objProductQuery = OBDal.getInstance().createQuery(Product.class,
          "as e where e.productCategory.id='" + objLeaf.getId() + "' order by e.creationDate desc");
      objProductQuery.setMaxResult(1);
      if (objProductQuery.list().size() > 0) {
        code = objProductQuery.list().get(0).getEscmDocno();
        if (code != null) {
          newCode = objElement.getName().concat(objLeaf.getName())
              .concat(String.format("%04d", code + 1));
          event.setCurrentState(objDocNo, code + 1);
        } else {
          newCode = objElement.getName().concat(objLeaf.getName())
              .concat(String.format("%04d", Long.valueOf("0001")));
          event.setCurrentState(objDocNo, Long.valueOf("0001"));
        }

      } else {
        newCode = objElement.getName().concat(objLeaf.getName())
            .concat(String.format("%04d", code));
        event.setCurrentState(objDocNo, code);
      }
      // set new search key
      event.setCurrentState(objSearchKey, newCode);
    } catch (OBException e) {
      log.error(" Exception while creating Product : " + e);
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
      String strQuery = "";
      Query query = null;
      BigDecimal qtyonhand = BigDecimal.ZERO;
      Product objProduct = (Product) event.getTargetInstance();
      final Property custatt = entities[0].getProperty(Product.PROPERTY_ESCMCUSATTRIBUTE);

      if ((event.getCurrentState(custatt) != null && (!event.getCurrentState(custatt).equals(
          event.getPreviousState(custatt))))) {
        strQuery = "    SELECT      COALESCE(sum(QtyOnHand),0) as   QtyOnHand  FROM (SELECT QtyOnHand "
            + "    FROM M_Storage_Detail s    WHERE s.M_Product_ID= ?  "
            + "    UNION    SELECT 0 AS QtyOnHand  FROM M_Storage_Pending s "
            + "   WHERE s.M_Product_ID= ?) a";
        query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
        query.setParameter(0, objProduct.getId());
        query.setParameter(1, objProduct.getId());
        log.debug("strQuery:" + query);
        if (query != null && query.list().size() > 0) {
          qtyonhand = (BigDecimal) query.list().get(0);
          if (qtyonhand.compareTo(BigDecimal.ZERO) < 0 || qtyonhand.compareTo(BigDecimal.ZERO) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_PrdDontchgCustAtt"));
          }
        }
      }

    } catch (Exception e) {
      log.error(" Exception while Deleting Product  : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
