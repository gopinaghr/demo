package sa.elm.ob.scm.charts.OnhandStatus;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

public class OnhandStatusAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    VariablesSecureApp vars = null;
    OnhandStatusDao dao = null;
    String action = "";
    if (log4j.isDebugEnabled())
      log4j.debug(" OnhandStatusAjax ");
    try {
      dao = new OnhandStatusDao(getConnection());
      vars = new VariablesSecureApp(request);
      action = vars.getStringParameter("action");

    } catch (Exception e) {
      log4j.error("Exception in OnhandStatusAjax", e);
    }
  }
}