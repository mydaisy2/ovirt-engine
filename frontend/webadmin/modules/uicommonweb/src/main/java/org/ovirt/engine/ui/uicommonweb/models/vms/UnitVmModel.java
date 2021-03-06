package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.VmWatchdogAction;
import org.ovirt.engine.core.common.businessentities.VmWatchdogType;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemType;
import org.ovirt.engine.ui.uicommonweb.models.storage.DisksAllocationModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueModel;
import org.ovirt.engine.ui.uicommonweb.validation.ByteSizeValidation;
import org.ovirt.engine.ui.uicommonweb.validation.I18NNameValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IntegerValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LengthValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NoTrimmingWhitespacesValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyQuotaValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.PoolNameValidation;
import org.ovirt.engine.ui.uicommonweb.validation.SpecialAsciiI18NOrNoneValidation;
import org.ovirt.engine.ui.uicommonweb.validation.ValidationResult;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.EnumTranslator;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.UIConstants;

public class UnitVmModel extends Model {

    public static final int VM_TEMPLATE_NAME_MAX_LIMIT = 40;
    public static final int DESCRIPTION_MAX_LIMIT = 255;

    private boolean privateIsNew;

    private EntityModel editingEnabled;

    public EntityModel getEditingEnabled() {
        return editingEnabled;
    }

    public void setEditingEnabled(EntityModel editingEnabled) {
        this.editingEnabled = editingEnabled;
    }

    public boolean getIsNew()
    {
        return privateIsNew;
    }

    public void setIsNew(boolean value)
    {
        privateIsNew = value;
    }

    private boolean vmAttachedToPool;

    public boolean isVmAttachedToPool() {
        return vmAttachedToPool;
    }

    private NotChangableForVmInPoolEntityModel isSoundcardEnabled;

    public EntityModel getIsSoundcardEnabled() {
        return isSoundcardEnabled;
    }

    private void setIsSoundcardEnabled(NotChangableForVmInPoolEntityModel isSoundcardEnabled) {
        this.isSoundcardEnabled = isSoundcardEnabled;
    }

    private NotChangableForVmInPoolListModel dataCenterWithClustersList;

    public ListModel getDataCenterWithClustersList() {
        return dataCenterWithClustersList;
    }

    private void setDataCenterWithClustersList(NotChangableForVmInPoolListModel dataCenterWithClustersList) {
        this.dataCenterWithClustersList = dataCenterWithClustersList;
    }

    private ListModel vnicProfiles;

    public ListModel getVnicProfiles() {
        return vnicProfiles;
    }

    private void setVnicProfiles(ListModel vnicProfiles) {
        this.vnicProfiles = vnicProfiles;
    }

    private ListModel nicsWithLogicalNetworks;

    public ListModel getNicsWithLogicalNetworks() {
        return nicsWithLogicalNetworks;
    }

    public void setNicsWithLogicalNetworks(ListModel nicsWithLogicalNetworks) {
        this.nicsWithLogicalNetworks = nicsWithLogicalNetworks;
    }

    /**
     * Note: We assume that this method is called only once, on the creation stage of the model. if this assumption is
     * changed (i.e the VM can attached/detached from a pool after the model is created), this method should be modified
     */
    public void setVmAttachedToPool(boolean value) {
        if (value) {
            // ==General Tab==
            getDataCenterWithClustersList().setIsChangable(!value);
            getQuota().setIsChangable(false);
            getDescription().setIsChangable(false);
            getComment().setIsChangable(false);

            getNumOfDesktops().setIsChangable(false);
            getPrestartedVms().setIsChangable(false);
            getMaxAssignedVmsPerUser().setIsChangable(false);

            getTemplate().setIsChangable(false);
            getMemSize().setIsChangable(false);
            getTotalCPUCores().setIsChangable(false);

            getCoresPerSocket().setIsChangable(false);
            getNumOfSockets().setIsChangable(false);

            getOSType().setIsChangable(false);
            getIsStateless().setIsChangable(false);
            getIsRunAndPause().setIsChangable(false);
            getIsDeleteProtected().setIsChangable(false);

            // ==Initial run Tab==
            getTimeZone().setIsChangable(false);
            getDomain().setIsChangable(false);

            // ==Console Tab==
            getDisplayProtocol().setIsChangable(false);
            getUsbPolicy().setIsChangable(false);
            getNumOfMonitors().setIsChangable(false);
            getIsSingleQxlEnabled().setIsChangable(false);
            getIsSmartcardEnabled().setIsChangable(false);
            getAllowConsoleReconnect().setIsChangable(false);
            getVncKeyboardLayout().setIsChangable(false);

            // ==Host Tab==
            getIsAutoAssign().setIsChangable(false);
            getDefaultHost().setIsChangable(false);
            getHostCpu().setIsChangable(false);
            getMigrationMode().setIsChangable(false);
            getCpuPinning().setIsChangable(false);

            // ==Resource Allocation Tab==
            getMinAllocatedMemory().setIsChangable(false);
            getProvisioning().setIsChangable(false);
            getProvisioningThin_IsSelected().setIsChangable(false);
            getProvisioningClone_IsSelected().setIsChangable(false);
            getDisksAllocationModel().setIsChangable(false);

            // ==Boot Options Tab==
            getFirstBootDevice().setIsChangable(false);
            getSecondBootDevice().setIsChangable(false);
            getCdAttached().setIsChangable(false);
            getCdImage().setIsChangable(false);
            getKernel_path().setIsChangable(false);
            getInitrd_path().setIsChangable(false);
            getKernel_parameters().setIsChangable(false);

            // ==Custom Properties Tab==
            getCustomProperties().setIsChangable(false);

            vmAttachedToPool = true;
        }
    }

    private String privateHash;

    public String getHash()
    {
        return privateHash;
    }

    public void setHash(String value)
    {
        privateHash = value;
    }

    private boolean isBlankTemplate;

    public boolean getIsBlankTemplate()
    {
        return isBlankTemplate;
    }

    public void setIsBlankTemplate(boolean value)
    {
        if (isBlankTemplate != value)
        {
            isBlankTemplate = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsBlankTemplate")); //$NON-NLS-1$
        }
    }

    private boolean isWindowsOS;

    public boolean getIsWindowsOS()
    {
        return isWindowsOS;
    }

    public void setIsWindowsOS(boolean value)
    {
        if (isWindowsOS != value)
        {
            isWindowsOS = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsWindowsOS")); //$NON-NLS-1$
        }
    }

    private boolean isLinuxOS;

    public boolean getIsLinuxOS()
    {
        return isLinuxOS;
    }

    public void setIsLinuxOS(boolean value)
    {
        if (isLinuxOS != value)
        {
            isLinuxOS = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsLinuxOS")); //$NON-NLS-1$
        }
    }

    private String cpuNotification;

    public String getCPUNotification()
    {
        return cpuNotification;
    }

    public void setCPUNotification(String value)
    {
        if (!StringHelper.stringsEqual(cpuNotification, value))
        {
            cpuNotification = value;
            onPropertyChanged(new PropertyChangedEventArgs("CPUNotification")); //$NON-NLS-1$
        }
    }

    public boolean isCPUsAmountValid;

    public boolean getIsCPUsAmountValid()
    {
        return isCPUsAmountValid;
    }

    public void setIsCPUsAmountValid(boolean value)
    {
        if (isCPUsAmountValid != value)
        {
            isCPUsAmountValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsCPUsAmountValid")); //$NON-NLS-1$
        }
    }

    private boolean isGeneralTabValid;

    public boolean getIsGeneralTabValid()
    {
        return isGeneralTabValid;
    }

