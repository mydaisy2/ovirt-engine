package org.ovirt.engine.core.bll.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.verification.VerificationMode;
import org.ovirt.engine.core.bll.context.NoOpCompensationContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VmDAO;
import org.ovirt.engine.core.dao.network.VmNetworkInterfaceDao;
import org.ovirt.engine.core.dao.network.VmNetworkStatisticsDao;
import org.ovirt.engine.core.dao.network.VmNicDao;
import org.ovirt.engine.core.utils.MockConfigRule;
import org.ovirt.engine.core.utils.RandomUtils;

public class VmInterfaceManagerTest {

    private final String NETWORK_NAME = "networkName";
    private final String VM_NAME = "vmName";
    private final static Version VERSION_3_2 = new Version(3, 2);
    private final static int OS_ID = 0;

    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.HotPlugEnabled, VERSION_3_2.getValue(), true));

    @Mock
    private MacPoolManager macPoolManager;

    @Mock
    private VmNetworkStatisticsDao vmNetworkStatisticsDAO;

    @Mock
    private VmNetworkInterfaceDao vmNetworkInterfaceDAO;

    @Mock
    private VmNicDao vmNicDao;

    @Mock
    private VmDAO vmDAO;

    @Mock
    private ExternalNetworkManager externalNetworkManager;

    @Spy
    private VmInterfaceManager vmInterfaceManager = new VmInterfaceManager();

    @Mock
    private Version version;

    @Before
    @SuppressWarnings("unchecked")
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);

        doReturn(macPoolManager).when(vmInterfaceManager).getMacPoolManager();
        doReturn(vmNetworkStatisticsDAO).when(vmInterfaceManager).getVmNetworkStatisticsDao();
        doReturn(vmNetworkInterfaceDAO).when(vmInterfaceManager).getVmNetworkInterfaceDao();
        doReturn(vmNicDao).when(vmInterfaceManager).getVmNicDao();
        doReturn(vmDAO).when(vmInterfaceManager).getVmDAO();
        doNothing().when(vmInterfaceManager).auditLogMacInUseUnplug(any(VmNic.class));
        doNothing().when(vmInterfaceManager).removeFromExternalNetworks(anyList());

        doNothing().when(vmInterfaceManager).log(any(AuditLogableBase.class), any(AuditLogType.class));
    }

    @Test
    public void add() {
        runAddAndVerify(createNewInterface(), true, times(1), OS_ID, VERSION_3_2);
    }

    @Test
    public void addWithExistingMacAddressSucceed() {
        VmNic iface = createNewInterface();
        runAddAndVerify(iface, true, times(1), OS_ID, VERSION_3_2);
    }

    protected void runAddAndVerify(VmNic iface,
            boolean addMacResult,
            VerificationMode addMacVerification,
            int osId,
            Version version) {
        OsRepository osRepository = mock(OsRepository.class);
        when(vmInterfaceManager.getOsRepository()).thenReturn(osRepository);
        when(osRepository.hasNicHotplugSupport(any(Integer.class), any(Version.class))).thenReturn(true);
        vmInterfaceManager.add(iface, NoOpCompensationContext.getInstance(), false, osId, version);
        verify(macPoolManager, times(1)).forceAddMac((iface.getMacAddress()));
        verifyAddDelegatedCorrectly(iface, addMacVerification);
    }

    @Test
    public void addAllocateNewMacAddress() {
        VmNic iface = createNewInterface();
        String newMac = RandomUtils.instance().nextString(10);
        when(macPoolManager.allocateNewMac()).thenReturn(newMac);
        vmInterfaceManager.add(iface, NoOpCompensationContext.getInstance(), true, OS_ID, VERSION_3_2);
        assertEquals(newMac, iface.getMacAddress());
    }

    @Test
    public void findActiveVmsUsingNetworks() {
        mockDaos();

        List<String> vmNames =
                vmInterfaceManager.findActiveVmsUsingNetworks(Guid.newGuid(), Collections.singletonList(NETWORK_NAME));
        assertTrue(vmNames.contains(VM_NAME));
    }

    @Test
    public void findNoneOfActiveVmsUsingNetworks() {
        mockDaos();

        List<String> vmNames =
                vmInterfaceManager.findActiveVmsUsingNetworks(Guid.newGuid(),
                        Collections.singletonList(NETWORK_NAME + "1"));
        assertTrue(vmNames.isEmpty());
    }

    @Test
    public void removeAll() {
        List<VmNic> interfaces = Arrays.asList(createNewInterface(), createNewInterface());

        when(vmNicDao.getAllForVm(any(Guid.class))).thenReturn(interfaces);

        vmInterfaceManager.removeAll(Guid.newGuid());

        for (VmNic iface : interfaces) {
            verifyRemoveAllDelegatedCorrectly(iface);
        }
    }

    private void mockDaos() {
        VM vm = createVM(VM_NAME, NETWORK_NAME);
        when(vmDAO.getAllRunningForVds(any(Guid.class))).thenReturn(Arrays.asList(vm));
        when(vmNetworkInterfaceDAO.getAllForVm(vm.getId())).thenReturn(vm.getInterfaces());
    }

    /**
     * Verify that {@link VmInterfaceManager#add} delegated correctly to {@link MacPoolManager} & DAOs.
     *
     * @param iface
     *            The interface to check for.
     * @param addMacVerification
     *            Mode to check (times(1), never(), etc) for {@link MacPoolManager#addMac(String)}.
     */
    protected void verifyAddDelegatedCorrectly(VmNic iface, VerificationMode addMacVerification) {
        verify(macPoolManager, addMacVerification).forceAddMac(iface.getMacAddress());
        verify(vmNicDao).save(iface);
        verify(vmNetworkStatisticsDAO).save(iface.getStatistics());
    }

    /**
     * Verify that {@link VmInterfaceManager#removeAll} delegated correctly to {@link MacPoolManager} & DAOs.
     *
     * @param iface
     *            The interface to check for.
     */
    protected void verifyRemoveAllDelegatedCorrectly(VmNic iface) {
        verify(macPoolManager, times(1)).freeMac(iface.getMacAddress());
        verify(vmNicDao).remove(iface.getId());
        verify(vmNetworkStatisticsDAO).remove(iface.getId());
    }

    /**
     * @return A new interface that can be used in tests.
     */
    protected VmNic createNewInterface() {
        VmNic iface = new VmNic();
        iface.setId(Guid.newGuid());
        iface.setMacAddress(RandomUtils.instance().nextString(10));
        return iface;
    }

    protected VmNetworkInterface createNewViewableInterface() {
        VmNetworkInterface iface = new VmNetworkInterface();
        iface.setId(Guid.newGuid());
        iface.setMacAddress(RandomUtils.instance().nextString(10));
        return iface;
    }

    private Network createNewNetwork(boolean isVmNetwork, String networkName) {
        Network network = new Network();
        network.setVmNetwork(isVmNetwork);
        network.setName(networkName);
        return network;
    }

    /**
     * Creates a VM instance with a given name, having an interface which uses a given network.
     *
     * @param vmName
     *            The VM name to be set
     * @param networkName
     *            The network name to be set for the VM interface
     * @return the VM instance with the appropriate data.
     */
    private VM createVM(String vmName, String networkName) {
        VM vm = new VM();
        vm.setId(Guid.newGuid());
        vm.setName(vmName);
        VmNetworkInterface vmIface = createNewViewableInterface();
        vmIface.setVmId(vm.getId());
        vmIface.setNetworkName(networkName);
        vm.getInterfaces().add(vmIface);
        return vm;
    }
}
