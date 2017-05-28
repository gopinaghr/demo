package sa.elm.ob.scm.ad_forms.ApprovalRevoke.header;

import java.io.IOException;
import java.sql.Connection;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

import sa.elm.ob.scm.ad_forms.ApprovalRevoke.dao.ApprovalRevokeDAO;
import sa.elm.ob.scm.ad_forms.ApprovalRevoke.vo.ApprovalRevokeVO;

/**
 * 
 * @author gopalakrishnan on 04/05/2017
 */

public class ApprovalRevoke extends HttpSecureAppServlet {

  /**
   * Employment form details
   */
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unused")
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    ApprovalRevokeDAO dao = null;
    Connection con = null;
    // EmploymentVO employmentVO = null;
    RequestDispatcher dispatch = null;
    VariablesSecureApp vars = null;
    ApprovalRevokeVO vo = null;

    try {
      OBContext.setAdminMode();
      String action = (request.getParameter("inpAction") == null ? "" : request
          .getParameter("inpAction"));
      String inpWindowId = (request.getParameter("inpWindowId") == null ? "MIR" : request
          .getParameter("inpWindowId"));
      con = getConnection();
      vars = new VariablesSecureApp(request);
      dao = new ApprovalRevokeDAO(con);

      if (action.equals("")) {
        request.setAttribute("inpWindowId", inpWindowId);
        request.setAttribute("SaveStatus", "");
        dispatch = request
            .getRequestDispatcher("../web/sa.elm.ob.scm/jsp/ApprovalRevoke/ApprovalRevoke.jsp");
      } else if (action.equals("Revoke")) {
        String selectIds = request.getParameter("selectIds");
        inpWindowId = request.getParameter("inpWindowId");
        request.setAttribute("inpWindowId", inpWindowId);
        String processedIds = dao.validateRecord(selectIds);
        if (processedIds != null) {
          request.setAttribute("SaveStatus", "AlreadyProcessed");
          request.setAttribute("DocumentNo", processedIds);
        } else {
          String success = dao.updateRevoke(vars, selectIds, inpWindowId);
          request.setAttribute("SaveStatus", success);
        }

        dispatch = request
            .getRequestDispatcher("../web/sa.elm.ob.scm/jsp/ApprovalRevoke/ApprovalRevoke.jsp");
      }
    } catch (final Exception e) {
      dispatch = request.getRequestDispatcher("../web/jsp/ErrorPage.jsp");
      log4j.error("Error in ApprovalRevoke : ", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        con.close();
        if (dispatch != null) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
        } else
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Error in ApprovalRevoke : ", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    doPost(request, response);
  }
}
