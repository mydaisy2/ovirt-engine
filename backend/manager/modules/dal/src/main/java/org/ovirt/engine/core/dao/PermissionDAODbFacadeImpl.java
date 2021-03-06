package org.ovirt.engine.core.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.RoleType;
import org.ovirt.engine.core.common.businessentities.permissions;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.compat.Guid;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * <code>PermissionsDAODbFacadeImpl</code> provides a concrete implementation of {@link PermissionDAO} using code from
 * DbFacade.
 */
public class PermissionDAODbFacadeImpl extends BaseDAODbFacade implements PermissionDAO {
    @Override
    public permissions get(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("id", id);

        return getCallsHandler().executeRead("GetPermissionsByid", PermissionRowMapper.instance, parameterSource);
    }

    @Override
    public List<permissions> getConsumedPermissionsForQuotaId(Guid quotaId) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("quota_id", quotaId);

        return getCallsHandler().executeReadList("GetConsumedPermissionsForQuotaId",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public permissions getForRoleAndAdElementAndObject(Guid roleid,
            Guid elementid, Guid objectid) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("role_id", roleid)
                .addValue("ad_element_id", elementid)
                .addValue("object_id", objectid);

        return getCallsHandler().executeRead("GetPermissionsByRoleIdAndAdElementIdAndObjectId",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public permissions getForRoleAndAdElementAndObjectWithGroupCheck(Guid roleid,
            Guid elementid, Guid objectid) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("role_id", roleid)
                .addValue("ad_element_id", elementid)
                .addValue("object_id", objectid);

        return getCallsHandler().executeRead("GetForRoleAndAdElementAndObject_wGroupCheck",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<permissions> getAllForAdElement(Guid id) {
        return getAllForAdElement(id, null, false);
    }

    @Override
    public List<permissions> getAllForAdElement(Guid id, Guid userID, boolean isFiltered) {
        Integer appMode = Config.<Integer> GetValue(ConfigValues.ApplicationMode);

        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("ad_element_id", id).
                addValue("user_id", userID).
                addValue("is_filtered", isFiltered).
                addValue("app_mode", appMode.intValue());

        return getCallsHandler().executeReadList("GetPermissionsByAdElementId",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<permissions> getAllDirectPermissionsForAdElement(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("ad_element_id", id);

        return getCallsHandler().executeReadList("GetDirectPermissionsByAdElementId",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<permissions> getAllForRole(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("role_id", id);

        return getCallsHandler().executeReadList("GetPermissionsByRoleId",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<permissions> getAllForRoleAndAdElement(Guid roleid,
            Guid elementid) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("role_id", roleid).addValue("ad_element_id",
                        elementid);

        return getCallsHandler().executeReadList("GetPermissionsByRoleIdAndAdElementId",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<permissions> getAllForRoleAndObject(Guid roleid, Guid objectid) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("role_id", roleid).addValue("object_id", objectid);

        return getCallsHandler().executeReadList("GetPermissionsByRoleIdAndObjectId",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<permissions> getAllForEntity(Guid id) {
        return getAllForEntity(id, null, false);
    }

    @Override
    public List<permissions> getAllForEntity(Guid id, Guid userID, boolean isFiltered) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("id", id).addValue("user_id", userID).addValue("is_filtered", isFiltered);
        return getCallsHandler().executeReadList("GetPermissionsByEntityId",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public List<permissions> getTreeForEntity(Guid id, VdcObjectType type) {
        return getTreeForEntity(id, type, null, false);
    }

    @Override
    public List<permissions> getTreeForEntity(Guid id, VdcObjectType type, Guid userID, boolean isFiltered) {
        MapSqlParameterSource parameterSource =
                getCustomMapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("object_type_id", type.getValue())
                        .addValue("user_id", userID)
                        .addValue("is_filtered", isFiltered);
        return getCallsHandler().executeReadList("GetPermissionsTreeByEntityId",
                PermissionRowMapper.instance,
                parameterSource);
    }

    @Override
    public Guid getEntityPermissions(Guid adElementId, ActionGroup actionGroup, Guid objectId,
                                     VdcObjectType vdcObjectType) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource().addValue("user_id", adElementId)
                .addValue("action_group_id", actionGroup.getId()).addValue("object_id", objectId).addValue(
                        "object_type_id", vdcObjectType.getValue());

        return getCallsHandler().executeRead("get_entity_permissions",
                createGuidMapper(),
                parameterSource);
    }

    @Override
    public Guid getEntityPermissionsForUserAndGroups(Guid userId,
                                                     String groupIds,
                                                     ActionGroup actionGroup,
                                                     Guid objectId,
                                                     VdcObjectType vdcObjectType,
                                                     boolean ignoreEveryone) {
        MapSqlParameterSource parameterSource =
                getCustomMapSqlParameterSource().addValue("user_id", userId)
                        .addValue("group_ids", groupIds)
                        .addValue("action_group_id", actionGroup.getId())
                        .addValue("object_id", objectId)
                        .addValue("object_type_id", vdcObjectType.getValue())
                        .addValue("ignore_everyone", ignoreEveryone);

        return getCallsHandler().executeRead("get_entity_permissions_for_user_and_groups",
                createGuidMapper(),
                parameterSource);
    }

    @Override
    public void save(permissions permission) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("ad_element_id", permission.getad_element_id())
                .addValue("id", permission.getId())
                .addValue("role_id", permission.getrole_id())
                .addValue("object_id", permission.getObjectId())
                .addValue("object_type_id",
                        permission.getObjectType().getValue());

        getCallsHandler().executeModification("InsertPermission", parameterSource);
    }

    @Override
    public void remove(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("id", id);

        getCallsHandler().executeModification("DeletePermission", parameterSource);
    }

    @Override
    public void removeForEntity(Guid id) {
        MapSqlParameterSource parameterSource = getCustomMapSqlParameterSource()
                .addValue("id", id);
        getCallsHandler().executeModification("DeletePermissionsByEntityId", parameterSource);
    }

    @Override
    public List<permissions> getAll() {
        throw new NotImplementedException();
    }

    @Override
    public void update(permissions entity) {
        throw new NotImplementedException();
    }

    private static class PermissionRowMapper implements RowMapper<permissions> {
        public static final PermissionRowMapper instance = new PermissionRowMapper();

        @Override
        public permissions mapRow(ResultSet rs, int rowNum) throws SQLException {
            permissions entity = new permissions();
            entity.setad_element_id(getGuidDefaultEmpty(rs, "ad_element_id"));
            entity.setId(getGuidDefaultEmpty(rs, "id"));
            entity.setrole_id(getGuidDefaultEmpty(rs, "role_id"));
            entity.setObjectId(getGuidDefaultEmpty(rs, "object_id"));
            entity.setObjectType(VdcObjectType.forValue(rs
                    .getInt(("object_type_id"))));
            entity.setRoleName(rs.getString("role_name"));
            entity.setObjectName(rs.getString("object_name"));
            entity.setOwnerName(rs.getString("owner_name"));
            entity.setRoleType(RoleType.getById(rs.getInt("role_type")));
            return entity;
        }
    }

}
