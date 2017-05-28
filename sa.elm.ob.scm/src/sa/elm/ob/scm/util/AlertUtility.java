package sa.elm.ob.scm.util;

/**
 * @auther gopalakrishnan on 21/02/2017
 * 
 *         Utility Class file for Requisition Alert Process
 */

public class AlertUtility {

  /**
   * This method only for Purchase Requisition Alert Process With Preference Configuration
   * 
   * @param property
   *          preference
   * @param clientId
   * @param description
   * @param status
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionPreference(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status, String Window) {
    return AlertUtilityDAO.alertInsertionPreference(DocumentId, DocumentNo, property, clientId,
        description, status, Window);
  }

  /**
   * This method only for Purchase Requisition Alert Process
   * 
   * @param property
   *          preference
   * @param clientId
   * @param description
   * @param status
   *          NEW-new Alert,SOLVED-alert Solved
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status, String Window) {
    return AlertUtilityDAO.alertInsertionRole(DocumentId, DocumentNo, roleId, userId, clientId,
        description, status, Window);
  }

  /**
   * 
   * @param roleId
   * @param clientId
   * @return True --Alert recipient Created, False --Error
   */
  public static Boolean insertAlertRecipient(String roleId, String userId, String clientId,
      String Window) {
    return AlertUtilityDAO.insertAlertRecipient(roleId, userId, clientId, Window);
  }
}
