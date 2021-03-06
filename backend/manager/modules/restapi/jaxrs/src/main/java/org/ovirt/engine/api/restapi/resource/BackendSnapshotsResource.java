package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.ConfigurationType;
import org.ovirt.engine.api.model.Snapshot;
import org.ovirt.engine.api.model.Snapshots;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.resource.SnapshotResource;
import org.ovirt.engine.api.resource.SnapshotsResource;
import org.ovirt.engine.api.restapi.types.SnapshotMapper;
import org.ovirt.engine.core.common.action.CreateAllSnapshotsFromVmParameters;
import org.ovirt.engine.core.common.action.RemoveSnapshotParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendSnapshotsResource
        extends AbstractBackendCollectionResource<Snapshot, org.ovirt.engine.core.common.businessentities.Snapshot>
        implements SnapshotsResource {

    static final String[] SUB_COLLECTIONS = { "cdroms", "disks", "nics" };
    protected Guid parentId;

    public BackendSnapshotsResource(Guid parentId) {
        super(Snapshot.class, org.ovirt.engine.core.common.businessentities.Snapshot.class, SUB_COLLECTIONS);
        this.parentId = parentId;
    }

    @Override
    public Snapshots list() {
        return mapCollection(getBackendCollection(VdcQueryType.GetAllVmSnapshotsByVmId,
                new IdQueryParameters(parentId)));
    }

    @Override
    public Response add(Snapshot snapshot) {
        return doAdd(snapshot, expectBlocking());
    }

    protected Response doAdd(Snapshot snapshot, boolean block) {
        validateParameters(snapshot, "description");
        CreateAllSnapshotsFromVmParameters snapshotParams =
            new CreateAllSnapshotsFromVmParameters(parentId, snapshot.getDescription());
        if (snapshot.isSetPersistMemorystate()) {
            snapshotParams.setSaveMemory(snapshot.isPersistMemorystate());
        }
        return performCreate(VdcActionType.CreateAllSnapshotsFromVm,
                               snapshotParams,
                               new SnapshotIdResolver(),
                               block);
    }

    @Override
    public Response performRemove(String id) {
        return performAction(VdcActionType.RemoveSnapshot,
                new RemoveSnapshotParameters(asGuid(id), parentId));
    }

    @Override
    @SingleEntityResource
    public SnapshotResource getSnapshotSubResource(String id) {
        return inject(new BackendSnapshotResource(id, parentId, this));
    }

    protected Snapshots mapCollection(List<org.ovirt.engine.core.common.businessentities.Snapshot> entities) {
        Snapshots snapshots = new Snapshots();
        for (org.ovirt.engine.core.common.businessentities.Snapshot entity : entities) {
            Snapshot snapshot = map(entity, null);
            snapshot = populate(snapshot, entity);
            snapshot = addLinks(snapshot);
            snapshot = addVmConfiguration(entity, snapshot);
            snapshots.getSnapshots().add(snapshot);
        }
        return snapshots;
    }

    protected Snapshot addVmConfiguration(org.ovirt.engine.core.common.businessentities.Snapshot entity, Snapshot snapshot) {
        if (entity.isVmConfigurationAvailable()) {
            snapshot.setVm(new VM());
            getMapper(org.ovirt.engine.core.common.businessentities.VM.class, VM.class).map(getVmPreview(snapshot), snapshot.getVm());
            snapshot.getVm().getLinks().addAll(snapshot.getLinks());
        }
        else {
            snapshot.setVm(null);
        }
        snapshot.getLinks().clear();
        return snapshot;
    }

    protected org.ovirt.engine.core.common.businessentities.VM getVmPreview(Snapshot snapshot) {
        org.ovirt.engine.core.common.businessentities.VM vm = getEntity(org.ovirt.engine.core.common.businessentities.VM.class, VdcQueryType.GetVmConfigurationBySnapshot, new IdQueryParameters(asGuid(snapshot.getId())), null);
        return vm;
    }

    protected org.ovirt.engine.core.common.businessentities.Snapshot getSnapshotById(Guid id) {
        //TODO: move to 'GetSnapshotBySnapshotId' once Backend supplies it.
        for (org.ovirt.engine.core.common.businessentities.Snapshot snapshot : getBackendCollection(VdcQueryType.GetAllVmSnapshotsByVmId,
                new IdQueryParameters(parentId))) {
            if (snapshot.getId().equals(id)) {
                return snapshot;
            }
        }
        return null;
    }

    @Override
    protected Snapshot addParents(Snapshot snapshot) {
        snapshot.setVm(new VM());
        snapshot.getVm().setId(parentId.toString());
        return snapshot;
    }

    protected class SnapshotIdResolver extends EntityIdResolver<Guid> {

        SnapshotIdResolver() {}

        @Override
        public org.ovirt.engine.core.common.businessentities.Snapshot lookupEntity(
                Guid id) throws BackendFailureException {
            return getSnapshotById(id);
        }
    }

    @Override
    protected Snapshot doPopulate(Snapshot model, org.ovirt.engine.core.common.businessentities.Snapshot entity) {
        return populateSnapshotConfiguration(model);
    }

    private Snapshot populateSnapshotConfiguration (Snapshot model) {
        VdcQueryReturnValue queryReturnValue =
                runQuery(VdcQueryType.GetVmOvfConfigurationBySnapshot,
                        new IdQueryParameters(Guid.createGuidFromString(model.getId())));

        if (queryReturnValue.getSucceeded() && queryReturnValue.getReturnValue() != null) {
            return SnapshotMapper.mapSnapshotConfiguration((String) queryReturnValue.getReturnValue(),
                    ConfigurationType.OVF,
                    model);
        }

        return model;
    }
}
