OB.ESCM = {};
OB.ESCM.QtyValidation	 = {};

OB.ESCM.QtyValidation.delqtymismatch = function (item, validator, value, record) { 
	if(value < 0){
		 isc.warn(OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
		    return false;
	}
	var acceptedQty = record.acceptedQuantity,deliveredQty=record.deliveredqty,quantity=record.quantity;
			 //console.log(new BigDecimal(String(record.acceptedQuantity)));
			 console.log(acceptedQty-deliveredQty);
			 if ((acceptedQty-deliveredQty-quantity) < 0) {
				 isc.warn(OB.I18N.getLabel('ESCM_POFinalRec_GrtActQty'));
				    return false; 
			 }
			 else
				 return true;
			 
};
OB.ESCM.QtyValidation.insqtymismatch = function (item, validator, value, record) { 
	var jsonInspList = {};
	jsonInspList.List = [];
	jsonInspList.List.length=0;
	var  selectedRecords = item.grid.getSelectedRecords(),
    selectedRecordsLength = selectedRecords.length, status="",tempescminitialreceipt="", errorflag=true;
	//console.log(record);
	
	//chk qty validation not exceeds remaining inspected qty
	var remaInspQty = (record.irqty-record.inspectedQty),quantity=record.quantity;
	 if(value < 0){
		 isc.warn(OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
		 item.grid.setEditValue(item.grid.getRecordIndex(record), 'quantity', 0);
		 errorflag=true;
	}
	 if ((remaInspQty-quantity) < 0) {
		 var msg= OB.I18N.getLabel('ESCM_POIns_GrtAvlInsQty');
		 msg= msg.replace('%',remaInspQty );
		 isc.warn(msg);
		   // item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, msg);
		 item.grid.setEditValue(item.grid.getRecordIndex(record), 'quantity', 0);
		 errorflag=true;
	 }
	
	if(errorflag){
	// accept and rejected qty validation
	  if(selectedRecordsLength > 0){
		  var data = jsonInspList.List,canAdd=true;
			jsonInspList.List.length=0;
			data.length=0;
			for (i = 0; i < selectedRecordsLength; i++) {
			    editedRecord = isc.addProperties({}, selectedRecords[i], item.grid.getEditedRecord(selectedRecords[i]));
			    var initialirqty= editedRecord.irqty,inspectedQty = editedRecord.inspectedQty , escminitialreceipt= editedRecord.escmInitialreceipt ;
			    var remInspectedQty=initialirqty-inspectedQty; var qty=editedRecord.quantity,id=editedRecord.id,status=editedRecord.alertStatus;
			    for ( var j in data) {
					if ( data[j].id == escminitialreceipt) {
						if(status=='Accept'){
							jsonInspList.List[len]['acceptid'] = id;
							jsonInspList.List[len]['acceptQty'] = qty;
						}
						else{
							jsonInspList.List[len]['rejectedid'] = id;
							jsonInspList.List[len]['rejectedQty'] = qty;
						}
						data[j].qty = data[j].qty + qty;
						canAdd = false;
						break;
					}
					else
						canAdd = true;
				}
			    if (canAdd) {
					var len = jsonInspList.List.length;
					jsonInspList.List[len] = {};
					jsonInspList.List[len]['id'] = escminitialreceipt;
					if(status=='Accept'){
						jsonInspList.List[len]['acceptid'] = id;
						jsonInspList.List[len]['acceptQty'] = qty;
					}
					else{
						jsonInspList.List[len]['rejectedid'] = id;
						jsonInspList.List[len]['rejectedQty'] = qty;
					}
						
					jsonInspList.List[len]['qty'] = qty;
					jsonInspList.List[len]['remInspectedQty'] = remInspectedQty;
			    }
			  }
	  }
			for ( var k in data) {
				 if(data[k].remInspectedQty-data[k].qty < 0 || (data[k].qty < 0) ){
					 if(data[k].acceptid !=null &&  data[k].rejectedid  !=null ){
						 if(data[k].acceptid != record.id){
							 item.grid.setEditValue(item.grid.getRecordIndex(item.grid.data.localData.find('id', data[k].acceptid)), 'quantity', (data[k].remInspectedQty-data[k].rejectedQty));
							 return  true;
						 }
							 else if(data[k].rejectedid != record.id){
								 item.grid.setEditValue(item.grid.getRecordIndex(item.grid.data.localData.find('id', data[k].rejectedid)), 'quantity', (data[k].remInspectedQty-data[k].acceptQty));
								 return  true;
							 }
					 }
					 else{
						 item.grid.setEditValue(item.grid.getRecordIndex(record), 'quantity', 0);
					 }
				 }
	  }
	}

	  return  true;

};  
OB.ESCM.QtyValidation.retqtymismatch = function (item, validator, value, record) { 
	
	var reserved = record.reservedQuantity,quantity=record.newquantity;
			 if ((reserved-quantity) < 0) {
				 isc.warn(OB.I18N.getLabel('ESCM_PORecGrtRetAvaQty'));
				    return false; 
			 }
			 else
				 return true;
			 
};

OB.ESCM.QtyValidation.siteissuereqqtymismatch = function (item, validator, value, record) { 
	
	if(value < 0){
		 isc.warn(OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
		    return false;
	}
	var reserved = record.reservedQuantity,quantity=record.reqqty,issuedQty= record.issuedQty,inprgqty=record.inprgqty;
			 if ((reserved-issuedQty-inprgqty) < quantity ) {
				 isc.warn(OB.I18N.getLabel('ESCM_SMIR_QtyGretthanRecQty'));
				    return false; 
			 }
			 
	var fractional =  (quantity - Math.floor(quantity));
	if(fractional < 1 && fractional!=0){
		 isc.warn(OB.I18N.getLabel('ESCM_Fractional(Custody)'));
		    return false; 
	}
	else
		 return true;
			 
};

OB.ESCM.QtyValidation.createprqtyval= function (item, validator, value, record) { 
	var recordId = record.id,pendingqty = record.pendingQty;
	if(value <= 0 || value == null){
		 item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
		    return false;
	}
	if(recordId !=null){
		 var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.MaterialIssuePendQtyHandler', {
	         recordId: recordId,
	       }, {}, function (response, data, request) {

	         if(value > data.pendingQty){
	      		 item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_SMIR_QtyGretthanRecQty'));
	      		item.grid.setEditValue(item.grid.getRecordIndex(record), 'pendingQty', 0); 
			 }
	       });
	}
	
	return true;
};
