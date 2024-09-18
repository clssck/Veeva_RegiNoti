package com.veeva.vault.custom.triggers;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.core.VaultRuntimeException;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordChange;
import com.veeva.vault.sdk.api.data.RecordEvent;
import com.veeva.vault.sdk.api.data.RecordTrigger;
import com.veeva.vault.sdk.api.data.RecordTriggerContext;
import com.veeva.vault.sdk.api.data.RecordTriggerInfo;
import com.veeva.vault.sdk.api.notification.NotificationMessage;
import com.veeva.vault.sdk.api.notification.NotificationParameters;
import com.veeva.vault.sdk.api.notification.NotificationService;
import com.veeva.vault.sdk.api.query.QueryResponse;
import com.veeva.vault.sdk.api.query.QueryResult;
import com.veeva.vault.sdk.api.query.QueryService;

import java.util.Iterator;
import java.util.Set;

/**
 * This class is a Record Trigger for the 'registration__rim' object.
 * It sends a notification when the 'state__v' field is updated.
 */
@RecordTriggerInfo(object = "registration__rim", events = {RecordEvent.AFTER_UPDATE})
public class RegistrationLifecycleNotification implements RecordTrigger {

    @Override
    public void execute(RecordTriggerContext context) {
        LogService logService = ServiceLocator.locate(LogService.class);

        for (RecordChange change : context.getRecordChanges()) {
            Record oldRecord = change.getOld();
            Record newRecord = change.getNew();

            String oldState = oldRecord.getValue("state__v", ValueType.STRING);
            String newState = newRecord.getValue("state__v", ValueType.STRING);

            if (hasStateChanged(oldState, newState)) {
                String responsiblePersonId = newRecord.getValue("responsible_person__c", ValueType.STRING);
                String registrationName = newRecord.getValue("name__v", ValueType.STRING);

                if (isValidResponsiblePerson(responsiblePersonId)) {
                    sendNotification(responsiblePersonId, oldState, newState, registrationName, logService);
                } else {
                    logService.error("Responsible person is null or empty for registration: " + registrationName);
                }
            }
        }
    }

    /**
     * Checks if the state has changed.
     *
     * @param oldState The old state of the registration.
     * @param newState The new state of the registration.
     * @return true if the state has changed, false otherwise.
     */
    private boolean hasStateChanged(String oldState, String newState) {
        return oldState != null && newState != null && !oldState.equals(newState);
    }

    /**
     * Checks if the responsible person is valid.
     *
     * @param responsiblePersonId The responsible person's ID to check.
     * @return true if the responsible person ID is valid, false otherwise.
     */
    private boolean isValidResponsiblePerson(String responsiblePersonId) {
        return responsiblePersonId != null && !responsiblePersonId.trim().isEmpty();
    }

    /**
     * Gets the description for a given state value.
     *
     * @param state The state value.
     * @return The description of the state, or the original state if not found.
     */
    private String getStateDescription(String state) {
        switch (state) {
            case "planned_state1__c":
                return "Planned";
            case "approved_state1__c":
                return "Approved/Authorized";
            case "closed_state__c":
                return "Closed";
            case "expired_state__c":
                return "Expired";
            case "on_hold_state__c":
                return "On Hold";
            case "suspended_state__c":
                return "Suspended";
            case "withdrawn_state1__c":
                return "Withdrawn";
            case "transferred_state__c":
                return "Transferred";
            default:
                return state;
        }
    }

    /**
     * Sends a notification to the responsible person.
     *
     * @param responsiblePersonId The ID of the responsible person.
     * @param oldState            The old state of the registration.
     * @param newState            The new state of the registration.
     * @param registrationName    The name of the registration.
     * @param logService          The LogService instance for logging.
     */
    private void sendNotification(String responsiblePersonId, String oldState, String newState,
                                  String registrationName, LogService logService) {

        String subject = "Registration Lifecycle Change Notification";
        String message = String.format("Registration Lifecycle changed from %s to %s on registration: %s",
                getStateDescription(oldState), getStateDescription(newState), registrationName);

        try {
            // Query the user__sys object to get the username
            QueryService queryService = ServiceLocator.locate(QueryService.class);
            String queryStr = "SELECT username__sys FROM user__sys WHERE id = '" + responsiblePersonId + "'";
            QueryResponse queryResponse = queryService.query(queryStr);

            String username = null;
            Iterator<QueryResult> iterator = queryResponse.streamResults().iterator();
            if (iterator.hasNext()) {
                QueryResult result = iterator.next();
                username = result.getValue("username__sys", ValueType.STRING);
            } else {
                logService.error("User not found for ID: " + responsiblePersonId);
                return;
            }

            // Log the username
            logService.info("Responsible person username: " + username);

            // Prepare the notification
            NotificationService notificationService = ServiceLocator.locate(NotificationService.class);
            NotificationParameters params = notificationService.newNotificationParameters();

            Set<String> recipients = VaultCollections.newSet();
            recipients.add(responsiblePersonId); // Use the ID directly
            params.setRecipientsByUserIds(recipients);

            // Optionally, set the Reply-To user if needed
            // params.setReplyToByUserId(responsiblePersonId);

            // Create and set up the NotificationMessage
            NotificationMessage notificationMessage = notificationService.newNotificationMessage();
            notificationMessage.setSubject(subject);
            notificationMessage.setMessage(message);             // Sets the email body
            notificationMessage.setNotificationText(message);    // Sets the in-app notification text

            // Send the notification
            notificationService.send(params, notificationMessage);
            logService.info("Notification sent to user ID " + responsiblePersonId + " for registration: " + registrationName);

        } catch (VaultRuntimeException e) {
            logService.error("Failed to send notification: " + e.getMessage());
        }
    }
}
