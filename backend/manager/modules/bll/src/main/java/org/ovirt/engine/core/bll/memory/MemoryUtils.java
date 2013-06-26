package org.ovirt.engine.core.bll.memory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.GuidUtils;

public class MemoryUtils {

    /**
     * Modified the given memory volume String representation to have the given storage
     * pool and storage domain
     */
    public static String changeStorageDomainAndPoolInMemoryVolume(
            String originalMemoryVolume, Guid storageDomainId, Guid storagePoolId) {
        List<Guid> guids = GuidUtils.getGuidListFromString(originalMemoryVolume);
        return String.format("%1$s,%2$s,%3$s,%4$s,%5$s,%6$s",
                storageDomainId.toString(), storagePoolId.toString(),
                guids.get(2), guids.get(3), guids.get(4), guids.get(5));
    }

    public static Set<String> getMemoryVolumesFromSnapshots(List<Snapshot> snapshots) {
        Set<String> memories = new HashSet<String>();
        for (Snapshot snapshot : snapshots) {
            memories.add(snapshot.getMemoryVolume());
        }
        memories.remove(StringUtils.EMPTY);
        return memories;
    }
}