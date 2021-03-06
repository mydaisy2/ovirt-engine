package org.ovirt.engine.ui.uicommonweb.models.clusters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.common.action.AttachNetworkToVdsGroupParameter;
import org.ovirt.engine.core.common.action.DisplayNetworkToVdsGroupParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Cloner;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.ClusterNewNetworkModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

@SuppressWarnings("unused")
public class ClusterNetworkListModel extends SearchableListModel
{

    private UICommand privateNewNetworkCommand;

    public UICommand getNewNetworkCommand()
    {
        return privateNewNetworkCommand;
    }

    private void setNewNetworkCommand(UICommand value)
    {
        privateNewNetworkCommand = value;
    }

    private UICommand privateManageCommand;

    public UICommand getManageCommand()
    {
        return privateManageCommand;
    }

    private void setManageCommand(UICommand value)
    {
        privateManageCommand = value;
    }

    private UICommand privateSetAsDisplayCommand;

    public UICommand getSetAsDisplayCommand()
    {
        return privateSetAsDisplayCommand;
    }

    private void setSetAsDisplayCommand(UICommand value)
    {
        privateSetAsDisplayCommand = value;
    }

    private final Network displayNetwork = null;

    @Override
    public VDSGroup getEntity()
    {
        return (VDSGroup) ((super.getEntity() instanceof VDSGroup) ? super.getEntity() : null);
    }

    public void setEntity(VDSGroup value)
    {
        super.setEntity(value);
    }

