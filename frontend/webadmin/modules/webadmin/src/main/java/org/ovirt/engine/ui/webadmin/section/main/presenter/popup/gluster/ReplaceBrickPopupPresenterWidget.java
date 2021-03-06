package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.models.gluster.ReplaceBrickModel;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class ReplaceBrickPopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<ReplaceBrickModel, ReplaceBrickPopupPresenterWidget.ViewDef> {

    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<ReplaceBrickModel> {
    }

    @Inject
    public ReplaceBrickPopupPresenterWidget(EventBus eventBus, ViewDef view) {
        super(eventBus, view);
    }

}
