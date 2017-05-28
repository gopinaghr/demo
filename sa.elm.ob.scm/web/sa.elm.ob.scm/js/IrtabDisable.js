var lastRecordId = "",
    Status = "",
    type = "",
    count, tabId, flag = 0,
    printButtonLastRecordId;
isc.OBToolbar.addClassProperties({
    BUTTON_PROPERTIES: {
        'newDoc': {
            updateState: function() {
                var view = this.view,
                    tabId = view['tabId'];
                form = view.viewForm, grid = view.viewGrid;
                //Initial receipt tab
                if (tabId === '2A8F52E5BF1846B2BFBDAAFEF6F89135') {
                    var recordId = view.parentRecordId,
                        thisRefir = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft == 1) {
                                thisRefir.setDisabled(true);
                            } else {
                                thisRefir.setDisabled(false);
                            }
                        });
                    }
                }
                // issue return transaction 
                else if (tabId === '5B16AE5DFDEF47BB9518CDD325F31DFF') {
                    var recordId = view.parentRecordId,
                        thisRefissreturn = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.receivingType === "IRT") {
                                thisRefissreturn.setDisabled(true);
                            } else {
                                thisRefissreturn.setDisabled(false);
                            }
                        });
                    }
                }
                //  return transaction 
                else if (tabId === '0C0819F5D78A401A916BDD8ADB30E4EF') {
                    var recordId = view.parentRecordId,
                        thisRefreturn = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.receivingType === "INR") {
                                thisRefreturn.setDisabled(true);
                            } else {
                                thisRefreturn.setDisabled(false);
                            }
                        });
                    }
                }
                // custody  return transaction 
                else if (tabId === 'DD6AB8A564D5482795B0976F6A68FBC5') {
                    var recordId = view.parentRecordId,
                        thisRefcus = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.receivingType === "INR") {
                                thisRefcus.setDisabled(true);
                            } else {
                                thisRefcus.setDisabled(false);
                            }
                        });

                    }
                }
                // custody  isue return transaction 
                else if (tabId === 'D4E9D5A2F73E4A15AEA52FD9A5A57902') {
                    var recordId = view.parentRecordId,
                        thisRefcusiss = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.receivingType === "IRT") {
                                thisRefcusiss.setDisabled(true);
                            } else {
                                thisRefcusiss.setDisabled(false);
                            }
                        });
                    }
                }
                //Custody Transfer window  line
                else if (tabId === '09E1F10975074FD9B4E43AF27AA54DE9') {
                    this.setDisabled(true);
                }
                //Custody Transfer window  Custody Transaction
                else if (tabId === '950C9D8B1D9944B3840495CD2BE80407') {
                    this.setDisabled(true);
                }
                //Material Issue Request
                else if (tabId === '4AB913F4E6064ED1833ED08A8B7FA2D5') {
                    tabId = 'CE947EDC9B174248883292F17F03BB32';
                    var recordId = view.parentRecordId,
                        thisRefmi = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft == 1) {
                                thisRefmi.setDisabled(true);
                            } else {
                                thisRefmi.setDisabled(false);
                            }
                        });
                    }
                }
                // //Material Issue Request --custody Transaction
                else if (tabId === 'CB52E60359E7477E82FA36BDBFF0008C') {
                    this.setDisabled(true);
                }
                //inventory counting line
                else if (tabId === '9A4225DDEFFD40C8BFA386059CA93DEC') {
                    var recordId = view.parentRecordId,
                        thisRefic = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft == 1) {
                                thisRefic.setDisabled(true);
                            } else {
                                thisRefic.setDisabled(false);
                            }
                        });
                    }
                } else {
                    if (view.isShowingForm) {
                        this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
                    } else {
                        this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
                    }
                }
            }
        },
        'newRow': {
            updateState: function() {
                var view = this.view,
                    tabId = view['tabId'];
                form = view.viewForm, grid = view.viewGrid;

                //Initial receipt tab
                if (tabId === '2A8F52E5BF1846B2BFBDAAFEF6F89135') {
                    var recordId = view.parentRecordId,
                        thisRefirrow = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft == 1) {
                                thisRefirrow.setDisabled(true);
                            } else {
                                thisRefirrow.setDisabled(false);
                            }
                        });
                    }
                }
                // issue return transaction 
                else if (tabId === '5B16AE5DFDEF47BB9518CDD325F31DFF') {
                    var recordId = view.parentRecordId,
                        thisRefissreturnrow = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.receivingType === "IRT") {
                                thisRefissreturnrow.setDisabled(true);
                            } else {
                                thisRefissreturnrow.setDisabled(false);
                            }
                        });
                    }
                }
                //  return transaction 
                else if (tabId === '0C0819F5D78A401A916BDD8ADB30E4EF') {
                    var recordId = view.parentRecordId,
                        thisRefrow = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.receivingType === "INR") {
                                thisRefrow.setDisabled(true);
                            } else {
                                thisRefrow.setDisabled(false);
                            }
                        });
                    }
                }
                // custody  return transaction 
                else if (tabId === 'DD6AB8A564D5482795B0976F6A68FBC5') {
                    var recordId = view.parentRecordId,
                        thisRefcusrow = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.receivingType === "INR") {
                                thisRefcusrow.setDisabled(true);
                            } else {
                                thisRefcusrow.setDisabled(false);
                            }
                        });

                    }
                }
                // custody  isue return transaction 
                else if (tabId === 'D4E9D5A2F73E4A15AEA52FD9A5A57902') {
                    var recordId = view.parentRecordId,
                        thisRefcusissrow = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.receivingType === "IRT") {
                                thisRefcusissrow.setDisabled(true);
                            } else {
                                thisRefcusissrow.setDisabled(false);
                            }
                        });
                    }
                }
                //Custody Transfer window  line
                else if (tabId === '09E1F10975074FD9B4E43AF27AA54DE9') {
                    this.setDisabled(true);
                }
                //Custody Transfer window  Custody Transaction
                else if (tabId === '950C9D8B1D9944B3840495CD2BE80407') {
                    this.setDisabled(true);
                }
                //Material Issue Request
                else if (tabId === '4AB913F4E6064ED1833ED08A8B7FA2D5') {
                    tabId = 'CE947EDC9B174248883292F17F03BB32';
                    var recordId = view.parentRecordId,
                        thisRefmirow = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft == 1) {
                                thisRefmirow.setDisabled(true);
                            } else {
                                thisRefmirow.setDisabled(false);
                            }
                        });
                    }
                }
                // //Material Issue Request --custody Transaction
                else if (tabId === 'CB52E60359E7477E82FA36BDBFF0008C') {
                    this.setDisabled(true);
                }
                //inventory counting line
                else if (tabId === '9A4225DDEFFD40C8BFA386059CA93DEC') {
                    var recordId = view.parentRecordId,
                        thisReficrow = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft == 1) {
                                thisReficrow.setDisabled(true);
                            } else {
                                thisReficrow.setDisabled(false);
                            }
                        });
                    }
                } else {
                    if (view.isShowingForm) {
                        this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
                    } else {
                        this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
                    }
                }
            }
        },
        'eliminate': {
            updateState: function() {
                var view = this.view,
                    tabId = view['tabId'];
                form = view.viewForm, grid = view.viewGrid;
                //Initial receipt tab
                if (tabId === '2A8F52E5BF1846B2BFBDAAFEF6F89135') {
                    var recordId = view.parentRecordId,
                        thisReficdel = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft == 1) {
                                thisReficdel.setDisabled(true);
                            } else {
                                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisReficdel);
                            }
                        });
                    }
                }
                //purchase requisition line
                else if (tabId === '800251') {
                    var recordId = view.parentRecordId,
                        thisRefprdel = this;
                    if (recordId != null) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.PurReqDeleteButtonDisableHandler', {
                            recordId: recordId
                        }, {}, function(response, data, request) {
                            if (data.reqCount == 0) {
                                thisRefprdel.setDisabled(true);
                            } else {
                                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRefprdel);
                            }
                        });
                    }
                }
                //inventory counting line
                else if (tabId === '9A4225DDEFFD40C8BFA386059CA93DEC') {
                    var recordId = view.parentRecordId,
                        thisReficdel = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft === 1) {
                                thisReficdel.setDisabled(true);
                            } else {
                                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisReficdel);
                            }
                        });
                    }
                }
                //Return Transaction(72A6B3CA5BE848ACA976304375A5B7A6) and Issue Return Transaction(922927563BFC48098D17E4DC85DD504C)
                else if (tabId === '72A6B3CA5BE848ACA976304375A5B7A6' || tabId === '922927563BFC48098D17E4DC85DD504C') {
                    if (grid.getSelectedRecord() != null) {
                        var recordId = grid.getSelectedRecord().id,
                            thisRTTranDel = this;
                        if (recordId != null && recordId != -1) {
                            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                                recordId: recordId,
                                tabId: tabId
                            }, {}, function(response, data, request) {
                                if (data.IsDraft === 1) {
                                    thisRTTranDel.setDisabled(true);
                                } else {
                                    thisRTTranDel.setDisabled(false);
                                }
                            });
                        }
                    }

                }
                //Return Transaction line (0C0819F5D78A401A916BDD8ADB30E4EF), Issue Return Transaction line(5B16AE5DFDEF47BB9518CDD325F31DFF)
                else if (tabId === '0C0819F5D78A401A916BDD8ADB30E4EF' || tabId === '5B16AE5DFDEF47BB9518CDD325F31DFF') {
                    var recordId = view.parentRecordId,
                        thisRTlineDel = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft === 1) {
                                thisRTlineDel.setDisabled(true);
                            } else {
                                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRTlineDel);
                            }
                        });
                    }
                }
                //Custody transaction under Return Transaction (DD6AB8A564D5482795B0976F6A68FBC5) and Custody transaction under Issue Return Transaction (D4E9D5A2F73E4A15AEA52FD9A5A57902)
                else if (tabId === 'DD6AB8A564D5482795B0976F6A68FBC5' || tabId === 'D4E9D5A2F73E4A15AEA52FD9A5A57902') {
                    var recordId = view.parentRecordId,
                        thisRTcusline = this;
                    if (recordId != null && recordId != -1) {
                        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                            recordId: recordId,
                            tabId: tabId
                        }, {}, function(response, data, request) {
                            if (data.IsDraft === 1) {
                                thisRTcusline.setDisabled(true);
                            } else {
                                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRTcusline);
                            }
                        });
                    }
                } else {
                    if (view.isShowingForm) {
                        this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly || form.isNew);
                    } else {
                        if (grid.getSelectedRecords().length > 0) {
                            this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
                        } else {
                            this.setDisabled(true);
                        }
                    }
                }
            },
            defaultUpdateFunction: function(eliminate) {
                var view = eliminate.view,
                    form = view.viewForm,
                    currentGrid, length, selectedRecords, i;
                if (view.isShowingTree) {
                    currentGrid = view.treeGrid;
                } else {
                    currentGrid = view.viewGrid;
                }
                selectedRecords = currentGrid.getSelectedRecords();
                length = selectedRecords.length;
                if (!eliminate.view.isDeleteableTable) {
                    eliminate.setDisabled(true);
                    return;
                }
                for (i = 0; i < length; i++) {
                    if (!currentGrid.isWritable(selectedRecords[i])) {
                        eliminate.setDisabled(true);
                        return;
                    }
                    if (selectedRecords[i]._new) {
                        eliminate.setDisabled(true);
                        return;
                    }
                }
                if (view.isShowingForm) {
                    eliminate.setDisabled(form.isSaving || form.readOnly || view.singleRecord || !view.hasValidState() || form.isNew || (view.standardWindow.allowDelete === 'N'));
                } else {
                    eliminate.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || !currentGrid.getSelectedRecords() || currentGrid.getSelectedRecords().length === 0 || (view.standardWindow.allowDelete === 'N'));
                }
            }
        },
        'print': {
            updateState: function() {
                var view = this.view,
                    tabId = view['tabId'];
                form = view.viewForm, grid = view.viewGrid;

                //PO Receipt - Header Tab
                if (tabId === '296') {
                    this.hide();
                } else {
                    if (view.isShowingForm) {
                        this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
                    } else {
                        this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
                    }
                }
            }
        },
        'escm_print_pdf': {
            updateState: function() {

                var view = this.view,
                    tabId = view['tabId'];
                form = view.viewForm, grid = view.viewGrid;
                //PO Receipt tab=296, Material Issue Req=CE947EDC9B174248883292F17F03BB32, Return Tran=72A6B3CA5BE848ACA976304375A5B7A6,Custody Transfer=CB9A2A4C6DB24FD19D542A78B07ED6C1
                if (tabId === '296' || tabId === 'CE947EDC9B174248883292F17F03BB32' || tabId === '72A6B3CA5BE848ACA976304375A5B7A6' || tabId === 'CB9A2A4C6DB24FD19D542A78B07ED6C1' || tabId === '922927563BFC48098D17E4DC85DD504C') {
                    if (grid.getSelectedRecords().length === 0) {
                        this.setDisabled(true);
                    }
                    if (grid.getSelectedRecords().length === 1 /*&& printButtonLastRecordId !== grid.getSelectedRecord().id*/ ) {
                        var recordId = grid.getSelectedRecord().id,
                            thisRefPrint = this;
                        if (recordId != null && recordId != -1) {
                            printButtonLastRecordId = recordId;
                            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.IrTabDisableProcess', {
                                recordId: recordId,
                                tabId: tabId
                            }, {}, function(response, data, request) {
                                if (data.IsDraft === 0) {
                                    thisRefPrint.setDisabled(true);
                                } else {
                                    thisRefPrint.setDisabled(false);
                                }
                            });
                        }
                    }
                } else {
                    if (view.isShowingForm) {
                        this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
                    } else {
                        this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
                    }
                }
            }
        }
    }
});