    public void setIsGeneralTabValid(boolean value)
    {
        if (isGeneralTabValid != value)
        {
            isGeneralTabValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsGeneralTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isFirstRunTabValid;

    public boolean getIsFirstRunTabValid()
    {
        return isFirstRunTabValid;
    }

    public void setIsFirstRunTabValid(boolean value)
    {
        if (isFirstRunTabValid != value)
        {
            isFirstRunTabValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsFirstRunTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isDisplayTabValid;

    public boolean getIsDisplayTabValid()
    {
        return isDisplayTabValid;
    }

    public void setIsDisplayTabValid(boolean value)
    {
        if (isDisplayTabValid != value)
        {
            isDisplayTabValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsDisplayTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isAllocationTabValid;

    public boolean getIsAllocationTabValid()
    {
        return isAllocationTabValid;
    }

    public void setIsAllocationTabValid(boolean value)
    {
        if (isAllocationTabValid != value)
        {
            isAllocationTabValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsAllocationTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isHostTabValid;

    public boolean getIsHostTabValid()
    {
        return isHostTabValid;
    }

    public void setIsHostTabValid(boolean value)
    {
        if (isHostTabValid != value)
        {
            isHostTabValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsHostTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isBootSequenceTabValid;

    public boolean getIsBootSequenceTabValid()
    {
        return isBootSequenceTabValid;
    }

    public void setIsBootSequenceTabValid(boolean value)
    {
        if (isBootSequenceTabValid != value)
        {
            isBootSequenceTabValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsBootSequenceTabValid")); //$NON-NLS-1$
        }
    }

    private boolean isCustomPropertiesTabValid;

    public boolean getIsCustomPropertiesTabValid()
    {
        return isCustomPropertiesTabValid;
    }

    public void setIsCustomPropertiesTabValid(boolean value)
    {
        if (isCustomPropertiesTabValid != value)
        {
            isCustomPropertiesTabValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsCustomPropertiesTabValid")); //$NON-NLS-1$
        }
    }

    private NotChangableForVmInPoolListModel privateStorageDomain;

    public ListModel getStorageDomain()
    {
        return privateStorageDomain;
    }

    private void setStorageDomain(NotChangableForVmInPoolListModel value)
    {
        privateStorageDomain = value;
    }

    private NotChangableForVmInPoolListModel privateTemplate;

    public ListModel getTemplate()
    {
        return privateTemplate;
    }

    private void setTemplate(NotChangableForVmInPoolListModel value)
    {
        privateTemplate = value;
    }

    private NotChangableForVmInPoolListModel vmType;

    public void setVmType(NotChangableForVmInPoolListModel vmType) {
        this.vmType = vmType;
    }

    public ListModel getVmType() {
        return vmType;
    }

    private EntityModel privateName;

    public EntityModel getName()
    {
        return privateName;
    }

    private void setName(EntityModel value)
    {
        privateName = value;
    }

    private NotChangableForVmInPoolListModel privateOSType;

    public ListModel getOSType()
    {
        return privateOSType;
    }

    private void setOSType(NotChangableForVmInPoolListModel value)
    {
        privateOSType = value;
    }

    private NotChangableForVmInPoolListModel privateNumOfMonitors;

    public ListModel getNumOfMonitors()
    {
        return privateNumOfMonitors;
    }

    private void setNumOfMonitors(NotChangableForVmInPoolListModel value)
    {
        privateNumOfMonitors = value;
    }

    private NotChangableForVmInPoolEntityModel privateIsSingleQxlEnabled;

    public EntityModel getIsSingleQxlEnabled()
    {
        return privateIsSingleQxlEnabled;
    }

    private void setIsSingleQxlEnabled(NotChangableForVmInPoolEntityModel value)
    {
        privateIsSingleQxlEnabled = value;
    }


    private NotChangableForVmInPoolEntityModel privateAllowConsoleReconnect;

    public EntityModel getAllowConsoleReconnect()
    {
        return privateAllowConsoleReconnect;
    }

    private void setAllowConsoleReconnect(NotChangableForVmInPoolEntityModel value)
    {
        privateAllowConsoleReconnect = value;
    }

    private NotChangableForVmInPoolEntityModel privateDescription;

    public EntityModel getDescription()
    {
        return privateDescription;
    }

    private void setDescription(NotChangableForVmInPoolEntityModel value)
    {
        privateDescription = value;
    }

    private NotChangableForVmInPoolEntityModel privateComment;

    public EntityModel getComment()
    {
        return privateComment;
    }

    private void setComment(NotChangableForVmInPoolEntityModel value)
    {
        privateComment = value;
    }

    private NotChangableForVmInPoolListModel privateDomain;

    public ListModel getDomain()
    {
        return privateDomain;
    }

    private void setDomain(NotChangableForVmInPoolListModel value)
    {
        privateDomain = value;
    }

    private NotChangableForVmInPoolEntityModel privateMemSize;

    public EntityModel getMemSize()
    {
        return privateMemSize;
    }

    private void setMemSize(NotChangableForVmInPoolEntityModel value)
    {
        privateMemSize = value;
    }

    private NotChangableForVmInPoolEntityModel privateMinAllocatedMemory;

    public EntityModel getMinAllocatedMemory()
    {
        return privateMinAllocatedMemory;
    }

    private void setMinAllocatedMemory(NotChangableForVmInPoolEntityModel value)
    {
        privateMinAllocatedMemory = value;
    }

    private NotChangableForVmInPoolListModel privateQuota;

    public ListModel getQuota()
    {
        return privateQuota;
    }

    private void setQuota(NotChangableForVmInPoolListModel value)
    {
        privateQuota = value;
    }

    private NotChangableForVmInPoolListModel privateUsbPolicy;

    public ListModel getUsbPolicy()
    {
        return privateUsbPolicy;
    }

    private void setUsbPolicy(NotChangableForVmInPoolListModel value)
    {
        privateUsbPolicy = value;
    }

    private NotChangableForVmInPoolListModel privateTimeZone;

    public ListModel getTimeZone()
    {
        return privateTimeZone;
    }

    private void setTimeZone(NotChangableForVmInPoolListModel value)
    {
        privateTimeZone = value;
    }

    private NotChangableForVmInPoolListModel privateNumOfSockets;

    public ListModel getNumOfSockets()
    {
        return privateNumOfSockets;
    }

    private void setNumOfSockets(NotChangableForVmInPoolListModel value)
    {
        privateNumOfSockets = value;
    }

    private NotChangableForVmInPoolEntityModel privateTotalCPUCores;

    public EntityModel getTotalCPUCores()
    {
        return privateTotalCPUCores;
    }

    private void setTotalCPUCores(NotChangableForVmInPoolEntityModel value)
    {
        privateTotalCPUCores = value;
    }

    private NotChangableForVmInPoolListModel privateCoresPerSocket;

    public ListModel getCoresPerSocket()
    {
        return privateCoresPerSocket;
    }

    private void setCoresPerSocket(NotChangableForVmInPoolListModel value)
    {
        privateCoresPerSocket = value;
    }

    private NotChangableForVmInPoolListModel privateDefaultHost;

    public ListModel getDefaultHost()
    {
        return privateDefaultHost;
    }

    private void setDefaultHost(NotChangableForVmInPoolListModel value)
    {
        privateDefaultHost = value;
    }

    private NotChangableForVmInPoolEntityModel privateisSmartcardEnabled;

    public EntityModel getIsSmartcardEnabled()
    {
        return privateisSmartcardEnabled;
    }

    private void setIsSmartcardEnabled(NotChangableForVmInPoolEntityModel value)
    {
        privateisSmartcardEnabled = value;
    }

    private NotChangableForVmInPoolEntityModel isConsoleDeviceEnabled;

    public EntityModel getIsConsoleDeviceEnabled() {
        return isConsoleDeviceEnabled;
    }

    private void setConsoleDeviceEnabled(NotChangableForVmInPoolEntityModel consoleDeviceEnabled) {
        this.isConsoleDeviceEnabled = consoleDeviceEnabled;
    }

    private NotChangableForVmInPoolEntityModel privateIsStateless;

    public EntityModel getIsStateless()
    {
        return privateIsStateless;
    }

    private void setIsStateless(NotChangableForVmInPoolEntityModel value)
    {
        privateIsStateless = value;
    }

    private NotChangableForVmInPoolEntityModel privateIsRunAndPause;

    public EntityModel getIsRunAndPause()
    {
        return privateIsRunAndPause;
    }

    private void setIsRunAndPause(NotChangableForVmInPoolEntityModel value)
    {
        privateIsRunAndPause = value;
    }

    private NotChangableForVmInPoolEntityModel privateIsDeleteProtected;

    public EntityModel getIsDeleteProtected() {
        return privateIsDeleteProtected;
    }

    public void setIsDeleteProtected(NotChangableForVmInPoolEntityModel deleteProtected) {
        this.privateIsDeleteProtected = deleteProtected;
    }

    private NotChangableForVmInPoolEntityModel copyPermissions;

    public EntityModel getCopyPermissions() {
        return copyPermissions;
    }

    private void setCopyPermissions(NotChangableForVmInPoolEntityModel copyPermissions) {
        this.copyPermissions = copyPermissions;
    }

    private EntityModel memoryBalloonDeviceEnabled;

    public EntityModel getMemoryBalloonDeviceEnabled() {
        return memoryBalloonDeviceEnabled;
    }

    public void setMemoryBalloonDeviceEnabled(EntityModel memoryBalloonDeviceEnabled) {
        this.memoryBalloonDeviceEnabled = memoryBalloonDeviceEnabled;
    }

    private NotChangableForVmInPoolListModel privateDisplayProtocol;

    public ListModel getDisplayProtocol()
    {
        return privateDisplayProtocol;
    }

    private void setDisplayProtocol(NotChangableForVmInPoolListModel value)
    {
        privateDisplayProtocol = value;
    }

    private NotChangableForVmInPoolEntityModel privateProvisioning;

    public EntityModel getProvisioning()
    {
        return privateProvisioning;
    }

    private void setProvisioning(NotChangableForVmInPoolEntityModel value)
    {
        privateProvisioning = value;
    }

    private NotChangableForVmInPoolEntityModel privateProvisioningThin_IsSelected;

    public EntityModel getProvisioningThin_IsSelected()
    {
        return privateProvisioningThin_IsSelected;
    }

    public void setProvisioningThin_IsSelected(NotChangableForVmInPoolEntityModel value)
    {
        privateProvisioningThin_IsSelected = value;
    }

    private NotChangableForVmInPoolEntityModel privateProvisioningClone_IsSelected;

    public EntityModel getProvisioningClone_IsSelected()
    {
        return privateProvisioningClone_IsSelected;
    }

    public void setProvisioningClone_IsSelected(NotChangableForVmInPoolEntityModel value)
    {
        privateProvisioningClone_IsSelected = value;
    }

    private EntityModel isVirtioScsiEnabled;

    public EntityModel getIsVirtioScsiEnabled() {
        return isVirtioScsiEnabled;
    }

    public void setIsVirtioScsiEnabled(EntityModel virtioScsiEnabled) {
        this.isVirtioScsiEnabled = virtioScsiEnabled;
    }

    private NotChangableForVmInPoolListModel privatePriority;

    public ListModel getPriority()
    {
        return privatePriority;
    }

    private void setPriority(NotChangableForVmInPoolListModel value)
    {
        privatePriority = value;
    }

    private NotChangableForVmInPoolEntityModel privateIsHighlyAvailable;

    public EntityModel getIsHighlyAvailable()
    {
        return privateIsHighlyAvailable;
    }

    private void setIsHighlyAvailable(NotChangableForVmInPoolEntityModel value)
    {
        privateIsHighlyAvailable = value;
    }

    private NotChangableForVmInPoolListModel privateFirstBootDevice;

    public ListModel getFirstBootDevice()
    {
        return privateFirstBootDevice;
    }

    private void setFirstBootDevice(NotChangableForVmInPoolListModel value)
    {
        privateFirstBootDevice = value;
    }

    private NotChangableForVmInPoolListModel privateSecondBootDevice;

    public ListModel getSecondBootDevice()
    {
        return privateSecondBootDevice;
    }

    private void setSecondBootDevice(NotChangableForVmInPoolListModel value)
    {
        privateSecondBootDevice = value;
    }

    private NotChangableForVmInPoolListModel privateCdImage;

    public ListModel getCdImage()
    {
        return privateCdImage;
    }

    private void setCdImage(NotChangableForVmInPoolListModel value)
    {
        privateCdImage = value;
    }

    private NotChangableForVmInPoolEntityModel cdAttached;

    public EntityModel getCdAttached() {
        return cdAttached;
    }

    public void setCdAttached(NotChangableForVmInPoolEntityModel value) {
        cdAttached = value;
    }

    private NotChangableForVmInPoolEntityModel privateInitrd_path;

    public EntityModel getInitrd_path()
    {
        return privateInitrd_path;
    }

    private void setInitrd_path(NotChangableForVmInPoolEntityModel value)
    {
        privateInitrd_path = value;
    }

    private NotChangableForVmInPoolEntityModel privateKernel_path;

    public EntityModel getKernel_path()
    {
        return privateKernel_path;
    }

    private void setKernel_path(NotChangableForVmInPoolEntityModel value)
    {
        privateKernel_path = value;
    }

    private NotChangableForVmInPoolEntityModel privateKernel_parameters;

    public EntityModel getKernel_parameters()
    {
        return privateKernel_parameters;
    }

    private void setKernel_parameters(NotChangableForVmInPoolEntityModel value)
    {
        privateKernel_parameters = value;
    }

    private NotChangableForVmInPoolEntityModel privateCustomProperties;

    public EntityModel getCustomProperties()
    {
        return privateCustomProperties;
    }

    private void setCustomProperties(NotChangableForVmInPoolEntityModel value)
    {
        privateCustomProperties = value;
    }

    private NotChangableForVmInPoolKeyValueModel customPropertySheet;

    public KeyValueModel getCustomPropertySheet() {
        return customPropertySheet;
    }

    public void setCustomPropertySheet(NotChangableForVmInPoolKeyValueModel customPropertySheet) {
        this.customPropertySheet = customPropertySheet;
    }

    private HashMap<Version, ArrayList<String>> privateCustomPropertiesKeysList;

    public HashMap<Version, ArrayList<String>> getCustomPropertiesKeysList()
    {
        return privateCustomPropertiesKeysList;
    }

    public void setCustomPropertiesKeysList(HashMap<Version, ArrayList<String>> value)
    {
        privateCustomPropertiesKeysList = value;
    }

    private NotChangableForVmInPoolEntityModel privateIsAutoAssign;

    public EntityModel getIsAutoAssign()
    {
        return privateIsAutoAssign;
    }

    public void setIsAutoAssign(NotChangableForVmInPoolEntityModel value)
    {
        privateIsAutoAssign = value;
    }

    private NotChangableForVmInPoolEntityModel hostCpu;

    public EntityModel getHostCpu() {
        return hostCpu;
    }

    public void setHostCpu(NotChangableForVmInPoolEntityModel hostCpu) {
        this.hostCpu = hostCpu;
    }

    private NotChangableForVmInPoolListModel migrationMode;

    public ListModel getMigrationMode()
    {
        return migrationMode;
    }

    public void setMigrationMode(NotChangableForVmInPoolListModel value)
    {
        migrationMode = value;
    }

    private NotChangableForVmInPoolEntityModel privateIsTemplatePublic;

    public EntityModel getIsTemplatePublic()
    {
        return privateIsTemplatePublic;
    }

    private void setIsTemplatePublic(NotChangableForVmInPoolEntityModel value)
    {
        privateIsTemplatePublic = value;
    }

    private boolean privateIsFirstRun;

    public boolean getIsFirstRun()
    {
        return privateIsFirstRun;
    }

    public void setIsFirstRun(boolean value)
    {
        privateIsFirstRun = value;
    }

    private List<DiskModel> disks;

    public List<DiskModel> getDisks()
    {
        return disks;
    }

    public void setDisks(List<DiskModel> value)
    {
        if (disks != value)
        {
            disks = value;
            onPropertyChanged(new PropertyChangedEventArgs("Disks")); //$NON-NLS-1$
        }
    }

    private DisksAllocationModel disksAllocationModel;

    public DisksAllocationModel getDisksAllocationModel()
    {
        return disksAllocationModel;
    }

    private void setDisksAllocationModel(DisksAllocationModel value)
    {
        disksAllocationModel = value;
    }

    private boolean isDisksAvailable;

    public boolean getIsDisksAvailable()
    {
        return isDisksAvailable;
    }

    public void setIsDisksAvailable(boolean value)
    {
        isDisksAvailable = value;
        onPropertyChanged(new PropertyChangedEventArgs("IsDisksAvailable")); //$NON-NLS-1$
    }

    private boolean isHostAvailable;

    public boolean getIsHostAvailable()
    {
        return isHostAvailable;
    }

    public void setIsHostAvailable(boolean value)
    {
        if (isHostAvailable != value)
        {
            isHostAvailable = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsHostAvailable")); //$NON-NLS-1$
        }
    }

    private boolean isCustomPropertiesTabAvailable;

    public boolean getIsCustomPropertiesTabAvailable()
    {
        return isCustomPropertiesTabAvailable;
    }

    public void setIsCustomPropertiesTabAvailable(boolean value)
    {
        if (isCustomPropertiesTabAvailable != value)
        {
            isCustomPropertiesTabAvailable = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsCustomPropertiesTabAvailable")); //$NON-NLS-1$
        }
    }

    private final VmModelBehaviorBase behavior;

    public VmModelBehaviorBase getBehavior() {
        return behavior;
    }

    private void setBehavior(VmModelBehaviorBase value) {
    }

    private int _minMemSize = 1;

    public int get_MinMemSize()
    {
        return _minMemSize;
    }

    public void set_MinMemSize(int value)
    {
        _minMemSize = value;
    }

    private int _maxMemSize32 = 20480;

    public int get_MaxMemSize32()
    {
        return _maxMemSize32;
    }

    public void set_MaxMemSize32(int value)
    {
        _maxMemSize32 = value;
    }

    private int _maxMemSize64 = 2097152;

    public int get_MaxMemSize64()
    {
        return _maxMemSize64;
    }

    public void set_MaxMemSize64(int value)
    {
        _maxMemSize64 = value;
    }

    private NotChangableForVmInPoolEntityModel cpuPinning;

    public EntityModel getCpuPinning() {
        return cpuPinning;
    }

    public void setCpuPinning(NotChangableForVmInPoolEntityModel cpuPinning) {
        this.cpuPinning = cpuPinning;
    }

    private NotChangableForVmInPoolEntityModel cpuSharesAmount;

    public EntityModel getCpuSharesAmount() {
        return cpuSharesAmount;
    }

    public void setCpuSharesAmount(NotChangableForVmInPoolEntityModel cpuSharesAmount) {
        this.cpuSharesAmount = cpuSharesAmount;
    }

    private NotChangableForVmInPoolListModel cpuSharesAmountSelection;

    public ListModel getCpuSharesAmountSelection() {
        return cpuSharesAmountSelection;
    }

    public void setCpuSharesAmountSelection(NotChangableForVmInPoolListModel cpuSharesAmountSelection) {
        this.cpuSharesAmountSelection = cpuSharesAmountSelection;
    }

    private ListModel vncKeyboardLayout;

    public ListModel getVncKeyboardLayout() {
        return vncKeyboardLayout;
    }

    public void setVncKeyboardLayout(ListModel vncKeyboardLayout) {
        this.vncKeyboardLayout = vncKeyboardLayout;
    }

    public UnitVmModel(VmModelBehaviorBase behavior)
    {
        Frontend.getQueryStartedEvent().addListener(this);
        Frontend.getQueryCompleteEvent().addListener(this);

        Frontend.Subscribe(new VdcQueryType[] { VdcQueryType.GetStorageDomainsByStoragePoolId,
                VdcQueryType.GetImagesListByStoragePoolId,
                VdcQueryType.GetDefaultTimeZone, VdcQueryType.GetStoragePoolsByClusterService,
                VdcQueryType.GetDomainList, VdcQueryType.GetConfigurationValue,
                VdcQueryType.GetVdsGroupsByStoragePoolId, VdcQueryType.GetVmTemplatesByStoragePoolId,
                VdcQueryType.GetVmTemplatesDisks, VdcQueryType.GetStorageDomainsByVmTemplateId,
                VdcQueryType.GetStorageDomainById, VdcQueryType.GetDataCentersWithPermittedActionOnClusters,
                VdcQueryType.GetClustersWithPermittedAction, VdcQueryType.GetVmTemplatesWithPermittedAction,
                VdcQueryType.GetVdsGroupById, VdcQueryType.GetStoragePoolById, VdcQueryType.GetAllDisksByVmId,
                VdcQueryType.GetVmTemplate, VdcQueryType.GetVmConfigurationBySnapshot, VdcQueryType.GetAllVdsGroups,
                VdcQueryType.GetPermittedStorageDomainsByStoragePoolId, VdcQueryType.GetHostsByClusterId,
                VdcQueryType.Search });

        this.behavior = behavior;
        this.behavior.setModel(this);

        setVnicProfiles(new ListModel());
        setNicsWithLogicalNetworks(new ListModel());
        setAdvancedMode(new EntityModel(false));
        setStorageDomain(new NotChangableForVmInPoolListModel());
        setName(new NotChangableForVmInPoolEntityModel());
        setNumOfMonitors(new NotChangableForVmInPoolListModel());
        setAllowConsoleReconnect(new NotChangableForVmInPoolEntityModel());
        setDescription(new NotChangableForVmInPoolEntityModel());
        setComment(new NotChangableForVmInPoolEntityModel());
        setDomain(new NotChangableForVmInPoolListModel());
        setMinAllocatedMemory(new NotChangableForVmInPoolEntityModel());
        setUsbPolicy(new NotChangableForVmInPoolListModel());
        setIsStateless(new NotChangableForVmInPoolEntityModel());
        setIsRunAndPause(new NotChangableForVmInPoolEntityModel());
        setIsSmartcardEnabled(new NotChangableForVmInPoolEntityModel());
        setIsDeleteProtected(new NotChangableForVmInPoolEntityModel());
        setConsoleDeviceEnabled(new NotChangableForVmInPoolEntityModel());
        setCopyPermissions(new NotChangableForVmInPoolEntityModel());
        // by default not available - only for new VM
        getCopyPermissions().setIsAvailable(false);
        getCopyPermissions().setEntity(false);
        setVncKeyboardLayout(new NotChangableForVmInPoolListModel());
        setVmType(new NotChangableForVmInPoolListModel());
        getVmType().setItems(Arrays.asList(VmType.Desktop, VmType.Server));
        getVmType().setSelectedItem(VmType.Server);
        getVmType().setIsChangable(false);
        getVmType().getSelectedItemChangedEvent().addListener(this);

        setCdImage(new NotChangableForVmInPoolListModel());
        getCdImage().setIsChangable(false);

        setMemoryBalloonDeviceEnabled(new EntityModel());
        getMemoryBalloonDeviceEnabled().setEntity(true);
        getMemoryBalloonDeviceEnabled().setIsAvailable(false);


        setCdAttached(new NotChangableForVmInPoolEntityModel());
        getCdAttached().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {

                getCdImage().setIsChangable((Boolean) getCdAttached().getEntity());
            }
        });
        getCdAttached().setEntity(false);

        setIsHighlyAvailable(new NotChangableForVmInPoolEntityModel());
        getIsHighlyAvailable().getEntityChangedEvent().addListener(this);
        setIsTemplatePublic(new NotChangableForVmInPoolEntityModel());
        setKernel_parameters(new NotChangableForVmInPoolEntityModel());
        setKernel_path(new NotChangableForVmInPoolEntityModel());
        setInitrd_path(new NotChangableForVmInPoolEntityModel());
        setCustomProperties(new NotChangableForVmInPoolEntityModel());
        setCustomPropertySheet(new NotChangableForVmInPoolKeyValueModel());
        setDisplayProtocol(new NotChangableForVmInPoolListModel());
        setSecondBootDevice(new NotChangableForVmInPoolListModel());
        setPriority(new NotChangableForVmInPoolListModel());

        setTemplate(new NotChangableForVmInPoolListModel());
        getTemplate().getSelectedItemChangedEvent().addListener(this);

        setQuota(new NotChangableForVmInPoolListModel());
        getQuota().setIsAvailable(false);

        setDataCenterWithClustersList(new NotChangableForVmInPoolListModel());
        getDataCenterWithClustersList().getSelectedItemChangedEvent().addListener(this);

        setTimeZone(new NotChangableForVmInPoolListModel());
        getTimeZone().getSelectedItemChangedEvent().addListener(this);

        setDefaultHost(new NotChangableForVmInPoolListModel());
        getDefaultHost().getSelectedItemChangedEvent().addListener(this);

        setOSType(new NotChangableForVmInPoolListModel());
        getOSType().getSelectedItemChangedEvent().addListener(this);

        setFirstBootDevice(new NotChangableForVmInPoolListModel());
        getFirstBootDevice().getSelectedItemChangedEvent().addListener(this);

        setProvisioning(new NotChangableForVmInPoolEntityModel());
        getProvisioning().getEntityChangedEvent().addListener(this);

        setMemSize(new NotChangableForVmInPoolEntityModel());
        getMemSize().getEntityChangedEvent().addListener(this);

        setTotalCPUCores(new NotChangableForVmInPoolEntityModel());
        getTotalCPUCores().getEntityChangedEvent().addListener(this);

        setNumOfSockets(new NotChangableForVmInPoolListModel());
        getNumOfSockets().getSelectedItemChangedEvent().addListener(this);

        setCoresPerSocket(new NotChangableForVmInPoolListModel());
        getCoresPerSocket().getSelectedItemChangedEvent().addListener(this);

        setMigrationMode(new NotChangableForVmInPoolListModel());
        getMigrationMode().getSelectedItemChangedEvent().addListener(this);

        setHostCpu(new NotChangableForVmInPoolEntityModel());
        getHostCpu().getEntityChangedEvent().addListener(this);

        setWatchdogAction(new NotChangableForVmInPoolListModel());
        getWatchdogAction().getEntityChangedEvent().addListener(this);
        ArrayList<String> watchDogActions = new ArrayList<String>();
        for (VmWatchdogAction action : VmWatchdogAction.values()) {
            watchDogActions.add(EnumTranslator.createAndTranslate(action));
        }
        getWatchdogAction().setItems(watchDogActions);

        setWatchdogModel(new NotChangableForVmInPoolListModel());
        getWatchdogModel().getEntityChangedEvent().addListener(this);
        ArrayList<String> watchDogModels = new ArrayList<String>();
        watchDogModels.add(null);
        for (VmWatchdogType type : VmWatchdogType.values()) {
            watchDogModels.add(EnumTranslator.createAndTranslate(type));
        }
        getWatchdogModel().setItems(watchDogModels);

        setIsAutoAssign(new NotChangableForVmInPoolEntityModel());
        getIsAutoAssign().getEntityChangedEvent().addListener(this);

        setIsTemplatePublic(new NotChangableForVmInPoolEntityModel());
        getIsTemplatePublic().getEntityChangedEvent().addListener(this);

        setIsHostTabValid(true);
        setIsCustomPropertiesTabAvailable(true);
        setIsCustomPropertiesTabValid(getIsHostTabValid());
        setIsBootSequenceTabValid(getIsCustomPropertiesTabValid());
        setIsAllocationTabValid(getIsBootSequenceTabValid());
        setIsDisplayTabValid(getIsAllocationTabValid());
        setIsFirstRunTabValid(getIsDisplayTabValid());
        setIsGeneralTabValid(getIsFirstRunTabValid());

        // NOTE: This is because currently the auto generated view code tries to register events of pooltype for
        // VM/Template views as this model is shared across VM/Template/Pool models
        setPoolType(new NotChangableForVmInPoolListModel());

        setNumOfDesktops(new NotChangableForVmInPoolEntityModel());
        getNumOfDesktops().setEntity(0);
        getNumOfDesktops().setIsAvailable(false);

        setAssignedVms(new NotChangableForVmInPoolEntityModel());
        getAssignedVms().setEntity(0);
        getAssignedVms().setIsAvailable(false);
        // Assigned VMs count is always read-only.
        getAssignedVms().setIsChangable(false);

        setPrestartedVms(new NotChangableForVmInPoolEntityModel());
        getPrestartedVms().setEntity(0);
        getPrestartedVms().setIsAvailable(false);

        setMaxAssignedVmsPerUser(new NotChangableForVmInPoolEntityModel());
        getMaxAssignedVmsPerUser().setEntity(1);
        getMaxAssignedVmsPerUser().setIsAvailable(false);

        setDisksAllocationModel(new DisksAllocationModel());

        setIsVirtioScsiEnabled(new EntityModel());
        getIsVirtioScsiEnabled().setEntity(false);
        getIsVirtioScsiEnabled().setIsAvailable(false);

        setProvisioningClone_IsSelected(new NotChangableForVmInPoolEntityModel());
        getProvisioningClone_IsSelected().getEntityChangedEvent().addListener(this);

        setProvisioningThin_IsSelected(new NotChangableForVmInPoolEntityModel());
        getProvisioningThin_IsSelected().getEntityChangedEvent().addListener(this);

        setCpuPinning(new NotChangableForVmInPoolEntityModel());
        getCpuPinning().setEntity("");
        getCpuPinning().setIsChangable(false);

        setCpuSharesAmount(new NotChangableForVmInPoolEntityModel());
        getCpuSharesAmount().setEntity("");
        getCpuSharesAmount().setIsChangable(false);

        setCpuSharesAmountSelection(new NotChangableForVmInPoolListModel());
        getCpuSharesAmountSelection().setItems(Arrays.asList(CpuSharesAmount.values()));
        getCpuSharesAmountSelection().getEntityChangedEvent().addListener(this);
        getCpuSharesAmountSelection().getSelectedItemChangedEvent().addListener(this);
        getCpuSharesAmountSelection().setSelectedItem(CpuSharesAmount.DISABLED);

        setIsSoundcardEnabled(new NotChangableForVmInPoolEntityModel());
        getIsSoundcardEnabled().setEntity(false);
        getIsSoundcardEnabled().setIsChangable(false);

        setIsSingleQxlEnabled(new NotChangableForVmInPoolEntityModel());
        getBehavior().enableSinglePCI(false);

        setEditingEnabled(new EntityModel());
        getEditingEnabled().setEntity(true);
    }

    public void initialize(SystemTreeItemModel SystemTreeSelectedItem)
    {
        super.initialize();

        setHash(getHashName() + new Date());

        getMemSize().setEntity(256);
        getMinAllocatedMemory().setEntity(256);
        getIsStateless().setEntity(false);
        getIsRunAndPause().setEntity(false);
        getIsSmartcardEnabled().setEntity(false);
        isConsoleDeviceEnabled.setEntity(false);
        getIsHighlyAvailable().setEntity(false);
        getIsAutoAssign().setEntity(true);
        getIsTemplatePublic().setEntity(true);
        getBehavior().enableSinglePCI(false);

        getHostCpu().setEntity(false);
        getMigrationMode().setIsChangable(true);

        getCdImage().setIsChangable(false);

        initOSType();
        initDisplayProtocol();
        initFirstBootDevice();
        initNumOfMonitors();
        initAllowConsoleReconnect();
        initMinimalVmMemSize();
        initMaximalVmMemSize32OS();
        initMigrationMode();
        initVncKeyboardLayout();

        behavior.initialize(SystemTreeSelectedItem);
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(Frontend.QueryStartedEventDefinition)
                && StringHelper.stringsEqual(Frontend.getCurrentContext(), getHash()))
        {
            frontend_QueryStarted();
        }
        else if (ev.matchesDefinition(Frontend.QueryCompleteEventDefinition)
                && StringHelper.stringsEqual(Frontend.getCurrentContext(), getHash()))
        {
            frontend_QueryComplete();
        }
        else if (ev.matchesDefinition(ListModel.selectedItemChangedEventDefinition))
        {
            if (sender == getVmType()) {
                vmTypeChanged();
            } else if (sender == getDataCenterWithClustersList())
            {
                dataCenterWithClusterSelectedItemChanged(sender, args);
                initUsbPolicy();
            }
            else if (sender == getTemplate())
            {
                template_SelectedItemChanged(sender, args);
            }
            else if (sender == getTimeZone())
            {
                timeZone_SelectedItemChanged(sender, args);
            }
            else if (sender == getDefaultHost())
            {
                defaultHost_SelectedItemChanged(sender, args);
            }
            else if (sender == getOSType())
            {
                oSType_SelectedItemChanged(sender, args);
                initUsbPolicy();
            }
            else if (sender == getFirstBootDevice())
            {
                firstBootDevice_SelectedItemChanged(sender, args);
            }
            else if (sender == getDisplayProtocol())
            {
                displayProtocol_SelectedItemChanged(sender, args);
                initUsbPolicy();
            }
            else if (sender == getNumOfSockets())
            {
                numOfSockets_EntityChanged(sender, args);
            }
            else if (sender == getCoresPerSocket())
            {
                coresPerSocket_EntityChanged(sender, args);
            }
            else if (sender == getMigrationMode())
            {
                behavior.updateUseHostCpuAvailability();
                behavior.updateCpuPinningVisibility();
                behavior.updateHaAvailability();
            }
            else if (sender == getCpuSharesAmountSelection())
            {
                behavior.updateCpuSharesAmountChangeability();
            }
        }
        else if (ev.matchesDefinition(EntityModel.EntityChangedEventDefinition))
        {
            if (sender == getMemSize())
            {
                memSize_EntityChanged(sender, args);
            }
            else if (sender == getTotalCPUCores())
            {
                totalCPUCores_EntityChanged(sender, args);
            }
            else if (sender == getIsAutoAssign())
            {
                behavior.updateUseHostCpuAvailability();
                behavior.updateCpuPinningVisibility();
                behavior.updateHaAvailability();
            }
            else if (sender == getProvisioning())
            {
                provisioning_SelectedItemChanged(sender, args);
            }
            else if (sender == getProvisioningThin_IsSelected())
            {
                if ((Boolean) getProvisioningThin_IsSelected().getEntity()) {
                    getProvisioning().setEntity(false);
                }
            }
            else if (sender == getProvisioningClone_IsSelected())
            {
                if ((Boolean) getProvisioningClone_IsSelected().getEntity()) {
                    getProvisioning().setEntity(true);
                }
            } else if (sender == getWatchdogModel()) {
                WatchdogModel_EntityChanged(sender, args);
            } else if (sender == getIsHighlyAvailable()) {
                behavior.updateMigrationAvailability();
            }
        }
    }

    private void vmTypeChanged() {
        behavior.vmTypeChanged(((VmType) getVmType().getSelectedItem()));
    }

    private void WatchdogModel_EntityChanged(Object sender, EventArgs args) {
        if ("".equals(getWatchdogModel().getEntity())) {
            getWatchdogAction().setIsChangable(false);
            getWatchdogAction().setSelectedItem(""); //$NON-NLS-1$
        } else {
            getWatchdogAction().setIsChangable(true);
        }
    }

    private int queryCounter;

    private void frontend_QueryStarted()
    {
        queryCounter++;
        if (getProgress() == null)
        {
            startProgress(null);
        }
    }

    private void frontend_QueryComplete()
    {
        queryCounter--;
        if (queryCounter == 0)
        {
            stopProgress();
        }
    }

    protected void initNumOfMonitors()
    {
        AsyncDataProvider.getNumOfMonitorList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        UnitVmModel model = (UnitVmModel) target;
                        Integer oldNumOfMonitors = null;
                        if (model.getNumOfMonitors().getSelectedItem() != null)
                        {
                            oldNumOfMonitors = (Integer) model.getNumOfMonitors().getSelectedItem();
                        }
                        ArrayList<Integer> numOfMonitors = (ArrayList<Integer>) returnValue;
                        model.getNumOfMonitors().setItems(numOfMonitors);
                        if (oldNumOfMonitors != null)
                        {
                            model.getNumOfMonitors().setSelectedItem(oldNumOfMonitors);
                        }

                    }
                }, getHash()));

    }

    protected void initAllowConsoleReconnect() {
        getAllowConsoleReconnect().setEntity(getVmType().getSelectedItem() == VmType.Server);
    }

    private void initOSType() {
        getOSType().setItems(AsyncDataProvider.getOsIds());
        getOSType().setSelectedItem(OsRepository.DEFAULT_OS);
    }

    private void initUsbPolicy() {
        VDSGroup cluster = getSelectedCluster();
        Integer osType = (Integer) getOSType().getSelectedItem();
        DisplayType displayType = (DisplayType) (getDisplayProtocol().getSelectedItem() != null ?
                ((EntityModel) getDisplayProtocol().getSelectedItem()).getEntity() : null);

        if (osType == null || cluster == null || displayType == null) {
            return;
        }

        getUsbPolicy().setIsChangable(true);
        if (Version.v3_1.compareTo(cluster.getcompatibility_version()) > 0) {
            if (AsyncDataProvider.isWindowsOsType(osType)) {
                getUsbPolicy().setItems(Arrays.asList(
                        UsbPolicy.DISABLED,
                        UsbPolicy.ENABLED_LEGACY
                        ));
            } else {
                getUsbPolicy().setItems(Arrays.asList(UsbPolicy.DISABLED));
                getUsbPolicy().setSelectedItem(UsbPolicy.DISABLED);
                getUsbPolicy().setIsChangable(false);
            }
        }

        if (Version.v3_1.compareTo(cluster.getcompatibility_version()) <= 0) {
            if (AsyncDataProvider.isLinuxOsType(osType)) {
                getUsbPolicy().setItems(Arrays.asList(
                        UsbPolicy.DISABLED,
                        UsbPolicy.ENABLED_NATIVE
                        ));
            } else {
                getUsbPolicy().setItems(
                        Arrays.asList(
                                UsbPolicy.DISABLED,
                                UsbPolicy.ENABLED_LEGACY,
                                UsbPolicy.ENABLED_NATIVE
                                ));
            }
        }

        if (displayType != DisplayType.qxl) {
            getUsbPolicy().setIsChangable(false);
        }

        getUsbPolicy().setSelectedItem(UsbPolicy.DISABLED);
    }

    private void initMinimalVmMemSize()
    {
        AsyncDataProvider.getMinimalVmMemSize(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        UnitVmModel vmModel = (UnitVmModel) target;
                        vmModel.set_MinMemSize((Integer) returnValue);

                    }
                }, getHash()));
    }

