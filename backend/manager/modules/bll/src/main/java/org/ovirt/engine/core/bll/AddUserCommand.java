package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.adbroker.AdActionType;
import org.ovirt.engine.core.bll.adbroker.LdapFactory;
import org.ovirt.engine.core.bll.adbroker.LdapSearchByIdParameters;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddUserParameters;
import org.ovirt.engine.core.common.businessentities.LdapGroup;
import org.ovirt.engine.core.common.businessentities.LdapUser;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;

public class AddUserCommand<T extends AddUserParameters> extends CommandBase<T> {

    public AddUserCommand(T params) {
        super(params);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_ADD : AuditLogType.USER_FAILED_ADD_ADUSER;
    }

    @Override
    protected boolean canDoAction() {
        Guid userId = null;
        String domain = null;
        if (getParameters().getUser() != null) {
            addCustomValue("NewUserName", getParameters().getUser().getLoginName());
            userId = getParameters().getUser().getId();
            domain = getParameters().getUser().getDomain();
            LdapUser adUser = (LdapUser) LdapFactory.getInstance(domain).RunAdAction(AdActionType.GetAdUserByUserId,
                    new LdapSearchByIdParameters(domain, userId)).getReturnValue();
            if (adUser == null) {
                addCanDoActionMessage(VdcBllMessages.USER_MUST_EXIST_IN_DIRECTORY);
                return false;
            }
            // set the AD user on the parameters to save another roundtrip to the AD when adding the user
            getParameters().setAdUser(adUser);
        }
        else if (getParameters().getAdUser() != null) {
            addCustomValue("NewUserName", getParameters().getAdUser().getUserName());
            userId = getParameters().getAdUser().getUserId();
            domain = getParameters().getAdUser().getDomainControler();
            LdapUser adUser = (LdapUser) LdapFactory.getInstance(domain).RunAdAction(AdActionType.GetAdUserByUserId,
                    new LdapSearchByIdParameters(domain, userId)).getReturnValue();
            if (adUser == null) {
                addCanDoActionMessage(VdcBllMessages.USER_MUST_EXIST_IN_DIRECTORY);
                return false;
            }
        }
        else if (getParameters().getAdGroup() != null) {
            addCustomValue("NewUserName", getParameters().getAdGroup().getname());
            userId = getParameters().getAdGroup().getid();
            domain = getParameters().getAdGroup().getdomain();
            LdapGroup adGroup =
                    (LdapGroup) LdapFactory.getInstance(domain).RunAdAction(AdActionType.GetAdGroupByGroupId,
                    new LdapSearchByIdParameters(domain, userId)).getReturnValue();
            if (adGroup == null) {
                addCanDoActionMessage(VdcBllMessages.USER_MUST_EXIST_IN_DIRECTORY);
                return false;
            }
        }

        if (userId == null) {
            addCanDoActionMessage(VdcBllMessages.MISSING_DIRECTORY_ELEMENT_ID);
            return false;
        }

        return true;
    }

    @Override
    protected void executeCommand() {
        if (getParameters().getAdUser() != null) {
            UserCommandBase.persistAuthenticatedUser(getParameters().getAdUser());
        }
        // try to add group to db if adGroup sent
        else if (getParameters().getAdGroup() != null) {
            AdGroupsHandlingCommandBase.initAdGroup(getParameters().getAdGroup());
        }
        setSucceeded(true);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(MultiLevelAdministrationHandler.SYSTEM_OBJECT_ID,
                VdcObjectType.System,
                getActionType().getActionGroup()));
    }
}
