package sa.elm.ob.scm.ad_process.printreport;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.MaterialIssueRequest;

public class PrintReport extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  String pageType = "";

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = null;
    RequestDispatcher dispatch = null;
    Connection con = null;

    Logger log4j = Logger.getLogger(PrintReport.class);
    try {
      con = getConnection();
      vars = new VariablesSecureApp(request);
      ServletOutputStream os = null;
      InputStream fin = null;
      String action = (request.getParameter("action") == null ? "" : request.getParameter("action"));
      HashMap<String, Object> designParameters = new HashMap<String, Object>();
      final String basedesign = getBaseDesignPath(vars.getLanguage());
      final String reportDir = basedesign + "/sa/elm/ob/scm/ad_reports/";
      designParameters.put("BASE_DESIGN", basedesign);
      String strReportName = "";
      String strFileName = "", imageFlag = "N";
      String reportGeneratedDate = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
      String hijriDate = sa.elm.ob.utility.util.Utility.convertTohijriDate(reportGeneratedDate);

      if (action.equals("DownloadReport")) {
        try {
          String inpReceivingType = request.getParameter("inpReceivingType");
          String paramType = request.getParameter("report");
          String inpDocNo = request.getParameter("inpDocNo");
          log4j.debug(inpReceivingType + "//" + paramType + "//"
              + request.getParameter("inpRecordId"));
          if (paramType.equals("InventoryDelivery") && inpReceivingType.equals("DEL")) {
            // find organisation image
            ShipmentInOut objInout = OBDal.getInstance().get(ShipmentInOut.class,
                request.getParameter("inpRecordId"));
            OrganizationInformation objInfo = objInout.getOrganization()
                .getOrganizationInformationList().get(0);
            // check org have image
            if (objInfo != null) {
              if (objInfo.getYourCompanyDocumentImage() != null) {
                imageFlag = "Y";
              }
            }
            designParameters.put("inpImageFlag", imageFlag);
            designParameters.put("inpOrgId", objInout.getOrganization().getId());
            designParameters.put("inpInOutId", request.getParameter("inpRecordId"));
            String inpOutputType = request.getParameter("inpOutputType");
            if (inpOutputType.equals("A4")) {
              strReportName = reportDir + "InventoryDeliverReport/InventoryDeliverUsingA4.jrxml";
              strFileName = "Inventory Delivery" + " " + inpDocNo + " " + hijriDate;
            } else if (inpOutputType.equals("DM")) {
              strReportName = reportDir
                  + "InventoryDeliverReport/InventoryDeliverUsingDotMatrix.jrxml";
              strFileName = "Inventory Delivery" + " " + inpDocNo + " " + hijriDate;
            }
          } else if (paramType.equals("SiteDelivery") && inpReceivingType.equals("SR")) {
            // find organisation image
            ShipmentInOut objInout = OBDal.getInstance().get(ShipmentInOut.class,
                request.getParameter("inpRecordId"));
            OrganizationInformation objInfo = objInout.getOrganization()
                .getOrganizationInformationList().get(0);
            // check org have image
            if (objInfo != null) {
              if (objInfo.getYourCompanyDocumentImage() != null) {
                imageFlag = "Y";
              }
            }
            designParameters.put("inpImageFlag", imageFlag);
            designParameters.put("inpOrgId", objInout.getOrganization().getId());
            designParameters.put("inpInOutId", request.getParameter("inpRecordId"));
            String inpOutputType = request.getParameter("inpOutputType");
            if (inpOutputType.equals("A4")) {
              strReportName = reportDir + "SiteDeliverNoteReport/SiteDeliverNote.jrxml";
              strFileName = "Site Receipt" + " " + inpDocNo + " " + hijriDate;
            } else if (inpOutputType.equals("DM")) {
              strReportName = reportDir + "SiteDeliverNoteReport/SiteDeliverNoteDotMatrix.jrxml";
              strFileName = "Site Receipt" + " " + inpDocNo + " " + hijriDate;
            }
          } else if (paramType.equals("MaterialReq")) {
            // find organisation image
            MaterialIssueRequest objMir = OBDal.getInstance().get(MaterialIssueRequest.class,
                request.getParameter("inpRecordId"));
            OrganizationInformation objInfo = objMir.getOrganization()
                .getOrganizationInformationList().get(0);
            // check org have image
            if (objInfo != null) {
              if (objInfo.getYourCompanyDocumentImage() != null) {
                imageFlag = "Y";
              }
            }
            designParameters.put("inpImageFlag", imageFlag);
            designParameters.put("inpOrgId", objMir.getOrganization().getId());
            designParameters.put("inpMaterialReqId", request.getParameter("inpRecordId"));
            String inpOutputType = request.getParameter("inpOutputType");
            if (inpOutputType.equals("A4")) {
              if (objMir.getWarehouse().getEscmWarehouseType().equals("MAW")) {
                strReportName = reportDir + "materialissuerequest/MaterialIssueRequestA4.jrxml";
                strFileName = "Material Req" + " " + inpDocNo + " " + hijriDate;
              } else if (objMir.getWarehouse().getEscmWarehouseType().equals("RTW")) {
                strReportName = reportDir
                    + "MIRIssueReturnTransaction/MIRIssueReturnTransactionReport.jrxml";
                strFileName = "Material Req" + " " + inpDocNo + " " + hijriDate;
              }
            } else if (inpOutputType.equals("DM")) {
              strReportName = reportDir
                  + "materialissuerequest/MaterialIssueRequestDotMatrix.jrxml";
              strFileName = "Material Req" + " " + inpDocNo + " " + hijriDate;
            }
          }
          fin = new FileInputStream(strReportName);
          JasperDesign jasperDesign = JRXmlLoader.load(fin);
          JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
          JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, designParameters,
              con);

          os = response.getOutputStream();
          response.setContentType("application/octet-stream");
          response.setHeader("Content-disposition", "inline" + "; filename=" + strFileName + ".pdf"
              + "");

          JasperExportManager.exportReportToPdfStream(jasperPrint, os);
        } catch (Exception e) {
          log4j.error("Exception while downloading : ", e);
        }

        finally {
          try {
            if (os != null) {
              os.flush();
              os.close();
            }
          } catch (Exception e) {
            log4j.error("Exception while downloading print report : ", e);
          }
        }
        return;
      } else if (action.equals("Close")) {
        vars = new VariablesSecureApp(request);
        if (request.getParameter("pageType") != null
            && request.getParameter("pageType").equals("Grid"))
          printPageClosePopUp(response, vars);
        else
          printPageClosePopUpAndRefreshParent(response, vars);
      } else if (action.equals("")) {
        String tabId = request.getParameter("inpTabId");
        String inpWindowID = (request.getParameter("inpWindowID") == null ? "" : request
            .getParameter("inpWindowID"));
        String inpRecordId = (request.getParameter("inpRecordId") == null ? "" : request
            .getParameter("inpRecordId"));
        pageType = request.getParameter("pageType") == null ? "" : request.getParameter("pageType");
        String receiveType = (request.getParameter("receiveType") == null ? "" : request
            .getParameter("receiveType"));
        String warehouseType = (request.getParameter("warehouseType") == null ? "" : request
            .getParameter("warehouseType"));
        String documentNo = (request.getParameter("documentNo") == null ? "" : request
            .getParameter("documentNo"));
        // PO Receipt Window Id - 184
        // Material Issue Req Id - D8BA0A87790B4B67A86A8DF714525736
        if ((inpWindowID.equals("184") && (receiveType.equals("IR") || receiveType.equals("INS")))
            || inpWindowID.equals("26209E1C023B4879BF58993F9BF9AAC9")
            || inpWindowID.equals("E397822E8DAB4FCDACC84F5C27455F8C")
            || inpWindowID.equals("D6F05B3A695E4D6BB357E1B6686E3D4D")
            || (inpWindowID.equals("D8BA0A87790B4B67A86A8DF714525736") && (warehouseType
                .equals("RTW")))) {
          if (inpWindowID.equals("D8BA0A87790B4B67A86A8DF714525736")) {
            MaterialIssueRequest objInout = OBDal.getInstance().get(MaterialIssueRequest.class,
                request.getParameter("inpRecordId"));
            OrganizationInformation objInfo = objInout.getOrganization()
                .getOrganizationInformationList().get(0);
            // check org have image
            if (objInfo != null) {
              if (objInfo.getYourCompanyDocumentImage() != null) {
                imageFlag = "Y";
              }
            }
            designParameters.put("inpImageFlag", imageFlag);
            designParameters.put("inpOrgId", objInout.getOrganization().getId());
            designParameters.put("inpMaterialReqId", request.getParameter("inpRecordId"));

            strReportName = reportDir
                + "MIRIssueReturnTransaction/MIRIssueReturnTransactionReport.jrxml";
            strFileName = "Material Req" + " " + documentNo + " " + hijriDate;

            fin = new FileInputStream(strReportName);
            JasperDesign jasperDesign = JRXmlLoader.load(fin);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, designParameters,
                con);

            os = response.getOutputStream();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "inline" + "; filename=" + strFileName
                + ".pdf" + "");

            JasperExportManager.exportReportToPdfStream(jasperPrint, os);

          } else {
            ShipmentInOut objInout = OBDal.getInstance().get(ShipmentInOut.class,
                request.getParameter("inpRecordId"));
            OrganizationInformation objInfo = objInout.getOrganization()
                .getOrganizationInformationList().get(0);
            // check org have image
            if (objInfo != null) {
              if (objInfo.getYourCompanyDocumentImage() != null) {
                imageFlag = "Y";
              }
            }

            /*
             * // check org have image if not then client's image String inpOrgid =
             * objInout.getOrganization().getId(); String imgOrg = ""; if (objInfo != null) { if
             * (objInfo.getYourCompanyDocumentImage() != null) { imgOrg = inpOrgid; } else { imgOrg
             * = ""; } }
             */
            // designParameters.put("imgOrg", imgOrg);
            designParameters.put("inpImageFlag", imageFlag);
            designParameters.put("inpOrgId", objInout.getOrganization().getId());
            designParameters.put("inpInOutId", request.getParameter("inpRecordId"));

            if (receiveType.equals("IR")) {
              strReportName = reportDir + "initialreceiving/InitialReceiving.jrxml";
              strFileName = "Initial Receipt" + " " + documentNo + " " + hijriDate;
            } else if (receiveType.equals("INS")) {
              strReportName = reportDir + "InspectionReport/Inspection.jrxml";
              strFileName = "Inspection" + " " + documentNo + " " + hijriDate;
            } else if (inpWindowID.equals("26209E1C023B4879BF58993F9BF9AAC9")) {
              strReportName = reportDir + "returnItemsReport/returnItemsReport.jrxml";
              strFileName = "IssueReturnTransaction" + " " + documentNo + " " + hijriDate;
            } else if (inpWindowID.equals("E397822E8DAB4FCDACC84F5C27455F8C")) {
              strReportName = reportDir + "returntransaction/ReturnTransaction.jrxml";
              strFileName = "Return Transaction" + " " + documentNo + " " + hijriDate;
            } else if (inpWindowID.equals("D6F05B3A695E4D6BB357E1B6686E3D4D")) {
              strReportName = reportDir + "CustodyReport/custodyreport.jrxml";
              strFileName = "Custody Transfer" + " " + documentNo + " " + hijriDate;
            }

            fin = new FileInputStream(strReportName);
            JasperDesign jasperDesign = JRXmlLoader.load(fin);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, designParameters,
                con);

            os = response.getOutputStream();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "inline" + "; filename=" + strFileName
                + ".pdf" + "");

            JasperExportManager.exportReportToPdfStream(jasperPrint, os);
          }
        }
        // inventory counting
        else if (inpWindowID.equals("8FC04D21ED7540F2B6A4ADCE9BDD58A6")) {
          InventoryCount invcount = OBDal.getInstance().get(InventoryCount.class,
              request.getParameter("inpRecordId"));
          log4j.debug(request.getParameter("inpRecordId"));

          OrganizationInformation objInfo = invcount.getOrganization()
              .getOrganizationInformationList().get(0);
          // check org have image
          if (objInfo != null) {
            if (objInfo.getYourCompanyDocumentImage() != null) {
              imageFlag = "Y";
            }
          }
          designParameters.put("inpImageFlag", imageFlag);
          designParameters.put("inpOrgId", invcount.getOrganization().getId());
          designParameters.put("inpInventoryCountId", request.getParameter("inpRecordId"));
          if (invcount.getEscmStatus().equals("DR")) {
            strReportName = reportDir + "CountingTicketlistReport/CountingTicketlistReport.jrxml";
            strFileName = "Counting Ticket list" + " " + documentNo + " " + hijriDate;
          } else if (invcount.getEscmStatus().equals("CO")) {
            strReportName = reportDir + "InventoryCountingReport/InventoryCountingReport.jrxml";
            strFileName = "Inventory Counting" + " " + documentNo + " " + hijriDate;
          }
          fin = new FileInputStream(strReportName);
          JasperDesign jasperDesign = JRXmlLoader.load(fin);
          JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
          JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, designParameters,
              con);

          os = response.getOutputStream();
          response.setContentType("application/octet-stream");
          response.setHeader("Content-disposition", "inline" + "; filename=" + strFileName + ".pdf"
              + "");

          JasperExportManager.exportReportToPdfStream(jasperPrint, os);

        } else {
          request.setAttribute("pageType", pageType);
          request.setAttribute("inpTabId", tabId);
          request.setAttribute("inpWindowID", inpWindowID);
          request.setAttribute("inpRecordId", inpRecordId);
          request.setAttribute("inpReceivingType", receiveType);
          request.setAttribute("inpDocNo", documentNo);
          dispatch = request
              .getRequestDispatcher("../web/sa.elm.ob.scm/jsp/printreport/PrintReport.jsp");

        }
      }
    } catch (final Exception e) {
      dispatch = request.getRequestDispatcher("../web/jsp/ErrorPage.jsp");
      log4j.error("Error file", e);
      e.printStackTrace();
      vars = new VariablesSecureApp(request);
      if (request.getParameter("pageType") != null
          && request.getParameter("pageType").equals("Grid"))
        printPageClosePopUp(response, vars);
      else
        printPageClosePopUpAndRefreshParent(response, vars);
    } finally {
      try {
        con.close();
        if (dispatch != null) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
        }
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Error file", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }
}