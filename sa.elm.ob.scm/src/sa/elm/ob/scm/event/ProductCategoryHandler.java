package sa.elm.ob.scm.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
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
public class ProductCategoryHandler extends EntityPersistenceEventObserver {
  /**
   * This Class is responsible for business events in M_Product_Category Table
   */
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ProductCategory.ENTITY_NAME) };

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
      ProductCategory objCategory = (ProductCategory) event.getTargetInstance();
      OBQuery<Product> appQuery = OBDal.getInstance().createQuery(
          Product.class,
          "as e where e.productCategory.id='" + objCategory.getId()
              + "' order by creationDate desc");
      appQuery.setMaxResult(1);
      if (appQuery.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Product_Category(Delete)"));
      }

    } catch (Exception e) {
      log.error(" Exception while Deleting Product Category : " + e);
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
      ProductCategory objCategory = (ProductCategory) event.getTargetInstance();
      final Property name = entities[0].getProperty(ProductCategory.PROPERTY_SEARCHKEY);
      final Property isactive = entities[0].getProperty(ProductCategory.PROPERTY_ACTIVE);
      final Property isdefault = entities[0].getProperty(ProductCategory.PROPERTY_DEFAULT);
      final Property obMasterCategory = entities[0]
          .getProperty(ProductCategory.PROPERTY_ESCMPRODUCTCATEGORY);
      // removed task no 4113 note 1258 point 3
      /*
       * if (event.getCurrentState(isactive).equals(event.getPreviousState(isactive)) &&
       * event.getCurrentState(isdefault).equals(event.getPreviousState(isdefault))) {
       * OBQuery<Product> appQuery = OBDal.getInstance().createQuery( Product.class,
       * "as e where e.productCategory.id='" + objCategory.getId() +
       * "' order by creationDate desc"); appQuery.setMaxResult(1); if (appQuery.list().size() > 0)
       * { throw new OBException(OBMessageUtils.messageBD("ESCM_Product_Category(Delete)")); } }
       */

      // check leaf code Code exists
      if (!objCategory.isSummaryLevel()) {
        if (objCategory.getEscmProductCategory() != null) {
          OBQuery<ProductCategory> productSubQuery = OBDal.getInstance().createQuery(
              ProductCategory.class,
              "as e where e.name='" + objCategory.getName().toString() + "' and e.client.id='"
                  + objCategory.getClient().getId() + "' and e.escmProductCategory.id='"
                  + objCategory.getEscmProductCategory().getId()
                  + "' and e.summaryLevel='N' and e.id <>'" + objCategory.getId() + "'");

          if (productSubQuery.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Category_Leaf(Exists)"));
          }
        }
      }
      // check Summary Level 'N' then
      // Master Category Mandatory
      if (!objCategory.isSummaryLevel()) {
        if (objCategory.getEscmProductCategory() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Master_Category(Empty)"));
        }
      } else {
        event.setCurrentState(obMasterCategory, null);
      }
      // check default checked more than once
      if (objCategory.isDefault()) {
        OBQuery<ProductCategory> productChkQuery = OBDal.getInstance().createQuery(
            ProductCategory.class,
            " as e where e.default='Y' and e.organization.id='"
                + objCategory.getOrganization().getId() + "'");
        if (productChkQuery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Product_Category(Default)"));
        }
      }
      // Name(value) should be unique
      if (!event.getPreviousState(name).equals(event.getCurrentState(name))) {
        OBQuery<ProductCategory> productChkName = OBDal.getInstance().createQuery(
            ProductCategory.class,
            " as e where e.searchKey='" + objCategory.getSearchKey() + "' and e.client.id='"
                + objCategory.getClient().getId() + "'");
        if (productChkName.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_product_category_value_unique"));
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating  Product Category:" + e);
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
      String code = "";
      ProductCategory objCategory = (ProductCategory) event.getTargetInstance();
      final Property objSummary = entities[0].getProperty(ProductCategory.PROPERTY_SUMMARYLEVEL);
      final Property obMasterCategory = entities[0]
          .getProperty(ProductCategory.PROPERTY_ESCMPRODUCTCATEGORY);
      code = objCategory.getName().toString();
      // get number count
      int count = 0;
      boolean isallow = false;
      for (int i = 0, len = code.length(); i < len; i++) {
        if (Character.isDigit(code.charAt(i))) {
          count++;
        }
      }
      // Code should allow only numeric values
      isallow = code.matches("-?\\d+(\\.\\d+)?"); // it will check only numeric values
      if (!isallow) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Product_Category_Code_Format"));
      }
      if (new BigDecimal(code).compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Product_Category_Code_Format"));
      }

      if (count > 2 && code.length() > 2) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Product_Category_Code_Format"));
      }
      // check SummaryLevel Code exists
      if (event.getCurrentState(objSummary).toString().equals("true")) {
        OBQuery<ProductCategory> productQuery = OBDal.getInstance().createQuery(
            ProductCategory.class,
            "as e where e.name='" + objCategory.getName().toString() + "' and e.client.id='"
                + objCategory.getClient().getId() + "' and e.summaryLevel='Y'");
        if (productQuery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Master_Category(Exists)"));
        }
      }

      // check leaf code Code exists
      if (objCategory.getEscmProductCategory() != null) {
        OBQuery<ProductCategory> productSubQuery = OBDal.getInstance().createQuery(
            ProductCategory.class,
            "as e where e.name='" + objCategory.getName().toString() + "' and e.client.id='"
                + objCategory.getClient().getId() + "' and e.escmProductCategory.id='"
                + objCategory.getEscmProductCategory().getId() + "' and e.summaryLevel='N'");
        if (productSubQuery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Category_Leaf(Exists)"));
        }
      }
      // check Summary Level 'N' then
      // Master Category Mandatory
      if (!objCategory.isSummaryLevel()) {
        if (objCategory.getEscmProductCategory() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Master_Category(Empty)"));
        }
      } else {
        event.setCurrentState(obMasterCategory, null);
      }
      // check default checked more than once
      if (objCategory.isDefault()) {
        OBQuery<ProductCategory> productChkQuery = OBDal.getInstance().createQuery(
            ProductCategory.class,
            " as e where e.default='Y' and e.organization.id='"
                + objCategory.getOrganization().getId() + "'");
        if (productChkQuery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Product_Category(Default)"));
        }
      }

      // Name(value) should be unique
      OBQuery<ProductCategory> productChkName = OBDal.getInstance().createQuery(
          ProductCategory.class,
          " as e where e.searchKey='" + objCategory.getSearchKey() + "' and e.client.id='"
              + objCategory.getClient().getId() + "'");
      if (productChkName.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_product_category_value_unique"));
      }

    } catch (OBException e) {
      log.error(" Exception while creating Product Category: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
