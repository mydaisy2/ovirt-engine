package org.ovirt.engine.ui.uicommonweb.models.networks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ovirt.engine.core.common.action.AttachNetworkToVdsGroupParameter;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.NetworkView;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.PairQueryable;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Cloner;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterNetworkManageModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterNetworkModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

@SuppressWarnings("unused")
public class NetworkClusterListModel extends SearchableListModel
{
    private UICommand manageCommand;

    private final Comparator<ClusterNetworkModel> manageModelComparator =
            new Comparator<ClusterNetworkModel>() {
                @Override
                public int compare(ClusterNetworkModel o1, ClusterNetworkModel o2) {
                    return o1.getCluster().getName().compareTo(o2.getCluster().getName());
                }
            };

    public NetworkClusterListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().clustersTitle());
        setHashName("clusters"); //$NON-NLS-1$

        setManageCommand(new UICommand("Manage", this)); //$NON-NLS-1$
    }

    public void manage()
    {
        if (getWindow() != null)
        {
            return;
        }

        ClusterNetworkManageModel manageModel = createManageList();
        setWindow(manageModel);
        manageModel.setTitle(ConstantsManager.getInstance().getConstants().assignDetachNetworkTitle());
        manageModel.setHashName("assign_network"); //$NON-NLS-1$
    }

    private ClusterNetworkManageModel createManageList() {
        List<ClusterNetworkModel> networkManageModelList = new ArrayList<ClusterNetworkModel>();
        List<PairQueryable<VDSGroup, NetworkCluster>> items =
                (List<PairQueryable<VDSGroup, NetworkCluster>>) getItems();

        for (PairQueryable<VDSGroup, NetworkCluster> item : items) {
            Network network = (Network) Cloner.clone(getEntity());
            if (item.getSecond() != null) {
                network.setCluster((NetworkCluster) Cloner.clone(item.getSecond()));
            }
            ClusterNetworkModel networkManageModel = new ClusterNetworkModel(network) {
                @Override
                public String getDisplayedName() {
                    return getCluster().getName();
                }
            };
            networkManageModel.setCluster((VDSGroup) Cloner.clone(item.getFirst()));

            networkManageModelList.add(networkManageModel);
        }

        Collections.sort(networkManageModelList, manageModelComparator);

        ClusterNetworkManageModel listModel = new ClusterNetworkManageModel() {
            @Override
            public boolean isMultiCluster() {
                return true;
            }
        };
        listModel.setItems(networkManageModelList);

        UICommand cancelCommand = new UICommand("Cancel", this); //$NON-NLS-1$
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        listModel.getCommands().add(cancelCommand);

        UICommand okCommand = new UICommand("OnManage", this); //$NON-NLS-1$
        okCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        okCommand.setIsDefault(true);
        listModel.getCommands().add(0, okCommand);

        return listModel;
    }

    public void onManage() {
        final ClusterNetworkManageModel windowModel = (ClusterNetworkManageModel) getWindow();

        List<ClusterNetworkModel> manageList = windowModel.getItems();
        final ArrayList<VdcActionParametersBase> toAttach = new ArrayList<VdcActionParametersBase>();
        final ArrayList<VdcActionParametersBase> toDetach = new ArrayList<VdcActionParametersBase>();

        for (ClusterNetworkModel manageModel : manageList) {
            PairQueryable<VDSGroup, NetworkCluster> item = getItem(manageModel.getCluster().getName());
            boolean wasAttached = item.getSecond() != null;

            boolean needsAttach = manageModel.isAttached() && !wasAttached;
            boolean needsDetach = !manageModel.isAttached() && wasAttached;
            boolean needsUpdate = false;

            // Attachment wasn't changed- check if needs update
            if (wasAttached && !needsDetach) {
                if ((manageModel.isRequired() != item.getSecond().isRequired())
                        || (manageModel.isDisplayNetwork() != item.getSecond().isDisplay())
                        || (manageModel.isMigrationNetwork() != item.getSecond().isMigration())) {
                    needsUpdate = true;
                }
            }

            if (needsAttach || needsUpdate) {
                toAttach.add(new AttachNetworkToVdsGroupParameter(manageModel.getCluster(), manageModel.getEntity()));
            }

            if (needsDetach) {
                toDetach.add(new AttachNetworkToVdsGroupParameter(manageModel.getCluster(), manageModel.getEntity()));
            }
        }

        final IFrontendMultipleActionAsyncCallback callback = new IFrontendMultipleActionAsyncCallback() {
            Boolean needsAttach = !toAttach.isEmpty();
            Boolean needsDetach = !toDetach.isEmpty();

            @Override
            public void executed(FrontendMultipleActionAsyncResult result) {
                if (result.getActionType() == VdcActionType.DetachNetworkToVdsGroup) {
                    needsDetach = false;
                }
                if (result.getActionType() == VdcActionType.AttachNetworkToVdsGroup) {
                    needsAttach = false;
                }

                if (needsAttach) {
                    Frontend.RunMultipleAction(VdcActionType.AttachNetworkToVdsGroup, toAttach, this, null);
                }

                if (needsDetach) {
                    Frontend.RunMultipleAction(VdcActionType.DetachNetworkToVdsGroup, toDetach, this, null);
                }

                if (!needsAttach && !needsDetach) {
                    doFinish();
                }
            }

            private void doFinish() {
                windowModel.stopProgress();
                cancel();
                forceRefresh();
            }
        };

        callback.executed(new FrontendMultipleActionAsyncResult(null, null, null));
        windowModel.startProgress(null);
    }

    private PairQueryable<VDSGroup, NetworkCluster> getItem(String clusterName) {
        List<PairQueryable<VDSGroup, NetworkCluster>> items =
                (List<PairQueryable<VDSGroup, NetworkCluster>>) getItems();
        for (PairQueryable<VDSGroup, NetworkCluster> item : items) {
            if (item.getFirst().getName().equals(clusterName)) {
                return item;
            }
        }
        return null;
    }

    public void cancel() {
        setWindow(null);
    }

    @Override
    public NetworkView getEntity() {
        return (NetworkView) ((super.getEntity() instanceof NetworkView) ? super.getEntity() : null);
    }

    public void setEntity(NetworkView value) {
        super.setEntity(value);
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();
        getSearchCommand().execute();
    }

    @Override
    public void search() {
        if (getEntity() != null)
        {
            super.search();
        }
    }

    @Override
    protected void syncSearch() {
        if (getEntity() == null)
        {
            return;
        }

        AsyncQuery asyncQuery = new AsyncQuery();
        asyncQuery.setModel(this);
        asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object ReturnValue)
            {
                NetworkClusterListModel.this.setItems((List<PairQueryable<VDSGroup, NetworkCluster>>) ((VdcQueryReturnValue) ReturnValue).getReturnValue());
            }
        };

        IdQueryParameters params = new IdQueryParameters(getEntity().getId());
        params.setRefresh(getIsQueryFirstTime());
        Frontend.RunQuery(VdcQueryType.GetVdsGroupsAndNetworksByNetworkId, params, asyncQuery);
    }

    @Override
    public void setItems(Iterable value) {
        Collections.sort((List<PairQueryable<VDSGroup, NetworkCluster>>) value,
                new Comparator<PairQueryable<VDSGroup, NetworkCluster>>() {

                    @Override
                    public int compare(PairQueryable<VDSGroup, NetworkCluster> arg0,
                            PairQueryable<VDSGroup, NetworkCluster> arg1) {
                        return arg0.getFirst().getName().compareTo(arg1.getFirst().getName());
                    }
                });
        super.setItems(value);
    }

    @Override
    protected void entityPropertyChanged(Object sender, PropertyChangedEventArgs e) {
        super.entityPropertyChanged(sender, e);

        if (e.PropertyName.equals("name")) //$NON-NLS-1$
        {
            getSearchCommand().execute();
        }
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getManageCommand())
        {
            manage();
        }
        else if (StringHelper.stringsEqual(command.getName(), "OnManage")) //$NON-NLS-1$
        {
            onManage();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            cancel();
        }
    }

    public UICommand getManageCommand() {
        return manageCommand;
    }

    private void setManageCommand(UICommand value) {
        manageCommand = value;
    }

    @Override
    protected String getListName() {
        return "NetworkClusterListModel"; //$NON-NLS-1$
    }
}