    private void initMaximalVmMemSize32OS()
    {
        AsyncDataProvider.getMaximalVmMemSize32OS(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        UnitVmModel vmModel = (UnitVmModel) target;
                        vmModel.set_MaxMemSize32((Integer) returnValue);

                    }
                }, getHash()));
    }

    private void updateMaximalVmMemSize()
    {
        DataCenterWithCluster dataCenterWithCluster =
                (DataCenterWithCluster) getDataCenterWithClustersList().getSelectedItem();
        if (dataCenterWithCluster == null) {
            return;
        }

        VDSGroup cluster = dataCenterWithCluster.getCluster();

        if (cluster != null)
        {
            AsyncDataProvider.getMaximalVmMemSize64OS(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target, Object returnValue) {

                            UnitVmModel vmModel = (UnitVmModel) target;
                            vmModel.set_MaxMemSize64((Integer) returnValue);

                        }
                    }, getHash()), cluster.getcompatibility_version().toString());
        }
    }

    private void initDisplayProtocol()
    {
        ArrayList<EntityModel> displayProtocolOptions = new ArrayList<EntityModel>();

        EntityModel spiceProtocol = new EntityModel();
        spiceProtocol.setTitle(ConstantsManager.getInstance().getConstants().spiceTitle());
        spiceProtocol.setEntity(DisplayType.qxl);

        EntityModel vncProtocol = new EntityModel();
        vncProtocol.setTitle(ConstantsManager.getInstance().getConstants().VNCTitle());
        vncProtocol.setEntity(DisplayType.vnc);

        displayProtocolOptions.add(spiceProtocol);
        displayProtocolOptions.add(vncProtocol);
        getDisplayProtocol().setItems(displayProtocolOptions);

        getDisplayProtocol().getSelectedItemChangedEvent().addListener(this);
    }

    private void initFirstBootDevice()
    {
        EntityModel tempVar = new EntityModel();
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().hardDiskTitle());
        tempVar.setEntity(BootSequence.C);
        EntityModel hardDiskOption = tempVar;

        ArrayList<EntityModel> firstBootDeviceItems = new ArrayList<EntityModel>();
        firstBootDeviceItems.add(hardDiskOption);
        EntityModel tempVar2 = new EntityModel();
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cdromTitle());
        tempVar2.setEntity(BootSequence.D);
        firstBootDeviceItems.add(tempVar2);
        EntityModel tempVar3 = new EntityModel();
        tempVar3.setTitle(ConstantsManager.getInstance().getConstants().networkPXETitle());
        tempVar3.setEntity(BootSequence.N);
        firstBootDeviceItems.add(tempVar3);
        getFirstBootDevice().setItems(firstBootDeviceItems);
        getFirstBootDevice().setSelectedItem(hardDiskOption);
    }

    private void initMigrationMode() {
        getMigrationMode().setItems(Arrays.asList(MigrationSupport.values()));
    }

    private void initVncKeyboardLayout() {

        final List<String> layouts =
                (List<String>) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.VncKeyboardLayoutValidValues);
        final ArrayList<String> vncKeyboardLayoutItems = new ArrayList<String>();
        vncKeyboardLayoutItems.add(null); // null value means the global VncKeyboardLayout from vdc_options will be used
        vncKeyboardLayoutItems.addAll(layouts);
        getVncKeyboardLayout().setItems(vncKeyboardLayoutItems);

        getVncKeyboardLayout().setIsAvailable(isVncSelected());
    }

    private void dataCenterWithClusterSelectedItemChanged(Object sender, EventArgs args)
    {
        behavior.dataCenterWithClusterSelectedItemChanged();

        DataCenterWithCluster dataCenterWithCluster =
                (DataCenterWithCluster) getDataCenterWithClustersList().getSelectedItem();
        if (dataCenterWithCluster != null && dataCenterWithCluster.getDataCenter() != null) {
            getDisksAllocationModel().setQuotaEnforcementType(dataCenterWithCluster.getDataCenter()
                    .getQuotaEnforcementType());
        }

        updateMaximalVmMemSize();
        handleQxlClusterLevel();
    }

    private void handleQxlClusterLevel() {
        // Enable Single PCI only on cluster 3.3 and high and on Linux OS
        boolean isLinux = getIsLinuxOS();
        boolean isQxl = getDisplayType() == DisplayType.qxl;
        boolean clusterSupportsSinglePci = getSelectedCluster() != null &&
        Version.v3_3.compareTo(getSelectedCluster().getcompatibility_version()) <= 0;

        getBehavior().enableSinglePCI(isLinux && isQxl && clusterSupportsSinglePci);
    }

    private void template_SelectedItemChanged(Object sender, EventArgs args)
    {
        behavior.template_SelectedItemChanged();
    }

    private void timeZone_SelectedItemChanged(Object sender, EventArgs args)
    {
    }

    private void defaultHost_SelectedItemChanged(Object sender, EventArgs args)
    {
        behavior.defaultHost_SelectedItemChanged();
    }

    private void oSType_SelectedItemChanged(Object sender, EventArgs args)
    {
        Integer osType = (Integer) getOSType().getSelectedItem();

        setIsWindowsOS(AsyncDataProvider.isWindowsOsType(osType));
        setIsLinuxOS(AsyncDataProvider.isLinuxOsType(osType));

        getInitrd_path().setIsChangable(getIsLinuxOS());
        getInitrd_path().setIsAvailable(getIsLinuxOS());

        getKernel_path().setIsChangable(getIsLinuxOS());
        getKernel_path().setIsAvailable(getIsLinuxOS());

        getKernel_parameters().setIsChangable(getIsLinuxOS());
        getKernel_parameters().setIsAvailable(getIsLinuxOS());

        getDomain().setIsChangable(getIsWindowsOS());

        getBehavior().updateDefaultTimeZone();

        handleQxlClusterLevel();
    }

    private void firstBootDevice_SelectedItemChanged(Object sender, EventArgs args)
    {
        EntityModel entityModel = (EntityModel) getFirstBootDevice().getSelectedItem();
        BootSequence firstDevice = (BootSequence) entityModel.getEntity();

        ArrayList<EntityModel> list = new ArrayList<EntityModel>();
        for (Object item : getFirstBootDevice().getItems())
        {
            EntityModel a = (EntityModel) item;
            if ((BootSequence) a.getEntity() != firstDevice)
            {
                list.add(a);
            }
        }

        EntityModel tempVar = new EntityModel();
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().noneTitle());
        EntityModel noneOption = tempVar;

        list.add(0, noneOption);

        getSecondBootDevice().setItems(list);
        getSecondBootDevice().setSelectedItem(noneOption);
    }

    private void provisioning_SelectedItemChanged(Object sender, EventArgs args)
    {
        behavior.provisioning_SelectedItemChanged();
    }

    private DisplayType getDisplayType() {
        EntityModel entityModel = (EntityModel) getDisplayProtocol().getSelectedItem();
        if (entityModel == null)
        {
            return null;
        }
        return (DisplayType) entityModel.getEntity();
    }

    private void displayProtocol_SelectedItemChanged(Object sender, EventArgs args)
    {
        if (getDisplayType() == null)
        {
            return;
        }
        DisplayType type = getDisplayType();

        if (type == DisplayType.vnc)
        {
            getUsbPolicy().setSelectedItem(org.ovirt.engine.core.common.businessentities.UsbPolicy.DISABLED);
            getIsSmartcardEnabled().setEntity(false);
        }

        handleQxlClusterLevel();

        getUsbPolicy().setIsChangable(type == DisplayType.qxl);
        getIsSmartcardEnabled().setIsChangable(type == DisplayType.qxl);

        getVncKeyboardLayout().setIsAvailable(type == DisplayType.vnc);

        updateNumOfMonitors();
    }

    private void memSize_EntityChanged(Object sender, EventArgs args)
    {
        behavior.updateMinAllocatedMemory();
    }

    private void numOfSockets_EntityChanged(Object sender, EventArgs args)
    {
        behavior.numOfSocketChanged();
    }

    private void totalCPUCores_EntityChanged(Object sender, EventArgs args) {
        // do not listen on changes while the totalCpuCoresChanged is adjusting them
        getNumOfSockets().getSelectedItemChangedEvent().removeListener(this);
        getTotalCPUCores().getEntityChangedEvent().removeListener(this);
        getCoresPerSocket().getSelectedItemChangedEvent().removeListener(this);

        behavior.totalCpuCoresChanged();

        // start listening again
        getTotalCPUCores().getEntityChangedEvent().addListener(this);
        getNumOfSockets().getSelectedItemChangedEvent().addListener(this);
        getCoresPerSocket().getSelectedItemChangedEvent().addListener(this);
    }

    private void coresPerSocket_EntityChanged(Object sender, EventArgs args) {
        behavior.coresPerSocketChanged();
    }

    private boolean isVncSelected() {
        boolean isVnc = false;

        if (getDisplayProtocol().getSelectedItem() != null)
        {
            DisplayType displayType = (DisplayType) ((EntityModel) getDisplayProtocol().getSelectedItem()).getEntity();
            isVnc = displayType == DisplayType.vnc;
        }

        return isVnc;
    }

    private void updateNumOfMonitors()
    {
        if (isVncSelected())
        {
            getNumOfMonitors().setSelectedItem(1);
            getNumOfMonitors().setIsChangable(false);
        } else {
            getNumOfMonitors().setIsChangable(true);
        }
    }

    public BootSequence getBootSequence()
    {
        EntityModel firstSelectedItem = (EntityModel) getFirstBootDevice().getSelectedItem();
        EntityModel secondSelectedItem = (EntityModel) getSecondBootDevice().getSelectedItem();

        String firstSelectedString =
                firstSelectedItem.getEntity() == null ? "" : firstSelectedItem.getEntity().toString(); //$NON-NLS-1$
        String secondSelectedString =
                secondSelectedItem.getEntity() == null ? "" : secondSelectedItem.getEntity().toString(); //$NON-NLS-1$

        return BootSequence.valueOf(firstSelectedString + secondSelectedString);
    }

    public void setBootSequence(BootSequence value)
    {
        ArrayList<BootSequence> items = new ArrayList<BootSequence>();
        for (char a : value.toString().toCharArray())
        {
            items.add(BootSequence.valueOf(String.valueOf(a)));
        }

        Object firstBootDevice = null;
        for (Object item : getFirstBootDevice().getItems())
        {
            EntityModel a = (EntityModel) item;
            if ((BootSequence) a.getEntity() == Linq.firstOrDefault(items))
            {
                firstBootDevice = a;
            }
        }
        getFirstBootDevice().setSelectedItem(firstBootDevice);

        ArrayList<EntityModel> secondDeviceOptions =
                Linq.<EntityModel> cast(getSecondBootDevice().getItems());

        if (items.size() > 1)
        {
            BootSequence last = items.get(items.size() - 1);
            for (EntityModel a : secondDeviceOptions)
            {
                if (a.getEntity() != null && (BootSequence) a.getEntity() == last)
                {
                    getSecondBootDevice().setSelectedItem(a);
                    break;
                }
            }
        }
        else
        {
            for (EntityModel a : secondDeviceOptions)
            {
                if (a.getEntity() == null)
                {
                    getSecondBootDevice().setSelectedItem(a);
                    break;
                }
            }
        }
    }

    public void setDataCentersAndClusters(UnitVmModel model,
            List<StoragePool> dataCenters,
            List<VDSGroup> clusters,
            Guid selectedCluster) {

        if (model.getBehavior().getSystemTreeSelectedItem() != null
                && model.getBehavior().getSystemTreeSelectedItem().getType() != SystemTreeItemType.System) {
            setupDataCenterWithClustersFromSystemTree(model, dataCenters, clusters, selectedCluster);
        } else {
            setupDataCenterWithClusters(model, dataCenters, clusters, selectedCluster);
        }

    }

    protected void setupDataCenterWithClustersFromSystemTree(UnitVmModel model,
            List<StoragePool> dataCenters,
            List<VDSGroup> clusters,
            Guid selectedCluster) {

        StoragePool dataCenter = getDataCenterAccordingSystemTree(model, dataCenters);

        // the dataCenters are the entities just downloaded from server while the dataCenter can be a cached one from the system tree
        dataCenter = dataCenter == null ? null : findDataCenterById(dataCenters, dataCenter.getId());

        List<VDSGroup> possibleClusters = getClusterAccordingSystemTree(model, clusters);
        if (dataCenter == null || possibleClusters == null) {
            getDataCenterWithClustersList().setIsChangable(false);
            return;
        }

        List<DataCenterWithCluster> dataCentersWithClusters =
                new ArrayList<DataCenterWithCluster>();

        for (VDSGroup cluster : possibleClusters) {
            if (cluster.getStoragePoolId() != null && cluster.getStoragePoolId().equals(dataCenter.getId())) {
                dataCentersWithClusters.add(new DataCenterWithCluster(dataCenter, cluster));
            }
        }
        getDataCenterWithClustersList().setItems(dataCentersWithClusters);

        selectDataCenterWithCluster(model, selectedCluster, dataCentersWithClusters);
    }

    protected void setupDataCenterWithClusters(UnitVmModel model,
            List<StoragePool> dataCenters,
            List<VDSGroup> clusters,
            Guid selectedCluster) {

        Map<Guid, List<VDSGroup>> dataCenterToCluster = new HashMap<Guid, List<VDSGroup>>();
        for (VDSGroup cluster : clusters) {
            if (cluster.getStoragePoolId() == null) {
                continue;
            }

            if (!dataCenterToCluster.containsKey(cluster.getStoragePoolId())) {
                dataCenterToCluster.put(cluster.getStoragePoolId(), new ArrayList<VDSGroup>());
            }
            dataCenterToCluster.get(cluster.getStoragePoolId()).add(cluster);
        }

        List<DataCenterWithCluster> dataCentersWithClusters =
                new ArrayList<DataCenterWithCluster>();

        for (StoragePool dataCenter : dataCenters) {
            for (VDSGroup cluster : dataCenterToCluster.get(dataCenter.getId())) {
                dataCentersWithClusters.add(new DataCenterWithCluster(dataCenter, cluster));
            }
        }
        getDataCenterWithClustersList().setItems(dataCentersWithClusters);

        selectDataCenterWithCluster(model, selectedCluster, dataCentersWithClusters);
    }

    protected void selectDataCenterWithCluster(UnitVmModel model,
            Guid selectedCluster,
            List<DataCenterWithCluster> dataCentersWithClusters) {
        if (selectedCluster == null) {
            getDataCenterWithClustersList().setSelectedItem(Linq.firstOrDefault(dataCentersWithClusters));
        } else {
            model.getDataCenterWithClustersList().setSelectedItem(Linq.firstOrDefault(dataCentersWithClusters,
                    new Linq.DataCenterWithClusterAccordingClusterPredicate((Guid) selectedCluster)));
        }
    }

    private StoragePool getDataCenterAccordingSystemTree(UnitVmModel model, List<StoragePool> list) {
        if (model.getBehavior().getSystemTreeSelectedItem() != null
                && model.getBehavior().getSystemTreeSelectedItem().getType() != SystemTreeItemType.System)
        {
            switch (model.getBehavior().getSystemTreeSelectedItem().getType())
            {
            case Templates:
            case DataCenter:
                return (StoragePool) model.getBehavior().getSystemTreeSelectedItem().getEntity();
            case Cluster:
            case Cluster_Gluster:
            case VMs:
                VDSGroup cluster = (VDSGroup) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                if (cluster.supportsVirtService()) {
                    return findDataCenterById(list, cluster.getStoragePoolId());
                }
                break;

            case Host:
                VDS host = (VDS) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                return findDataCenterById(list, host.getStoragePoolId());

            case Storage:
                StorageDomain storage = (StorageDomain) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                return findDataCenterById(list, storage.getStoragePoolId());
            }
        }
        return null;
    }

    private StoragePool findDataCenterById(List<StoragePool> list, Guid id) {
        if (id == null) {
            return null;
        }

        for (StoragePool dc : list) {
            if (dc.getId().equals(id)) {
                return dc;
            }
        }

        return null;
    }

    private List<VDSGroup> getClusterAccordingSystemTree(UnitVmModel model, List<VDSGroup> clusters) {
        if (behavior.getSystemTreeSelectedItem() != null
                && behavior.getSystemTreeSelectedItem().getType() != SystemTreeItemType.System)
        {
            switch (model.getBehavior().getSystemTreeSelectedItem().getType())
            {
            case Cluster:
            case VMs:
                VDSGroup cluster = (VDSGroup) behavior.getSystemTreeSelectedItem().getEntity();
                return Arrays.asList(cluster);

            case Host:
                VDS host = (VDS) behavior.getSystemTreeSelectedItem().getEntity();
                for (VDSGroup iterCluster : clusters) {
                    if (iterCluster.getId().equals(host.getVdsGroupId())) {
                        return Arrays.asList(iterCluster);
                    }
                }
                break;
            default:
                return clusters;
            }
        }

        return null;
    }

    public boolean validate() {
        getDataCenterWithClustersList().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        getMemSize().validateEntity(new IValidation[] { new ByteSizeValidation() });
        getMinAllocatedMemory().validateEntity(new IValidation[] { new ByteSizeValidation() });
        getOSType().validateSelectedItem(new NotEmptyValidation[] { new NotEmptyValidation() });

        DataCenterWithCluster dataCenterWithCluster =
                (DataCenterWithCluster) getDataCenterWithClustersList().getSelectedItem();

        StoragePool dataCenter =
                dataCenterWithCluster == null ? null : dataCenterWithCluster.getDataCenter();
        if (dataCenter != null && dataCenter.getQuotaEnforcementType() == QuotaEnforcementTypeEnum.HARD_ENFORCEMENT) {
            getQuota().validateSelectedItem(new IValidation[] { new NotEmptyQuotaValidation() });
        }

        getTotalCPUCores().validateEntity(new IValidation[] {
                new NotEmptyValidation(),
                new IntegerValidation(1, behavior.maxCpus),
                new TotalCpuCoresComposableValidation() });

        if (getOSType().getIsValid()) {
            Integer osType = (Integer) getOSType().getSelectedItem();
            getName().validateEntity(
                    new IValidation[] {
                            new NotEmptyValidation(),
                            new LengthValidation(
                                    (getBehavior() instanceof TemplateVmModelBehavior || getBehavior() instanceof NewTemplateVmModelBehavior)
                                            ? VM_TEMPLATE_NAME_MAX_LIMIT
                                            : AsyncDataProvider.isWindowsOsType(osType) ? AsyncDataProvider.getMaxVmNameLengthWin()
                                                    : AsyncDataProvider.getMaxVmNameLengthNonWin()),
                            isPoolTabValid ? new PoolNameValidation() : new I18NNameValidation()
                    });

            getDescription().validateEntity(
                    new IValidation[] {
                            new LengthValidation(DESCRIPTION_MAX_LIMIT),
                            new SpecialAsciiI18NOrNoneValidation()
                    });

            AsyncQuery asyncQuery = new AsyncQuery();
            asyncQuery.setModel(this);
            asyncQuery.asyncCallback = new INewAsyncCallback() {
                @Override
                public void onSuccess(Object model, Object returnValue) {
                    validateMemorySize(getMemSize(), (Integer)((VdcQueryReturnValue)returnValue).getReturnValue(), _minMemSize);
                    if (!(((UnitVmModel)model).getBehavior() instanceof TemplateVmModelBehavior)) {
                        // Minimum 'Physical Memory Guaranteed' is 1MB
                        validateMemorySize(getMinAllocatedMemory(), (Integer) getMemSize().getEntity(), 1);
                    }
                }
            };

            if (getSelectedCluster() != null) {
                AsyncDataProvider.getOsMaxRam(osType, ((VDSGroup) getSelectedCluster()).getcompatibility_version(), asyncQuery);
            }

            getComment().validateEntity(new IValidation[] { new SpecialAsciiI18NOrNoneValidation() });
        }

        if (getIsAutoAssign().getEntity() != null && ((Boolean) getIsAutoAssign().getEntity()) == false) {
            getDefaultHost().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        } else {
            getDefaultHost().setIsValid(true);
        }

        getTemplate().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        getDisksAllocationModel().validateEntity(new IValidation[] {});

        getCdImage().setIsValid(true);
        if (getCdImage().getIsChangable()) {
            getCdImage().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        }

        if (getIsLinuxOS()) {
            getKernel_path().validateEntity(new IValidation[] { new NoTrimmingWhitespacesValidation() });
            getInitrd_path().validateEntity(new IValidation[] { new NoTrimmingWhitespacesValidation() });
            getKernel_parameters().validateEntity(new IValidation[] { new NoTrimmingWhitespacesValidation() });

            // initrd path and kernel params require kernel path to be filled
            if (StringHelper.isNullOrEmpty((String) getKernel_path().getEntity())) {
                final UIConstants constants = ConstantsManager.getInstance().getConstants();

                if (!StringHelper.isNullOrEmpty((String) getInitrd_path().getEntity())) {
                    getInitrd_path().getInvalidityReasons().add(constants.initrdPathInvalid());
                    getInitrd_path().setIsValid(false);
                    getKernel_path().getInvalidityReasons().add(constants.initrdPathInvalid());
                    getKernel_path().setIsValid(false);
                }

                if (!StringHelper.isNullOrEmpty((String) getKernel_parameters().getEntity())) {
                    getKernel_parameters().getInvalidityReasons().add(constants.kernelParamsInvalid());
                    getKernel_parameters().setIsValid(false);
                    getKernel_path().getInvalidityReasons().add(constants.kernelParamsInvalid());
                    getKernel_path().setIsValid(false);
                }
            }
        }

        if (getCpuSharesAmount().getIsAvailable()) {
            getCpuSharesAmount().validateEntity(new IValidation[] {new NotEmptyValidation()
                    , new IntegerValidation(0, 262144)});
        }

        boolean customPropertySheetValid = getCustomPropertySheet().validate();

        setIsBootSequenceTabValid(true);
        setIsAllocationTabValid(getIsBootSequenceTabValid());
        setIsDisplayTabValid(getIsAllocationTabValid());
        setIsFirstRunTabValid(getIsDisplayTabValid());
        setIsGeneralTabValid(getIsFirstRunTabValid());

        setIsGeneralTabValid(getName().getIsValid() && getDescription().getIsValid() && getComment().getIsValid()
                && getDataCenterWithClustersList().getIsValid()
                && getTemplate().getIsValid() && getMemSize().getIsValid()
                && getMinAllocatedMemory().getIsValid());

        setIsFirstRunTabValid(getDomain().getIsValid() && getTimeZone().getIsValid());
        setIsDisplayTabValid(getUsbPolicy().getIsValid() && getNumOfMonitors().getIsValid());
        setIsHostTabValid(getDefaultHost().getIsValid());
        setIsAllocationTabValid(getDisksAllocationModel().getIsValid() && getMinAllocatedMemory().getIsValid()
                && getCpuSharesAmount().getIsValid());
        setIsBootSequenceTabValid(getCdImage().getIsValid() && getKernel_path().getIsValid());
        setIsCustomPropertiesTabValid(customPropertySheetValid);

        return getName().getIsValid() && getDescription().getIsValid() && getDataCenterWithClustersList().getIsValid()
                && getDisksAllocationModel().getIsValid() && getTemplate().getIsValid() && getComment().getIsValid()
                && getDefaultHost().getIsValid() && getMemSize().getIsValid() && getMinAllocatedMemory().getIsValid()
                && getNumOfMonitors().getIsValid() && getDomain().getIsValid() && getUsbPolicy().getIsValid()
                && getTimeZone().getIsValid() && getOSType().getIsValid() && getCdImage().getIsValid()
                && getKernel_path().getIsValid()
                && getInitrd_path().getIsValid()
                && getKernel_parameters().getIsValid()
                && getCpuSharesAmount().getIsValid()
                && behavior.validate()
                && customPropertySheetValid && getQuota().getIsValid();

    }

    class TotalCpuCoresComposableValidation implements IValidation {

        @Override
        public ValidationResult validate(Object value) {
            boolean isOk = behavior.isNumOfSocketsCorrect(Integer.parseInt(getTotalCPUCores().getEntity().toString()));
            ValidationResult res = new ValidationResult();
            res.setSuccess(isOk);
            res.setReasons(Arrays.asList(ConstantsManager.getInstance()
                    .getMessages()
                    .incorrectVCPUNumber()));
            return res;

        }

    }

    private void validateMemorySize(EntityModel model, int maxMemSize, int minMemSize)
    {
        boolean isValid = false;

        int memSize = (Integer) model.getEntity();

        if (memSize == 0)
        {
            model.getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getMessages()
                    .memSizeBetween(minMemSize, maxMemSize));
        }
        else if (memSize > maxMemSize)
        {
            model.getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getMessages()
                    .maxMemSizeIs(maxMemSize));
        }
        else if (memSize < minMemSize)
        {
            model.getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getMessages()
                    .minMemSizeIs(minMemSize));
        }
        else
        {
            isValid = true;
        }

        model.setIsValid(isValid);
    }

    private NotChangableForVmInPoolListModel poolType;

    public ListModel getPoolType()
    {
        return poolType;
    }

    protected void setPoolType(NotChangableForVmInPoolListModel value)
    {
        poolType = value;
    }

    private NotChangableForVmInPoolEntityModel numOfDesktops;

    public EntityModel getNumOfDesktops()
    {
        return numOfDesktops;
    }

    protected void setNumOfDesktops(NotChangableForVmInPoolEntityModel value)
    {
        numOfDesktops = value;
    }

    private NotChangableForVmInPoolEntityModel assignedVms;

    public EntityModel getAssignedVms()
    {
        return assignedVms;
    }

    public void setAssignedVms(NotChangableForVmInPoolEntityModel value)
    {
        assignedVms = value;
    }

    private boolean isPoolTabValid;

    public boolean getIsPoolTabValid()
    {
        return isPoolTabValid;
    }

    public void setIsPoolTabValid(boolean value)
    {
        if (isPoolTabValid != value)
        {
            isPoolTabValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsPoolTabValid")); //$NON-NLS-1$
        }
    }

    private NotChangableForVmInPoolEntityModel prestartedVms;

    public EntityModel getPrestartedVms() {
        return prestartedVms;
    }

    protected void setPrestartedVms(NotChangableForVmInPoolEntityModel value) {
        prestartedVms = value;
    }

    private String prestartedVmsHint;

    public String getPrestartedVmsHint() {
        return prestartedVmsHint;
    }

    public void setPrestartedVmsHint(String value) {
        if (!StringHelper.stringsEqual(prestartedVmsHint, value)) {
            prestartedVmsHint = value;
            onPropertyChanged(new PropertyChangedEventArgs("PrestartedVmsHint")); //$NON-NLS-1$
        }
    }

    private NotChangableForVmInPoolEntityModel maxAssignedVmsPerUser;

    public EntityModel getMaxAssignedVmsPerUser() {
        return maxAssignedVmsPerUser;
    }

    public void setMaxAssignedVmsPerUser(NotChangableForVmInPoolEntityModel maxAssignedVmsPerUser) {
        this.maxAssignedVmsPerUser = maxAssignedVmsPerUser;
    }

    private class NotChangableForVmInPoolListModel extends ListModel {
        @Override
        public ListModel setIsChangable(boolean value) {
            if (!isVmAttachedToPool())
                super.setIsChangable(value);
            return this;
        }
    }

    private class NotChangableForVmInPoolEntityModel extends EntityModel {
        @Override
        public EntityModel setIsChangable(boolean value) {
            if (!isVmAttachedToPool())
                super.setIsChangable(value);
            return this;
        }
    }

    private class NotChangableForVmInPoolKeyValueModel extends KeyValueModel {
        @Override
        public KeyValueModel setIsChangable(boolean value) {
            if (!isVmAttachedToPool())
                super.setIsChangable(value);
            return this;
        }
    }

    private ListModel watchdogModel;

    public ListModel getWatchdogModel() {
        return watchdogModel;
    }

    public void setWatchdogModel(ListModel watchdogModel) {
        this.watchdogModel = watchdogModel;
    }

    private ListModel watchdogAction;

    public ListModel getWatchdogAction() {
        return watchdogAction;
    }

    public void setWatchdogAction(ListModel watchdogAction) {
        this.watchdogAction = watchdogAction;
    }

    public StoragePool getSelectedDataCenter() {
        DataCenterWithCluster dataCenterWithCluster =
                (DataCenterWithCluster) getDataCenterWithClustersList().getSelectedItem();
        if (dataCenterWithCluster == null) {
            return null;
        }

        return dataCenterWithCluster.getDataCenter();
    }

    public VDSGroup getSelectedCluster() {
        DataCenterWithCluster dataCenterWithCluster =
                (DataCenterWithCluster) getDataCenterWithClustersList().getSelectedItem();
        if (dataCenterWithCluster == null) {
            return null;
        }

        return dataCenterWithCluster.getCluster();
    }

    public void disableEditing() {
        getDefaultCommand().setIsExecutionAllowed(false);
        getEditingEnabled().setEntity(false);
    }

    public static enum CpuSharesAmount {
        DISABLED(0), LOW(512), MEDIUM(1024), HIGH(2048), CUSTOM(-1);

        private int value;
        private CpuSharesAmount(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
}