    public ClusterNetworkListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().logicalNetworksTitle());
        setHashName("logical_networks"); //$NON-NLS-1$

        setManageCommand(new UICommand("Manage", this)); //$NON-NLS-1$
        setSetAsDisplayCommand(new UICommand("SetAsDisplay", this)); //$NON-NLS-1$
        setNewNetworkCommand(new UICommand("New", this)); //$NON-NLS-1$

        updateActionAvailability();
    }

    @Override
    protected void onEntityChanged()
    {
        super.onEntityChanged();
        getSearchCommand().execute();
    }

    @Override
    public void search()
    {
        if (getEntity() != null)
        {
            super.search();
        }
    }

    @Override
    protected void syncSearch()
    {
        if (getEntity() == null)
        {
            return;
        }

        super.syncSearch();

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object ReturnValue)
            {
                SearchableListModel searchableListModel = (SearchableListModel) model;
                ArrayList<Network> newItems = (ArrayList<Network>) ((VdcQueryReturnValue) ReturnValue).getReturnValue();
                Collections.sort(newItems, new Linq.NetworkComparator());
                searchableListModel.setItems(newItems);
            }
        };

        IdQueryParameters tempVar = new IdQueryParameters(getEntity().getId());
        tempVar.setRefresh(getIsQueryFirstTime());
        Frontend.RunQuery(VdcQueryType.GetAllNetworksByClusterId, tempVar, _asyncQuery);
    }

    public void setAsDisplay()
    {
        Network network = (Network) getSelectedItem();

        Frontend.RunAction(VdcActionType.UpdateDisplayToVdsGroup, new DisplayNetworkToVdsGroupParameters(getEntity(),
                network, true));
    }

    public void manage()
    {
        if (getWindow() != null)
        {
            return;
        }

        Guid storagePoolId =
                (getEntity().getStoragePoolId() != null) ? getEntity().getStoragePoolId() : Guid.Empty;

        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.setModel(this);
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model, Object result)
            {
                ClusterNetworkListModel clusterNetworkListModel = (ClusterNetworkListModel) model;
                ArrayList<Network> dcNetworks = (ArrayList<Network>) result;
                ClusterNetworkManageModel networkToManage = createNetworkList(dcNetworks);
                clusterNetworkListModel.setWindow(networkToManage);
                networkToManage.setTitle(ConstantsManager.getInstance().getConstants().assignDetachNetworksTitle());
                networkToManage.setHashName("assign_networks"); //$NON-NLS-1$
            }
        };
        // fetch the list of DC Networks
        AsyncDataProvider.getNetworkList(_asyncQuery, storagePoolId);
    }

    private ClusterNetworkManageModel createNetworkList(List<Network> dcNetworks) {
        List<ClusterNetworkModel> networkList = new ArrayList<ClusterNetworkModel>();
        java.util.ArrayList<Network> clusterNetworks = Linq.<Network> cast(getItems());
        for (Network network : dcNetworks) {
            ClusterNetworkModel networkManageModel;
            int index = clusterNetworks.indexOf(network);
            if (index >= 0) {
                Network clusterNetwork = clusterNetworks.get(index);
                networkManageModel = new ClusterNetworkModel((Network) Cloner.clone(clusterNetwork));
            } else {
                networkManageModel = new ClusterNetworkModel((Network) Cloner.clone(network));
            }
            networkManageModel.setCluster((VDSGroup) Cloner.clone(getEntity()));
            networkList.add(networkManageModel);
        }

        Collections.sort(networkList, new Linq.ClusterNetworkModelComparator());

        ClusterNetworkManageModel listModel = new ClusterNetworkManageModel();
        listModel.setItems(networkList);

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
        List<Network> existingClusterNetworks = Linq.<Network> cast(getItems());
        final ArrayList<VdcActionParametersBase> toAttach = new ArrayList<VdcActionParametersBase>();
        final ArrayList<VdcActionParametersBase> toDetach = new ArrayList<VdcActionParametersBase>();

        for (ClusterNetworkModel networkModel : manageList) {
            Network network = networkModel.getEntity();
            boolean contains = existingClusterNetworks.contains(network);

            boolean needsAttach = networkModel.isAttached() && !contains;
            boolean needsDetach = !networkModel.isAttached() && contains;
            boolean needsUpdate = false;

            if (contains && !needsDetach) {
                Network clusterNetwork = existingClusterNetworks.get(existingClusterNetworks.indexOf(network));

                if ((networkModel.isRequired() != clusterNetwork.getCluster().isRequired())
                        || (networkModel.isDisplayNetwork() != clusterNetwork.getCluster().isDisplay())
                        || (networkModel.isMigrationNetwork() != clusterNetwork.getCluster().isMigration())) {
                    needsUpdate = true;
                }
            }

            if (needsAttach || needsUpdate) {
                toAttach.add(new AttachNetworkToVdsGroupParameter(getEntity(), network));
            }

            if (needsDetach) {
                toDetach.add(new AttachNetworkToVdsGroupParameter(getEntity(), network));
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

    public void cancel()
    {
        setWindow(null);
    }

    @Override
    protected void entityChanging(Object newValue, Object oldValue)
    {
        VDSGroup vdsGroup = (VDSGroup) newValue;
        getNewNetworkCommand().setIsExecutionAllowed(vdsGroup != null && vdsGroup.getStoragePoolId() != null);
    }

    @Override
    protected void onSelectedItemChanged()
    {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged()
    {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    private void updateActionAvailability()
    {
        Network network = (Network) getSelectedItem();

        // CanRemove = SelectedItems != null && SelectedItems.Count > 0;
        getSetAsDisplayCommand().setIsExecutionAllowed(getSelectedItems() != null && getSelectedItems().size() == 1
                && network != null && !network.getCluster().isDisplay()
                && network.getCluster().getStatus() != NetworkStatus.NON_OPERATIONAL);
    }

    public void newEntity()
    {
        if (getWindow() != null)
        {
            return;
        }

        final ClusterNewNetworkModel networkModel = new ClusterNewNetworkModel(this, getEntity());
        setWindow(networkModel);

        // Set selected dc
        if (getEntity().getStoragePoolId() != null)
        {
            AsyncQuery _asyncQuery = new AsyncQuery();
            _asyncQuery.setModel(networkModel);
            _asyncQuery.asyncCallback = new INewAsyncCallback() {
                @Override
                public void onSuccess(Object model, Object result)
                {
                    final StoragePool dataCenter = (StoragePool) result;
                    networkModel.getDataCenters().setItems(Arrays.asList(dataCenter));
                    networkModel.getDataCenters().setSelectedItem(dataCenter);

                }
            };
            AsyncDataProvider.getDataCenterById(_asyncQuery, getEntity().getStoragePoolId());
        }
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getManageCommand())
        {
            manage();
        }
        else if (command == getSetAsDisplayCommand())
        {
            setAsDisplay();
        }

        else if (StringHelper.stringsEqual(command.getName(), "OnManage")) //$NON-NLS-1$
        {
            onManage();
        }
        else if (StringHelper.stringsEqual(command.getName(), "New")) //$NON-NLS-1$
        {
            newEntity();
        }
        else if (StringHelper.stringsEqual(command.getName(), "Cancel")) //$NON-NLS-1$
        {
            cancel();
        }
    }

    @Override
    protected String getListName() {
        return "ClusterNetworkListModel"; //$NON-NLS-1$
    }

}
