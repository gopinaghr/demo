(function () {
  var view;
  var grid;
  var recordId, receiveType = '',
      documentNo, whType = '';
  var tabId, windowId;
  var buttonProps = {
    action: function () {
      var callback, i;

      view = this.view;
      grid = view.viewGrid;
      windowId = view['windowId'];
      tabId = view['tabId'];
      recordId = view.viewGrid.getSelectedRecord().id;
      if (windowId == '184') {
        receiveType = view.viewGrid.getSelectedRecord().escmReceivingtype;
      } else if (windowId == 'D8BA0A87790B4B67A86A8DF714525736') {
        whType = view.viewGrid.getSelectedRecord().warehouse$escmWarehouseType;
      }
      documentNo = view.viewGrid.getSelectedRecord().documentNo;
      if (tabId = '1511F2A65DCD4CD49290C1964D5ED741') {
        documentNo = view.viewGrid.getSelectedRecord().escmSpecno;
      }
      if (windowId == 'D6F05B3A695E4D6BB357E1B6686E3D4D' || windowId == 'E397822E8DAB4FCDACC84F5C27455F8C' || windowId == '26209E1C023B4879BF58993F9BF9AAC9' || windowId == 'D8BA0A87790B4B67A86A8DF714525736') {
        documentNo = view.viewGrid.getSelectedRecord().documentNo;
      }


      openReportDownloadDialog(tabId, windowId);
    },
    buttonType: 'escm_print_pdf',
    prompt: OB.I18N.getLabel('ESCM_PrintReport'),
    updateState: function () {
      var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid,
          selectedRecords = grid.getSelectedRecords();
      if (view.isShowingForm && form.isNew) {
        this.setDisabled(true);
      } else if (view.isEditingGrid && grid.getEditForm().isNew) {
        this.setDisabled(true);
      } else {
        this.setDisabled(selectedRecords.length === 0);
      }
    }
  };

  function openReportDownloadDialog(tabId, windowId) {
    var action = "",wi;
    if (windowId == '184') //PO Receipt
    wi = OB.Layout.ClassicOBCompatibility.Popup.open('MaterialMgmtShipmentInOut', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&receiveType=" + receiveType + "&documentNo=" + documentNo, '', null, false, false, true);
    else if (windowId == 'D8BA0A87790B4B67A86A8DF714525736') //Material Issue Request
    wi = OB.Layout.ClassicOBCompatibility.Popup.open('Escm_Material_Request', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&warehouseType="+ whType +"&documentNo=" + documentNo, '', null, false, false, true);
    else if (windowId == 'E397822E8DAB4FCDACC84F5C27455F8C' || windowId == '26209E1C023B4879BF58993F9BF9AAC9' || windowId == 'D6F05B3A695E4D6BB357E1B6686E3D4D') //Return Transaction Window , return items and Custody Transfer
    wi = OB.Layout.ClassicOBCompatibility.Popup.open('MaterialMgmtShipmentInOut', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=" + documentNo, '', null, false, false, true);
    else if (windowId == '8FC04D21ED7540F2B6A4ADCE9BDD58A6') //inventory counting
    wi = OB.Layout.ClassicOBCompatibility.Popup.open('MaterialMgmtInventoryCount', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=" + documentNo, '', null, false, false, true);
    if (receiveType === 'IR' || receiveType === 'INS' || whType === 'RTW' || windowId == 'E397822E8DAB4FCDACC84F5C27455F8C' || windowId == 'D6F05B3A695E4D6BB357E1B6686E3D4D' || windowId == '26209E1C023B4879BF58993F9BF9AAC9' || windowId == '8FC04D21ED7540F2B6A4ADCE9BDD58A6') 
    	wi.close();
  }
  // register the print button for the PO Receipt tab, Material Issue Request and Return transaction Tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 160, ['296', 'CE947EDC9B174248883292F17F03BB32', '72A6B3CA5BE848ACA976304375A5B7A6', '922927563BFC48098D17E4DC85DD504C', 'CB9A2A4C6DB24FD19D542A78B07ED6C1', '1511F2A65DCD4CD49290C1964D5ED741']);
}());